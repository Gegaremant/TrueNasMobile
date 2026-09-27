package com.gegaremant.truenasmobile.data.helpers

import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import java.util.concurrent.ConcurrentHashMap

/**
 * One authenticated [TrueNASApiManager] per account, kept for the life of the
 * process.
 *
 * Switching profiles used to mean, every single time: a fresh WebSocket
 * handshake, a full `auth.login`, and an `auth.generate_token` - three round
 * trips before the app could start drawing, repeated on every visit to the same
 * NAS. Nothing was ever reused.
 *
 * An entry here is an already-connected, already-authenticated manager, so a
 * switch to a profile that is present costs no handshake and no login at all.
 * Entries come from [remember] on a successful login and from the background
 * warming in `MainViewModel.warmOtherProfiles`, so a profile the user never
 * tapped is usually warm too.
 *
 * Nothing here talks to storage: this is a cache of live sockets, not a
 * persistence layer. A token that outlives the process is
 * `MultiAccountPrefs`' business, and the server rejects a stale token anyway, so
 * a missing entry only ever costs an ordinary login.
 */
object AccountSessionRegistry {

    /**
     * How long an entry may sit unused before [acquire] spends a round trip
     * proving it is still there.
     *
     * The local connection state cannot be trusted on its own for a socket that
     * has been idle: a network path change can kill a connection without a
     * close frame ever arriving, leaving the client convinced it is connected.
     * Handing that out would make the dashboard hang instead of falling back to
     * a login.
     *
     * A `core.ping` is one round trip on an already-open socket - no handshake,
     * no login - so verifying costs far less than the login this is avoiding,
     * and anything fresher than this is left alone so the common case stays
     * free.
     */
    private const val STALE_AFTER_MILLIS = 3 * 60 * 1000L

    private val sessions = ConcurrentHashMap<String, TrueNASApiManager>()
    private val lastProvenAlive = ConcurrentHashMap<String, Long>()

    /** Account ids that currently hold a manager, connected or not. */
    val trackedAccountIds: Set<String>
        get() = sessions.keys.toSet()

    /**
     * A manager for [accountId] that is ready to serve calls, or null if the
     * caller has to log in.
     *
     * Cheap for a recently used entry - a map read and nothing else. An entry
     * that has been idle gets one `core.ping` first, and a failed ping drops it
     * rather than handing out a corpse.
     */
    suspend fun acquire(accountId: String): TrueNASApiManager? {
        val manager = sessions[accountId] ?: return null
        if (!isConnected(manager)) {
            forget(accountId)
            return null
        }

        val provenAt = lastProvenAlive[accountId] ?: 0L
        val age = System.currentTimeMillis() - provenAt
        if (provenAt != 0L && age < STALE_AFTER_MILLIS) return manager

        return when (ping(manager)) {
            true -> {
                lastProvenAlive[accountId] = System.currentTimeMillis()
                manager
            }
            else -> {
                forget(accountId)
                null
            }
        }
    }

    fun remember(accountId: String, manager: TrueNASApiManager) {
        sessions[accountId] = manager
        // A manager handed over by a login has just proved itself, so it does
        // not need a ping before its first use.
        lastProvenAlive[accountId] = System.currentTimeMillis()
    }

    /**
     * Records that [accountId]'s manager answered a call, so a later [acquire]
     * can skip the verification round trip. Safe to call for unknown accounts.
     */
    fun markAlive(accountId: String) {
        if (sessions.containsKey(accountId)) {
            lastProvenAlive[accountId] = System.currentTimeMillis()
        }
    }

    /** Drops one account's session and closes its socket. */
    suspend fun forget(accountId: String) {
        lastProvenAlive.remove(accountId)
        sessions.remove(accountId)?.let { closeQuietly(it) }
    }

    /**
     * Drops every session and closes every socket.
     *
     * Signing out has to call this: the managers hold authenticated sockets to
     * servers the user just said goodbye to, and the process is long-lived.
     */
    suspend fun forgetAll() {
        val live = sessions.values.toList()
        sessions.clear()
        lastProvenAlive.clear()
        live.forEach { closeQuietly(it) }
    }

    private suspend fun isConnected(manager: TrueNASApiManager): Boolean = try {
        manager.isConnected()
    } catch (_: Exception) {
        false
    }

    private suspend fun ping(manager: TrueNASApiManager): Boolean = try {
        manager.connection.pingConnectionWithResult() is ApiResult.Success
    } catch (_: Exception) {
        false
    }

    private suspend fun closeQuietly(manager: TrueNASApiManager) {
        try {
            manager.disconnect()
        } catch (_: Exception) {
            // Closing a socket that is already gone is not worth reporting.
        }
    }
}
