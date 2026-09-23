package com.gegaremant.truenasmobile.relay.crypto

import com.gegaremant.truenasmobile.relay.model.Envelope
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
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
 * E2E encryption to a registered device.
 *
 * Flow per message:
 *  1. Generate a fresh ephemeral EC P-256 keypair (forward secrecy).
 *  2. ECDH(ephemeral_private, device_public) -> shared secret.
 *  3. AES-256-GCM key = SHA-256(shared secret).
 *  4. Envelope carries only the ephemeral public key + ciphertext.
 *
 * The relay never has the device private key, so it cannot decrypt — this
 * holds even for a hostile or compromised public relay.
 */
object E2E {

    private const val EC_CURVE = "secp256r1"
    private const val IV_SIZE = 12
    private const val KEY_DERIVATION_PREFIX = "truenasmobile-relay-v1:"

    private val ecParams: ECParameterSpec by lazy {
        AlgorithmParameters.getInstance("EC")
            .apply { init(ECGenParameterSpec(EC_CURVE)) }
            .getParameterSpec(ECParameterSpec::class.java)
    }

    /** Encrypts [plaintext] to the device whose raw public point is [pubKeyB64]. */
    fun encrypt(pubKeyB64: String, plaintext: ByteArray): Envelope {
        val peerPublic = decodePublicKey(pubKeyB64)
        val ephemeral = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec(EC_CURVE)) }
            .generateKeyPair()

        val sharedSecret = KeyAgreement.getInstance("ECDH").run {
            init(ephemeral.private)
            doPhase(peerPublic, true)
            generateSecret()
        }

        val key = deriveKey(sharedSecret)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val ciphertext = cipher.doFinal(plaintext)

        val payload = cipher.iv + ciphertext
        val ephPoint = (ephemeral.public as ECPublicKey).w.toUncompressedBytes()

        return Envelope(
            eph = Base64.getEncoder().encodeToString(ephPoint),
            ct = Base64.getEncoder().encodeToString(payload)
        )
    }

    private fun deriveKey(sharedSecret: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(KEY_DERIVATION_PREFIX.toByteArray(Charsets.UTF_8))
        return digest.digest(sharedSecret)
    }

    private fun decodePublicKey(pubKeyB64: String): ECPublicKey {
        val raw = Base64.getDecoder().decode(pubKeyB64)
        require(raw.size == 65 && raw[0] == 0x04.toByte()) { "Expected 65-byte uncompressed EC point" }
        val x = BigInteger(1, raw.copyOfRange(1, 33))
        val y = BigInteger(1, raw.copyOfRange(33, 65))
        return KeyFactory.getInstance("EC")
            .generatePublic(ECPublicKeySpec(ECPoint(x, y), ecParams)) as ECPublicKey
    }

    private fun ECPoint.toUncompressedBytes(): ByteArray {
        val x = affineX.toFixedBytes(32)
        val y = affineY.toFixedBytes(32)
        return byteArrayOf(0x04) + x + y
    }

    private fun BigInteger.toFixedBytes(size: Int): ByteArray {
        val raw = toByteArray()
        return when {
            raw.size == size -> raw
            raw.size == size + 1 && raw[0] == 0.toByte() -> raw.copyOfRange(1, raw.size)
            raw.size < size -> ByteArray(size - raw.size) + raw
            else -> error("Integer too large for $size bytes")
        }
    }
}