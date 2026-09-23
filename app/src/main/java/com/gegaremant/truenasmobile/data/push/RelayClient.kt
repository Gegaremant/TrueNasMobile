package com.gegaremant.truenasmobile.data.push

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal HTTP client for the self-hosted relay (`relay/` module):
 *
 *   GET  /healthz             -> connectivity probe
 *   POST /api/device/register -> register this device's E2E public key
 *   POST /api/send            -> inject a test alert webhook
 *
 * All requests are authenticated with "Authorization: Bearer <relayToken>".
 */
class RelayClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    sealed class RelayResult {
        data class Success(val body: JSONObject) : RelayResult()
        data class Error(val message: String) : RelayResult()
    }

    fun healthz(relayUrl: String): RelayResult = runCatching {
        call("GET", "${relayUrl.trimEnd('/')}/healthz", token = null, jsonBody = null)
    }.getOrElse { RelayResult.Error(it.message ?: "network error") }

    fun register(relayUrl: String, token: String, registration: JSONObject): RelayResult = runCatching {
        call("POST", "${relayUrl.trimEnd('/')}/api/device/register", token = token, jsonBody = registration)
    }.getOrElse { RelayResult.Error(it.message ?: "network error") }

    /** Posts an arbitrary alert payload to the relay to verify the full E2E path. */
    fun sendTest(relayUrl: String, token: String, payload: JSONObject): RelayResult = runCatching {
        call("POST", "${relayUrl.trimEnd('/')}/api/send", token = token, jsonBody = payload)
    }.getOrElse { RelayResult.Error(it.message ?: "network error") }

    private fun call(method: String, url: String, token: String?, jsonBody: JSONObject?): RelayResult {
        val requestBody = jsonBody?.toString()?.toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url(url)
            .method(method, requestBody)
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        requestBuilder.header("Accept", "application/json")

        client.newCall(requestBuilder.build()).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) {
                return RelayResult.Error("HTTP ${response.code}: ${text.take(300)}")
            }
            return RelayResult.Success(JSONObject(text.ifBlank { "{}" }))
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}