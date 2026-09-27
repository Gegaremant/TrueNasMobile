package com.gegaremant.truenasmobile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards what has to be true for profile switching to be correct.
 *
 * The switcher used to move the session without moving the app's idea of who is
 * logged in. `MultiAccountPrefs.getLastUsedProfile` is read in seventeen places
 * - app start, both widget activities, the AI app functions, the alert, job and
 * sync workers, and session recovery - and only the login screen ever wrote it.
 * So after switching profiles the app still treated the previous NAS as active:
 * a relaunch went back to the old account, the background workers polled its
 * pools and sent its alerts, and an expired session recovered with the wrong
 * credentials. Nothing about that is visible in the switcher UI, so it is
 * checked here.
 */
class AccountSwitchingTest {

    private fun source(path: String) = File("src/main/java/com/gegaremant/truenasmobile/$path").readText()

    private val mainViewModel = source("MainViewModel.kt")
    private val switcherScreen = source("ui/account/AccountSwitcherScreen.kt")
    private val settingsViewModel = source("ui/settings/SettingsScreenViewModel.kt")

    @Test
    fun `switching a profile moves the last-used pointer`() {
        // Count the writers, not the readers: only one file may write it, and it
        // has to be the login path plus the switch.
        val writers = listOf("MainViewModel.kt", "ui/login/LoginScreenViewModel.kt")
            .filter { source(it).contains("saveLastUsedProfile") }
        assertTrue(
            "expected both the login path and the profile switch to write the " +
                "last-used pointer, only found it in $writers",
            writers.containsAll(listOf("MainViewModel.kt", "ui/login/LoginScreenViewModel.kt"))
        )

        val switchBody = mainViewModel.substringAfter("private suspend fun activateProfile(")
        assertTrue(
            "activateProfile is the single place a switch is committed, and it " +
                "has to move the last-used pointer or every worker keeps talking " +
                "to the previous server",
            switchBody.contains("saveLastUsedProfile")
        )
        assertTrue(
            "a switch has to drop the shared AppCache: its single bucket holds " +
                "the pools, apps and shares of the server just left behind",
            switchBody.contains("AppCache.clearAllCache()")
        )
    }

    @Test
    fun `the switcher reuses a warm session instead of logging in again`() {
        val switchBody = mainViewModel.substringAfter("suspend fun attemptLoginWithProfile(")
            .substringBefore("private suspend fun activateProfile(")
        assertTrue(
            "attemptLoginWithProfile must try the registry before building a " +
                "new client, that is the whole point of the fast path",
            switchBody.contains("AccountSessionRegistry.acquire(")
        )
        val warm = switchBody.substringBefore("return try {")
        assertTrue(
            "the fast path has to return before the login branch, otherwise it " +
                "is dead code",
            warm.contains("activateProfile")
        )
    }

    @Test
    fun `signing out closes every warm socket`() {
        val forgets = settingsViewModel.split("AccountSessionRegistry.forgetAll()").size - 1
        assertTrue(
            "both sign-out paths must drop the registry: the managers hold " +
                "authenticated sockets to servers the user just left",
            forgets >= 2
        )
    }

    @Test
    fun `deleting a profile closes its socket`() {
        assertTrue(
            "deleting a saved profile has to forget its session, otherwise the " +
                "app keeps a live authenticated socket to a server whose " +
                "credentials were just erased",
            switcherScreen.contains("AccountSessionRegistry.forget(")
        )
    }

    @Test
    fun `warming cannot become the active profile`() {
        val warm = mainViewModel.substringAfter("fun warmOtherProfiles(context: Context) {")
            .substringBefore("private suspend fun warmProfile(")
        for (forbidden in listOf("saveLastUsedProfile", "saveCurrentSession", "setActiveUser")) {
            assertTrue(
                "warming must not call $forbidden - it logs in accounts the user " +
                    "did not pick, and must never be able to make one current",
                !warm.contains(forbidden)
            )
        }
        assertTrue(
            "warming has to skip the profile that is already active",
            warm.contains("if (account.id == lastAccountId) continue")
        )
    }
}
