package com.imnotndesh.truehub.data.push

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Subscribes to the device's ntfy topic over WebSocket and hands incoming
 * ciphertext envelopes to [onEnvelope]. Reconnects with exponential backoff;
 * the topic carries only ciphertext, so losing the connection leaks nothing.
 */
class NtfyPushClient(
    private val ntfyBaseUrl: String,
    private val topic: String,
    private val scope: CoroutineScope,
    private val onEnvelope: (PushEnvelope) -> Unit
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // stream: never stop reading
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    @Volatile
    private var stopped = false

    fun start() {
        stopped = false
        connect()
    }

    fun stop() {
        stopped = true
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "stopping")
        webSocket = null
    }

    private fun connect() {
        val request = Request.Builder().url(wsUrl())
            .addHeader("Accept", "application/json")
            .build()
        webSocket = client.newWebSocket(request, listener)
    }

    private fun wsUrl(): String {
        val normalized = ntfyBaseUrl.trimEnd('/')
        val wsBase = normalized
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        return "$wsBase/$topic/ws"
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempt = 0
            Log.i(TAG, "Connected to ntfy topic '$topic'")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            reconnectAttempt = 0
            val event = runCatching { JSONObject(text) }.getOrNull() ?: return
            if (event.optString("event") != "message") return

            val envelopeJson = event.optString("message")
            if (envelopeJson.isBlank()) return

            val envelope = runCatching { PushEnvelope.parse(envelopeJson) }.getOrNull()
            if (envelope != null) {
                onEnvelope(envelope)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "ntfy connection failed: ${t.message}")
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "ntfy connection closed ($code $reason)")
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (stopped) return
        if (reconnectJob?.isActive == true) return
        val attempt = reconnectAttempt++
        reconnectJob = scope.launch {
            val backoffMs = (5_000L shl attempt.coerceAtMost(3)).coerceAtMost(60_000L)
            delay(backoffMs)
            if (isActive && !stopped) {
                connect()
            }
        }
    }

    private companion object {
        const val TAG = "NtfyPushClient"
    }
}