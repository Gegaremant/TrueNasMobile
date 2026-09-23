package com.gegaremant.truenasmobile.data.push

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.pushDataStore: DataStore<Preferences> by preferencesDataStore(name = "push_preferences")

/**
 * Persisted configuration for the push notification pipeline
 * (TrueNAS webhook -> relay -> ntfy/FCM -> this device).
 */
data class PushConfig(
    val enabled: Boolean = false,
    val relayUrl: String = "",
    val relayToken: String = "",
    val transport: String = "ntfy",
    val ntfyBaseUrl: String = "https://ntfy.sh",
    val ntfyTopic: String = "",
    val deviceId: String = ""
)

object PushPrefs {

    val enabledKey = booleanPreferencesKey("enabled")

    /** Public relay endpoint, e.g. "https://push.example.com". */
    val relayUrlKey = stringPreferencesKey("relay_url")

    /** Per-token used for device registration against the relay. */
    val relayTokenKey = stringPreferencesKey("relay_token")

    /** "ntfy" (default, GMS-free) or "fcm" (Firebase, needs a Firebase project). */
    val transportKey = stringPreferencesKey("push_transport")

    /** ntfy-compatible server used as the ciphertext mailbox. */
    val ntfyBaseUrlKey = stringPreferencesKey("ntfy_base_url")

    /** Random mailbox name on the ntfy server where the relay deposits envelopes. */
    val ntfyTopicKey = stringPreferencesKey("ntfy_topic")

    /** Device id returned by the relay upon registration. */
    val deviceIdKey = stringPreferencesKey("device_id")

    fun enabledFlow(context: Context): Flow<Boolean> =
        context.pushDataStore.data.map { it[enabledKey] ?: false }

    suspend fun isEnabled(context: Context): Boolean =
        context.pushDataStore.data.first()[enabledKey] ?: false

    suspend fun getConfig(context: Context): PushConfig {
        val prefs = context.pushDataStore.data.first()
        return PushConfig(
            enabled = prefs[enabledKey] ?: false,
            relayUrl = prefs[relayUrlKey].orEmpty(),
            relayToken = prefs[relayTokenKey].orEmpty(),
            transport = prefs[transportKey] ?: "ntfy",
            ntfyBaseUrl = prefs[ntfyBaseUrlKey] ?: "https://ntfy.sh",
            ntfyTopic = prefs[ntfyTopicKey].orEmpty(),
            deviceId = prefs[deviceIdKey].orEmpty()
        )
    }

    suspend fun saveConfig(context: Context, config: PushConfig) {
        context.pushDataStore.edit {
            it[enabledKey] = config.enabled
            it[relayUrlKey] = config.relayUrl
            it[relayTokenKey] = config.relayToken
            it[transportKey] = config.transport
            it[ntfyBaseUrlKey] = config.ntfyBaseUrl
            it[ntfyTopicKey] = config.ntfyTopic
            it[deviceIdKey] = config.deviceId
        }
    }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.pushDataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun clear(context: Context) {
        context.pushDataStore.edit { it.clear() }
    }
}