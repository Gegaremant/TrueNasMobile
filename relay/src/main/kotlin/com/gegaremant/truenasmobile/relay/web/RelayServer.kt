package com.gegaremant.truenasmobile.relay.web

import com.gegaremant.truenasmobile.relay.Config
import com.gegaremant.truenasmobile.relay.crypto.E2E
import com.gegaremant.truenasmobile.relay.model.ApiResponse
import com.gegaremant.truenasmobile.relay.model.Device
import com.gegaremant.truenasmobile.relay.model.RegisterDeviceRequest
import com.gegaremant.truenasmobile.relay.store.DeviceStore
import com.gegaremant.truenasmobile.relay.transport.PushRouter
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * Minimal HTTP API on top of the JDK's built-in [HttpServer] — deliberately
 * dependency-free for a small self-hosted daemon. Routes:
 *
 *   GET  /healthz                 -> 200 {"ok":true}
 *   POST /api/device/register     -> register a device under the bearer token
 *   POST /api/send                -> TrueNAS alert webhook (opaque JSON body)
 *
 * Auth: "Authorization: Bearer <relayToken>". An empty RELAY_TOKENS config
 * enables dev mode (any non-blank token).
 */
class RelayServer(
    private val store: DeviceStore,
    private val router: PushRouter,
    private val port: Int = Config.port
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun start() {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.executor = Executors.newFixedThreadPool(8)

        server.createContext("/healthz") { exchange -> respond(exchange, 200, ApiResponse(true)) }

        server.createContext("/api/device/register") { exchange ->
            handle(exchange) { token ->
                requireMethod(exchange, "POST")
                val req = parseBody<RegisterDeviceRequest>(exchange)
                require(req.pubkey.isNotBlank() && req.pushToken.isNotBlank()) {
                    "pubkey and pushToken are required"
                }

                val device = Device(
                    name = req.name.ifBlank { "device" },
                    pubkey = req.pubkey,
                    transport = req.transport,
                    pushToken = req.pushToken,
                    relayToken = token
                )
                val id = store.register(device)
                respond(exchange, 200, ApiResponse(ok = true, deviceId = id))
            }
        }

        server.createContext("/api/send") { exchange ->
            handle(exchange) { token ->
                requireMethod(exchange, "POST")
                val body = exchange.requestBody.readBytes()
                require(body.isNotEmpty()) { "empty webhook body" }

                val devices = store.byToken(token)
                require(devices.isNotEmpty()) { "no devices registered for this token" }

                // One envelope per device: each is encrypted to that device's own
                // public key, so no two devices can open each other's ciphertext.
                var delivered = 0
                val failures = mutableListOf<String>()
                for (device in devices) {
                    val envelope = E2E.encrypt(device.pubkey, body)
                    if (router.transportFor(device) == null) {
                        failures.add("${device.id}:unknown transport '${device.transport}'")
                        continue
                    }
                    router.transportFor(device)!!.push(device, envelope)
                        .onSuccess { delivered++ }
                        .onFailure { failures.add("${device.id}:${it.message}") }
                }

                respond(
                    exchange,
                    if (failures.isEmpty()) 200 else 502,
                    ApiResponse(ok = failures.isEmpty(), delivered = delivered)
                )
            }
        }

        server.start()
        println("[relay] truenasmobile-relay listening on :$port" +
            (if (Config.devMode) " (DEV MODE: any token accepted)" else " (tokens: ${Config.tokens.size})"))
    }

    // ---- plumbing ----

    private fun handle(exchange: HttpExchange, block: (String) -> Unit) {
        try {
            val token = bearerToken(exchange)
                ?: throw UnauthorizedException("missing Authorization: Bearer <token>")

            if (!Config.devMode && token !in Config.tokens) {
                throw UnauthorizedException("unknown relay token")
            }
            block(token)
        } catch (e: UnauthorizedException) {
            respond(exchange, 401, ApiResponse(false, e.message))
        } catch (e: IllegalArgumentException) {
            respond(exchange, 400, ApiResponse(false, e.message))
        } catch (e: Exception) {
            System.err.println("[relay] error handling ${exchange.requestURI}: ${e.message}")
            respond(exchange, 500, ApiResponse(false, "internal error"))
        }
    }

    private fun bearerToken(exchange: HttpExchange): String? {
        val header = exchange.requestHeaders.getFirst("Authorization") ?: return null
        val parts = header.split(" ")
        return if (parts.size == 2 && parts[0].equals("Bearer", ignoreCase = true)) parts[1] else null
    }

    private fun requireMethod(exchange: HttpExchange, expected: String) {
        if (exchange.requestMethod != expected) {
            throw IllegalArgumentException("method ${exchange.requestMethod} not allowed")
        }
    }

    private inline fun <reified T> parseBody(exchange: HttpExchange): T {
        val body = exchange.requestBody.readBytes()
        return json.decodeFromString(serializer<T>(), body.decodeToString())
    }

    private fun respond(exchange: HttpExchange, status: Int, body: ApiResponse) {
        val bytes = json.encodeToString(ApiResponse.serializer(), body).toByteArray()
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private class UnauthorizedException(message: String) : RuntimeException(message)
}