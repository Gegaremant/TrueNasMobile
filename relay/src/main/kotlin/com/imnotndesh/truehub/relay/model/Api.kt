package com.imnotndesh.truehub.relay.model

import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/device/register.
 *
 * @param pubkey Base64 of the device's EC P-256 public key as a raw 65-byte
 *               uncompressed point (0x04 || X(32) || Y(32)). The matching
 *               private key never leaves the device, so the relay can push
 *               to it without ever being able to read the payload.
 * @param transport "ntfy" (any ntfy-compatible server) or "fcm" (Firebase).
 * @param pushToken For "ntfy": the topic name to publish to (on NTFY_URL, or
 *                  ntfy.sh by default). For "fcm": the device registration token.
 */
@Serializable
data class RegisterDeviceRequest(
    val name: String,
    val pubkey: String,
    val transport: String,
    val pushToken: String
)

@Serializable
data class Device(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val pubkey: String,
    val transport: String,
    val pushToken: String,
    val relayToken: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Ciphertext envelope pushed to the device. The relay never sees the plaintext:
 * it only ever holds the device's public key (see [RegisterDeviceRequest.pubkey]).
 */
@Serializable
data class Envelope(
    val v: Int = 1,
    /** Base64 ephemeral EC P-256 public key (raw uncompressed 65-byte point). */
    val eph: String,
    /** Base64 of IV(12) || AES-256-GCM ciphertext (+ 16-byte tag). */
    val ct: String,
    val ts: Long = System.currentTimeMillis()
)

@Serializable
data class ApiResponse(
    val ok: Boolean,
    val error: String? = null,
    val deviceId: String? = null,
    val delivered: Int? = null
)