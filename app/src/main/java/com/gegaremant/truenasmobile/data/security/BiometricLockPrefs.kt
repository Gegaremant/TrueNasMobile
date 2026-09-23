package com.gegaremant.truenasmobile.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

/**
 * Local app-lock preference (biometric / device credential).
 * Stored in the same secure DataStore as account secrets.
 */
object BiometricLockPrefs {
    private val biometricLockEnabledKey = booleanPreferencesKey("biometric_lock_enabled")

    suspend fun isEnabled(store: DataStore<Preferences>): Boolean {
        return store.data.first()[biometricLockEnabledKey] ?: false
    }

    suspend fun setEnabled(store: DataStore<Preferences>, enabled: Boolean) {
        store.edit {
            it[biometricLockEnabledKey] = enabled
        }
    }
}