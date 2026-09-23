package com.gegaremant.truenasmobile.relay.transport

import com.gegaremant.truenasmobile.relay.model.Device
import com.gegaremant.truenasmobile.relay.model.Envelope

/**
 * A push transport. The relay is transport-agnostic: the device chooses
 * "ntfy" (FOSS, works everywhere) or "fcm" (Firebase, Google Play devices)
 * at registration, and the relay just routes the same ciphertext envelope.
 */
interface PushTransport {
    val name: String
    fun push(device: Device, envelope: Envelope): Result<Unit>
}

class PushRouter(private val transports: Map<String, PushTransport>) {

    fun transportFor(device: Device): PushTransport? = transports[device.transport]

    fun pushAll(devices: List<Device>, envelope: Envelope): PushSummary {
        var delivered = 0
        val failures = mutableListOf<String>()

        for (device in devices) {
            transportFor(device)?.let { transport ->
                transport.push(device, envelope)
                    .onSuccess { delivered++ }
                    .onFailure { failures.add("${device.id}:${it.message}") }
            } ?: failures.add("${device.id}:unknown transport '${device.transport}'")
        }
        return PushSummary(delivered, failures)
    }
}

data class PushSummary(val delivered: Int, val failures: List<String>) {
    val ok: Boolean get() = failures.isEmpty()
}