package com.imnotndesh.truehub.relay.store

import com.imnotndesh.truehub.relay.Config
import com.imnotndesh.truehub.relay.model.Device
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe device registry persisted as a JSON file.
 *
 * Good enough for a self-hosted relay (up to low thousands of devices).
 * If the public/community relay outgrows this, swap the backend for SQLite
 * without touching the HTTP layer.
 */
class DeviceStore(private val file: Path = Config.dataFile) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val lock = ReentrantLock()

    private var devices: List<Device> = load()

    init {
        Files.createDirectories(file.parent ?: file.toAbsolutePath().parent)
    }

    private fun load(): List<Device> {
        return if (Files.exists(file)) {
            try {
                json.decodeFromString(ListSerializer(Device.serializer()), Files.readString(file))
            } catch (e: Exception) {
                System.err.println("[relay] Failed to parse $file, starting empty: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun register(device: Device) = lock.withLock {
        // Re-registering the same device on the same token replaces the old entry.
        devices = devices.filterNot {
            it.relayToken == device.relayToken && it.pushToken == device.pushToken
        } + device
        persist()
        device.id
    }

    fun byToken(relayToken: String): List<Device> = lock.withLock {
        devices.filter { it.relayToken == relayToken }
    }

    fun remove(deviceId: String) = lock.withLock {
        devices = devices.filterNot { it.id == deviceId }
        persist()
    }

    fun count(): Int = lock.withLock { devices.size }

    private fun persist() {
        val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
        try {
            Files.writeString(tmp, json.encodeToString(ListSerializer(Device.serializer()), devices))
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            // ATOMIC_MOVE may be unsupported on some filesystems; retry without it.
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
            } catch (e2: IOException) {
                System.err.println("[relay] Failed to persist device store: ${e2.message}")
            }
        }
    }
}