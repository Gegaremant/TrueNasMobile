package com.imnotndesh.truehub.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BiometricLockPrefsTest {

    private fun createStore(file: File): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        return PreferenceDataStoreFactory.create(scope = scope) { file }
    }

    @Test
    fun defaultsToDisabled() = runBlocking {
        val dir = kotlin.io.path.createTempDirectory("biometric-prefs").toFile()
        try {
            val store = createStore(File(dir, "prefs.preferences_pb"))
            assertFalse(BiometricLockPrefs.isEnabled(store))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun roundTripEnabledDisabled() = runBlocking {
        val dir = kotlin.io.path.createTempDirectory("biometric-prefs").toFile()
        try {
            val store = createStore(File(dir, "prefs.preferences_pb"))
            BiometricLockPrefs.setEnabled(store, true)
            assertTrue("flag should read back as enabled", BiometricLockPrefs.isEnabled(store))

            BiometricLockPrefs.setEnabled(store, false)
            assertFalse("flag should read back as disabled", BiometricLockPrefs.isEnabled(store))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun persistsAcrossStoreInstances() = runBlocking {
        val dir = kotlin.io.path.createTempDirectory("biometric-prefs").toFile()
        try {
            val file = File(dir, "prefs.preferences_pb")

            val writerScope = CoroutineScope(Dispatchers.IO + Job())
            val writer = PreferenceDataStoreFactory.create(scope = writerScope) { file }
            BiometricLockPrefs.setEnabled(writer, true)
            writerScope.cancel()

            // A fresh DataStore instance (as after a process restart) must see the value.
            val readerScope = CoroutineScope(Dispatchers.IO + Job())
            val reader = PreferenceDataStoreFactory.create(scope = readerScope) { file }
            try {
                assertTrue("flag must survive a DataStore instance restart", BiometricLockPrefs.isEnabled(reader))
            } finally {
                readerScope.cancel()
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}