package com.gegaremant.truenasmobile

import com.gegaremant.truenasmobile.data.models.Apps
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

    private companion object {
        /** Any window the screens use; the point is that the flag tracks writes. */
        const val FRESH_WINDOW_MILLIS = 60_000L
    }

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
    fun `a write makes an entry fresh and a clear makes it stale again`() {
        assertTrue(
            "an entry nobody wrote must not be fresh, or a cold start would " +
                "skip the network and show nothing",
            !AppCache.isFresh(AppCache.Entry.APPS, FRESH_WINDOW_MILLIS)
        )

        AppCache.updateApps(
            listOf(Apps.AppQueryResponse(name = "plex", id = "abc", state = "RUNNING"))
        )
        assertTrue(
            "a write has to make the entry fresh, or every screen opening would " +
                "re-fetch a list the poller just refreshed",
            AppCache.isFresh(AppCache.Entry.APPS, FRESH_WINDOW_MILLIS)
        )

        AppCache.clearAllCache()

        assertTrue(
            "a clear has to make the entry stale, or the next account would open " +
                "on the previous server's rows",
            !AppCache.isFresh(AppCache.Entry.APPS, FRESH_WINDOW_MILLIS)
        )
    }

    @Test
    fun `the navigation holders are dropped with the session too`() {
        val holders = listOf(
            "ui/services/apps/details/appdetails/AppDataHolder.kt",
            "ui/homepage/pools/PoolDataHolder.kt",
            "ui/services/containers/details/ContainerDataHolder.kt",
            "ui/services/vm/details/VmDataHolder.kt"
        )
        for (path in holders) {
            val text = File("src/main/java/com/gegaremant/truenasmobile/$path").readText()
            assertTrue(
                "$path has no clear(), so a pool, app, container or VM picked on " +
                    "one TrueNAS stays in a global after switching to another",
                text.contains("fun clear()")
            )
        }

        val aggregator = File("src/main/java/com/gegaremant/truenasmobile/ui/utils/NavigationHolders.kt")
        assertTrue("NavigationHolders is missing", aggregator.exists())

        for (path in listOf("ui/login/LoginScreenViewModel.kt", "ui/settings/SettingsScreenViewModel.kt")) {
            val text = File("src/main/java/com/gegaremant/truenasmobile/$path").readText()
            val clears = text.split("NavigationHolders.clearAll()").size - 1
            assertTrue(
                "$path clears the cache but not the navigation holders, so the " +
                    "next account can still be handed the previous one's data " +
                    "(found $clears call sites)",
                clears >= 1
            )
        }
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
