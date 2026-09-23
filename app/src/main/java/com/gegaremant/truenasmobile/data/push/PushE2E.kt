package com.gegaremant.truenasmobile.data.push

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Ciphertext envelope as produced by the relay (`relay/` module). The `eph`
 * field is the relay's ephemeral EC P-256 public key (raw uncompressed point),
 * `ct` is IV(12) || AES-256-GCM ciphertext+tag, both Base64.
 */
data class PushEnvelope(
    val v: Int,
    val eph: String,
    val ct: String,
    val ts: Long
) {
    companion object {
        fun parse(json: String): PushEnvelope {
            val o = JSONObject(json)
            return PushEnvelope(
                v = o.optInt("v", 1),
                eph = o.getString("eph"),
                ct = o.getString("ct"),
                ts = o.optLong("ts", 0L)
            )
        }
    }
}

/**
 * Device end of the relay E2E pipeline.
 *
 * The private EC P-256 key lives in the Android Keystore and never leaves the
 * device. The public key (raw 65-byte point) is registered with the relay,
 * which encrypts each alert to an ephemeral key — the relay itself can never
 * read the plaintext.
 */
object PushE2E {

    const val KEY_ALIAS = "truenasmobile_push_e2e"
    const val KEY_DERIVATION_PREFIX = "truenasmobile-relay-v1:"

    private const val CURVE = "secp256r1"
    private const val IV_SIZE = 12

    private val ecParams: ECParameterSpec by lazy {
        AlgorithmParameters.getInstance("EC")
            .apply { init(ECGenParameterSpec(CURVE)) }
            .getParameterSpec(ECParameterSpec::class.java)
    }

    /** Returns the existing device keypair, creating it on first call. */
    fun getOrCreateKeyPair(context: Context, alias: String = KEY_ALIAS): KeyPair {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = ks.getKey(alias, null) as? java.security.PrivateKey
        val cert = ks.getCertificate(alias)
        if (privateKey != null && cert != null) {
            return KeyPair(cert.publicKey, privateKey)
        }

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        generator.initialize(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_AGREE_KEY)
                .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
                .build()
        )
        return generator.generateKeyPair()
    }

    /**
     * Base64 of the device public key as a raw 65-byte uncompressed point
     * (0x04 || X(32) || Y(32)) — the exact format the relay expects.
     */
    fun publicKeyPointB64(context: Context, alias: String = KEY_ALIAS): String {
        val pub = getOrCreateKeyPair(context, alias).public as ECPublicKey
        val point = byteArrayOf(0x04) + pub.w.affineX.toFixed(32) + pub.w.affineY.toFixed(32)
        return Base64.getEncoder().encodeToString(point)
    }

    /** Decrypts [envelope] with the device keypair, returning the raw alert JSON. */
    fun decryptEnvelope(context: Context, envelope: PushEnvelope, alias: String = KEY_ALIAS): ByteArray {
        val ciphertext = Base64.getDecoder().decode(envelope.ct)
        require(ciphertext.size > IV_SIZE + 16) { "Ciphertext too short" }
        val iv = ciphertext.copyOfRange(0, IV_SIZE)
        val body = ciphertext.copyOfRange(IV_SIZE, ciphertext.size)

        val keyPair = getOrCreateKeyPair(context, alias)

        val sharedSecret = KeyAgreement.getInstance("ECDH").run {
            init(keyPair.private)
            doPhase(decodeEcPoint(Base64.getDecoder().decode(envelope.eph)), true)
            generateSecret()
        }

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(KEY_DERIVATION_PREFIX.toByteArray(Charsets.UTF_8))
        val aesKey = digest.digest(sharedSecret)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }

    private fun decodeEcPoint(raw: ByteArray): ECPublicKey {
        require(raw.size == 65 && raw[0] == 0x04.toByte()) { "Expected 65-byte uncompressed EC point" }
        val x = BigInteger(1, raw.copyOfRange(1, 33))
        val y = BigInteger(1, raw.copyOfRange(33, 65))
        val spec = ECPublicKeySpec(ECPoint(x, y), ecParams)
        return java.security.KeyFactory.getInstance("EC").generatePublic(spec) as ECPublicKey
    }

    private fun BigInteger.toFixed(size: Int): ByteArray {
        val raw = toByteArray()
        return when {
            raw.size == size -> raw
            raw.size == size + 1 && raw[0] == 0.toByte() -> raw.copyOfRange(1, raw.size)
            raw.size < size -> ByteArray(size - raw.size) + raw
            else -> error("Integer too large for $size bytes")
        }
    }
}