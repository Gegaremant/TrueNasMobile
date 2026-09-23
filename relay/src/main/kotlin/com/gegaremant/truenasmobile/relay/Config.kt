package com.gegaremant.truenasmobile.relay

import java.nio.file.Path

/**
 * Environment-driven configuration.
 *
 * | Var              | Default          | Meaning                                        |
 * |------------------|------------------|------------------------------------------------|
 * | PORT             | 8080             | HTTP listen port                               |
 * | RELAY_TOKENS     | (empty = dev)    | Comma-separated relay tokens. Webhooks and     |
 * |                  |                  | device registration require "Bearer <token>".  |
 * |                  |                  | Empty => any non-blank token accepted (dev).   |
 * | RELAY_DATA_FILE  | data/devices.json| JSON file holding the device registry.         |
 * | NTFY_URL         | https://ntfy.sh  | Default ntfy server for "ntfy" transport.      |
 * | FCM_SERVICE_ACCOUNT_FILE | (unset) | Path to the Firebase service-account JSON.      |
 * |                  |                  | Enables the "fcm" transport.                   |
 * | FCM_PROJECT_ID   | (from JSON)     | Optional override of the Firebase project id.  |
 */
object Config {
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080

    val tokens: Set<String> = (System.getenv("RELAY_TOKENS") ?: "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    val devMode: Boolean get() = tokens.isEmpty()

    val dataFile: Path = Path.of(System.getenv("RELAY_DATA_FILE") ?: "data/devices.json")

    val ntfyUrl: String = (System.getenv("NTFY_URL") ?: "https://ntfy.sh").trimEnd('/')

    val fcmServiceAccountFile: String? = System.getenv("FCM_SERVICE_ACCOUNT_FILE")?.trim()?.takeIf { it.isNotEmpty() }

    /** True when the configured transport is usable. */
    val transportsAvailable: Set<String> =
        buildSet {
            add("ntfy")
            if (fcmServiceAccountFile != null) add("fcm")
        }
}