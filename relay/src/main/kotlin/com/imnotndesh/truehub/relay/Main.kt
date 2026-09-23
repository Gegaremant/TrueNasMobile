package com.imnotndesh.truehub.relay

import com.imnotndesh.truehub.relay.store.DeviceStore
import com.imnotndesh.truehub.relay.transport.FcmTransport
import com.imnotndesh.truehub.relay.transport.NtfyTransport
import com.imnotndesh.truehub.relay.transport.PushRouter
import com.imnotndesh.truehub.relay.web.RelayServer

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