package com.gegaremant.truenasmobile

import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.utils.AppCache
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the AppCache lifecycle.
 *
 * `AppCache` is a process-wide object that is only ever written to and cleared,
 * never rebuilt. That makes two failure modes possible, and both shipped:
 *
 *  - Nothing called `clearAllCache()`. Signing out of one TrueNAS and into
 *    another left the previous server's pools, apps, shares, services and
 *    system info in memory for the dashboard and the search overlay to render.
 *  - `clearAllCache()` itself skipped the marketplace catalogue and the service
 *    list, so even when it did run, stale entries survived it.
 *
 * The second one is easy to reintroduce by adding a field and forgetting the
 * reset, which is why the completeness check reads the source.
 */
class AppCacheLifecycleTest {

    private val cacheFile = File("src/main/java/com/gegaremant/truenasmobile/ui/utils/Appcache.kt")

    @Test
    fun `clearAllCache resets every cache flow`() {
        // Pool has an all-default constructor; the service row needs five
        // scalars. Enough to prove the reset reaches the list flows and the
        // nullable one.
        AppCache.updatePools(listOf(System.Pool(name = "tank")))
        AppCache.updateServices(
            listOf(System.ServiceQueryResponse(1, "sshd", enable = true, state = "RUNNING", pids = listOf(7)))
        )

        AppCache.clearAllCache()

        assertTrue("pools survived clearAllCache", AppCache.cachedPools.value.isEmpty())
        assertTrue("services survived clearAllCache", AppCache.cachedServices.value.isEmpty())
    }

    @Test
    fun `every declared cache flow is reset inside clearAllCache`() {
        val text = cacheFile.readText()

        val declared = Regex("""private\s+val\s+_(\w+)\s*=\s*MutableStateFlow""").findAll(text)
            .map { it.groupValues[1] }
            .toList()
        assertTrue(
            "no MutableStateFlow declarations matched in ${cacheFile.name} - the " +
                "pattern has drifted and this test would pass vacuously",
            declared.isNotEmpty()
        )

        val body = text.substringAfter("fun clearAllCache(")
        val missing = declared.filterNot { body.contains("_$it.value") }
        assertTrue(
            "these cache flows are never reset by clearAllCache, so they would " +
                "leak into the next account's session: $missing",
            missing.isEmpty()
        )
    }

    @Test
    fun `signing out and signing in both clear the cache`() {
        // The cache outlives the session, so every path that ends or starts one
        // has to drop it. Both live in files the test can read; a JVM test cannot
        // drive the Compose login flow itself.
        val login = File("src/main/java/com/gegaremant/truenasmobile/ui/login/LoginScreenViewModel.kt")
            .readText()
        val settings = File("src/main/java/com/gegaremant/truenasmobile/ui/settings/SettingsScreenViewModel.kt")
            .readText()

        assertTrue(
            "no successful login clears AppCache, so switching accounts keeps the " +
                "previous server's data on screen",
            login.contains("AppCache.clearAllCache()")
        )
        assertTrue(
            "signing out must clear AppCache as well - the login-side clear only " +
                "runs once the next account is already on its way in",
            settings.contains("AppCache.clearAllCache()")
        )
    }
}
