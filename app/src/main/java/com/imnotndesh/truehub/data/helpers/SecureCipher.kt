package com.imnotndesh.truehub.data.helpers

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keystore-backed AES-256-GCM encryption for secrets at rest.
 *
 * The master key lives in the Android Keystore and can never be extracted.
 * Stored values look like "v1:<base64(iv || ciphertext)>". Values without the
 * "v1:" prefix are treated as legacy plaintext (written before this class
 * existed) and passed through unchanged on read; the next write re-encrypts them.
 *
 * No user authentication is required to use the key, so auto-login and
 * background workers keep working. This protects credentials from plain file
 * extraction / backups, not from the user themselves.
 */
object SecureCipher {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "truehub_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE_BITS = 128
    private const val PREFIX = "v1:"

    private val keyLock = Any()

    private fun getOrCreateKey(): SecretKey = synchronized(keyLock) {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return@synchronized it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        generator.generateKey()
    }

    /**
     * Encrypts [plaintext] for storage. Returns null if encryption failed;
     * callers may then fall back to storing the raw value.
     */
    fun encrypt(plaintext: String?): String? {
        if (plaintext == null) return null
        return runCatching { encryptInternal(plaintext) }.getOrNull()
    }

    /**
     * Decrypts a stored value. Legacy plaintext (no "v1:" prefix) is returned
     * unchanged. Returns null if the value cannot be decrypted.
     */
    fun decrypt(value: String?): String? {
        if (value == null) return null
        if (!value.startsWith(PREFIX)) return value
        return runCatching { decryptInternal(value) }.getOrNull()
    }

    fun isEncrypted(value: String?): Boolean = value != null && value.startsWith(PREFIX)

    private fun encryptInternal(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val iv = cipher.iv
        val payload = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(ciphertext, 0, payload, iv.size, ciphertext.size)

        return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decryptInternal(value: String): String? {
        val raw = try {
            Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (raw.size <= IV_SIZE) return null

        val iv = raw.copyOfRange(0, IV_SIZE)
        val ciphertext = raw.copyOfRange(IV_SIZE, raw.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}