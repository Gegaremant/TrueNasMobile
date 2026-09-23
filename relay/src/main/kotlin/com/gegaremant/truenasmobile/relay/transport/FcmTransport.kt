package com.gegaremant.truenasmobile.relay.transport

import com.gegaremant.truenasmobile.relay.model.Device
import com.gegaremant.truenasmobile.relay.model.Envelope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64

/**
 * Firebase Cloud Messaging HTTP v1 transport.
 *
 * The relay authenticates with a Firebase service account and pushes the same
 * ciphertext [Envelope] as a `data` message, so it still never touches
 * plaintext. Configure via env:
 *
 *  - `FCM_SERVICE_ACCOUNT_FILE` — path to the Firebase service-account JSON.
 *    When unset the transport is present but every push fails with a clear
 *    "not configured" error instead of crashing the router.
 *  - `FCM_PROJECT_ID` (optional) — overrides the `project_id` from the JSON.
 */
class FcmTransport internal constructor(
    private val config: FcmConfig?,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
) : PushTransport {

    override val name: String = "fcm"

    @Volatile
    private var cachedToken: Pair<String, Long>? = null // (accessToken, expiresAtSec)

    constructor() : this(FcmConfig.fromEnvironment())

    override fun push(device: Device, envelope: Envelope): Result<Unit> = runCatching {
        val cfg = config ?: error(
            "FCM transport not configured: set FCM_SERVICE_ACCOUNT_FILE " +
                "(Firebase service account JSON) and optionally FCM_PROJECT_ID"
        )

        val accessToken = accessToken(cfg)
        val body = json.encodeToString(
            buildJsonObject {
                put(
                    "message",
                    buildJsonObject {
                        put("token", device.pushToken)
                        put(
                            "data",
                            buildJsonObject {
                                put("envelope", json.encodeToString(Envelope.serializer(), envelope))
                            }
                        )
                    }
                )
            }
        )

        val request = HttpRequest.newBuilder(fcmSendUrl(cfg.projectId))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $accessToken")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "FCM responded ${response.statusCode()}: ${response.body().take(300)}"
        }
    }

    /** Short-lived OAuth2 token, cached until ~1 minute before expiry. */
    private fun accessToken(config: FcmConfig): String {
        val nowSec = System.currentTimeMillis() / 1000
        cachedToken?.let { (token, expiresAt) ->
            if (expiresAt - nowSec > 60) return token
        }

        val jwt = FcmJwt.signed(
            issuer = config.clientEmail,
            audience = config.tokenUri,
            privateKeyPem = config.privateKeyPem
        )
        val form = "grant_type=${enc("urn:ietf:params:oauth:grant-type:jwt-bearer")}&assertion=$jwt"

        val request = HttpRequest.newBuilder(URI(config.tokenUri))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "OAuth2 token endpoint responded ${response.statusCode()}: ${response.body().take(300)}"
        }

        val tokenJson = Json { ignoreUnknownKeys = true }
            .decodeFromString<OAuth2TokenResponse>(response.body())
        require(tokenJson.access_token.isNotBlank()) { "OAuth2 response has no access_token" }

        val expiresAt = nowSec + (tokenJson.expires_in ?: 3600)
        cachedToken = tokenJson.access_token to expiresAt
        return tokenJson.access_token
    }

    private fun fcmSendUrl(projectId: String) =
        URI("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")

    private fun enc(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
data class OAuth2TokenResponse(
    val access_token: String = "",
    val expires_in: Long? = null
)

/**
 * Configuration parsed from a Firebase service-account JSON.
 * Kept in raw form so [FcmTransport] can mint its own OAuth2 tokens.
 */
data class FcmConfig(
    val projectId: String,
    val clientEmail: String,
    val privateKeyPem: String,
    val tokenUri: String
) {
    companion object {
        /** Reads FCM_SERVICE_ACCOUNT_FILE (and optional FCM_PROJECT_ID). */
        fun fromEnvironment(): FcmConfig? {
            val path = System.getenv("FCM_SERVICE_ACCOUNT_FILE")?.trim()
            if (path.isNullOrEmpty()) return null
            return try {
                fromJson(Files.readString(Path.of(path)))
            } catch (e: Exception) {
                System.err.println("[relay] FCM_SERVICE_ACCOUNT_FILE unreadable: ${e.message}")
                null
            }
        }

        fun fromJson(text: String, overrideProject: String? = null): FcmConfig? = try {
            val sa = Json { ignoreUnknownKeys = true }
                .decodeFromString<ServiceAccountJson>(text)
            FcmConfig(
                projectId = overrideProject?.takeIf { it.isNotEmpty() } ?: sa.project_id,
                clientEmail = sa.client_email,
                privateKeyPem = sa.private_key,
                tokenUri = sa.token_uri
            )
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class ServiceAccountJson(
    val project_id: String,
    val client_email: String,
    val private_key: String,
    val token_uri: String = "https://oauth2.googleapis.com/token"
)

/**
 * Self-issued RS256 JWT used to exchange for an OAuth2 access token
 * (urn:ietf:params:oauth:grant-type:jwt-bearer). Pure JDK crypto, no
 * google-auth dependency.
 */
internal object FcmJwt {
    private val json = Json

    internal fun signed(issuer: String, audience: String, privateKeyPem: String): String {
        val now = System.currentTimeMillis() / 1000
        val header = """{"alg":"RS256","typ":"JWT"}"""
        val claims = json.encodeToString(
            buildJsonObject {
                put("iss", issuer)
                put("sub", issuer)
                put("aud", audience)
                put("iat", now)
                put("exp", now + 3600)
                put("scope", "https://www.googleapis.com/auth/firebase.messaging")
            }
        )

        val signingInput = "${b64url(header.toByteArray(StandardCharsets.US_ASCII))}." +
            b64url(claims.toByteArray(StandardCharsets.US_ASCII))
        return "$signingInput.${signature(signingInput, parsePrivateKey(privateKeyPem))}"
    }

    /** Base64url (no padding) RS256 signature over [input] with [key]. */
    internal fun signature(input: String, key: PrivateKey): String {
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(key)
        signer.update(input.toByteArray(StandardCharsets.US_ASCII))
        return b64url(signer.sign())
    }

    internal fun parsePrivateKey(pem: String): PrivateKey {
        val body = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace(Regex("\\s"), "")
        val spec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(body))
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    private fun b64url(data: ByteArray) =
        Base64.getUrlEncoder().withoutPadding().encodeToString(data)
}