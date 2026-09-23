package com.gegaremant.truenasmobile.relay

import com.gegaremant.truenasmobile.relay.store.DeviceStore
import com.gegaremant.truenasmobile.relay.transport.FcmTransport
import com.gegaremant.truenasmobile.relay.transport.NtfyTransport
import com.gegaremant.truenasmobile.relay.transport.PushRouter
import com.gegaremant.truenasmobile.relay.web.RelayServer

fun main() {
    if (Config.devMode) {
        println("[relay] WARNING: RELAY_TOKENS is empty — running in DEV MODE (any token accepted). Set RELAY_TOKENS in production.")
    }

    val store = DeviceStore()
    val router = PushRouter(
        transports = mapOf(
            "ntfy" to NtfyTransport(),
            "fcm" to FcmTransport()
        )
    )

    RelayServer(store, router).start()
    println("[relay] device registry: ${Config.dataFile} (${store.count()} devices)")
    println(
        "[relay] transports: ntfy -> ${Config.ntfyUrl} | fcm -> " +
            if (Config.fcmServiceAccountFile != null) "configured (${Config.fcmServiceAccountFile})"
            else "not configured (set FCM_SERVICE_ACCOUNT_FILE to enable)"
    )
}