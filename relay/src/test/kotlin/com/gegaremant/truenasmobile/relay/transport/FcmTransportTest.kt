package com.gegaremant.truenasmobile.relay.transport

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FcmJwtTest {

    @Test
    fun `signed JWT verifies with the service account public key`() {
        val (privatePem, publicKey) = rsaKeyPair()

        val jwt = FcmJwt.signed(
            issuer = "relay-test@project.iam.gserviceaccount.com",
            audience = "https://oauth2.googleapis.com/token",
            privateKeyPem = privatePem
        )

        val parts = jwt.split(".")
        assertEquals(3, parts.size, "JWT must be header.payload.signature")

        // Payload claims are what we asked for.
        val claims = Json.parseToJsonElement(
            String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        ).jsonObject
        assertEquals("relay-test@project.iam.gserviceaccount.com", claims["iss"]?.jsonPrimitive?.content)
        assertEquals("https://oauth2.googleapis.com/token", claims["aud"]?.jsonPrimitive?.content)
        assertTrue((claims["exp"]!!.jsonPrimitive.long - claims["iat"]!!.jsonPrimitive.long) == 3600L)

        // Signature is verifiable with the matching public key (RS256).
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        verifier.update("${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII))
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(parts[2])), "JWT signature must verify")
    }

    @Test
    fun `parsePrivateKey rejects malformed input`() {
        kotlin.test.assertFailsWith<Exception> {
            FcmJwt.parsePrivateKey("not a pem")
        }
    }

    private fun rsaKeyPair(): Pair<String, RSAPublicKey> {
        val generator = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val pair: KeyPair = generator.generateKeyPair()

        val priv = Base64.getEncoder().encodeToString(pair.private.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$priv\n-----END PRIVATE KEY-----"

        val pubSpec = X509EncodedKeySpec(pair.public.encoded)
        val pub = java.security.KeyFactory.getInstance("RSA").generatePublic(pubSpec) as RSAPublicKey
        return pem to pub
    }
}

class FcmConfigTest {

    @Test
    fun `parses service account json and uses its project id`() {
        val config = FcmConfig.fromJson(
            """
            {
              "type": "service_account",
              "project_id": "truenasmobile-push",
              "client_email": "relay@truenasmobile-push.iam.gserviceaccount.com",
              "private_key": "-----BEGIN PRIVATE KEY-----\nMIIABC\n-----END PRIVATE KEY-----",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
            """.trimIndent()
        )

        assertNotNull(config)
        val cfg = config
        assertEquals("truenasmobile-push", cfg.projectId)
        assertEquals("relay@truenasmobile-push.iam.gserviceaccount.com", cfg.clientEmail)
        assertEquals("https://oauth2.googleapis.com/token", cfg.tokenUri)
    }

    @Test
    fun `project override wins over json value`() {
        val config = FcmConfig.fromJson(
            """{"project_id":"from-json","client_email":"a@b.iam.gserviceaccount.com","private_key":"x"}""",
            overrideProject = "from-env"
        )
        assertEquals("from-env", config!!.projectId)
    }

    @Test
    fun `garbage json yields null, no crash`() {
        assertEquals(null, FcmConfig.fromJson("not json at all"))
    }
}

class FcmTransportTest {

    @Test
    fun `push without configuration fails with a clear message`() {
        val transport = FcmTransport(config = null)
        val result = transport.push(
            device = com.gegaremant.truenasmobile.relay.model.Device(
                name = "test",
                pubkey = "x",
                transport = "fcm",
                pushToken = "token",
                relayToken = "t"
            ),
            envelope = com.gegaremant.truenasmobile.relay.model.Envelope(eph = "x", ct = "y")
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not configured") == true)
    }
}