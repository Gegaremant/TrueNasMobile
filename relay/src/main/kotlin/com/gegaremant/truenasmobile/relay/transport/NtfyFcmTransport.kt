package com.gegaremant.truenasmobile.relay.transport

import com.gegaremant.truenasmobile.relay.Config
import com.gegaremant.truenasmobile.relay.model.Device
import com.gegaremant.truenasmobile.relay.model.Envelope
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Publishes the ciphertext envelope to an ntfy-compatible server.
 *
 * The topic never transports plaintext: the envelope body is encrypted by
 * [com.gegaremant.truenasmobile.relay.crypto.E2E] and can only be opened on the
 * device. ntfy is only ever a (potentially public) mailbox for ciphertext.
 */
class NtfyTransport(
    private val server: String = Config.ntfyUrl
) : PushTransport {

    override val name: String = "ntfy"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    override fun push(device: Device, envelope: Envelope): Result<Unit> = runCatching {
        val topic = device.pushToken.trim().trimStart('/')
        require(topic.isNotBlank() && !topic.contains('/')) { "ntfy topic must be a plain name" }

        // The mailbox entry is the raw ciphertext envelope JSON (no wrapper):
        // the Android client parses it directly from the ntfy event message.
        val body = json.encodeToString(Envelope.serializer(), envelope)

        val request = HttpRequest.newBuilder(URI("$server/$topic"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "text/plain; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "ntfy responded ${response.statusCode()}: ${response.body().take(200)}"
        }
    }
}