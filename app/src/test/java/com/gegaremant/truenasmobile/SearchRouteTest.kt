package com.gegaremant.truenasmobile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the search index against routes no NavHost can reach.
 *
 * The app runs two NavHosts: an inner one inside MainScreen that holds every
 * post-login tab, and the root one in MainActivity that holds the account
 * level screens (login, account switcher, change password, settings). They are
 * siblings, so neither graph sees the other's destinations.
 *
 * SearchViewModel indexes destinations by raw route string, and
 * SearchResultNavigation picks the controller at tap time. A route that is
 * registered in neither graph therefore only fails at runtime, as
 * "Navigation destination that cannot be found in the NavController's graph"
 * on the user's tap - which is exactly how it happened: two search entries
 * pointed at root-only routes while the navigation call went to the inner
 * controller.
 *
 * This test cannot see the graphs themselves (they are built in Compose), so
 * it reads the source instead - the same approach LocalizationTest uses for
 * the locale dictionaries.
 */
class SearchRouteTest {

    private val sourceDir = File("src/main/java/com/gegaremant/truenasmobile")

    private val searchViewModel = File(sourceDir, "ui/topbar/SearchViewModel.kt")
    private val rootNavHost = File(sourceDir, "MainActivity.kt")
    private val innerNavHost = File(sourceDir, "ui/MainScreen.kt")

    /** `Screen.Home.route` -> `Home` */
    private fun screenRefs(text: String): Set<String> =
        Regex("""Screen\.(\w+)\.route""").findAll(text).map { it.groupValues[1] }.toSet()

    /**
     * Routes passed to `composable(...)`. The route argument is always the
     * first thing in that call, so stopping at the first `)` is enough - the
     * nested `navArgument(...)` parens come later and cannot swallow it.
     */
    private fun registeredRoutes(file: File): Set<String> {
        val text = file.readText()
        val registered = mutableSetOf<String>()
        for (match in Regex("""composable\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL).findAll(text)) {
            registered += screenRefs(match.groupValues[1])
        }
        return registered
    }

    @Test
    fun `every route in the search registries is reachable in some navhost`() {
        val referenced = screenRefs(searchViewModel.readText())
        assertTrue(
            "no Screen.*.route references found in ${searchViewModel.name} - " +
                "the parsing below has stopped matching and would pass vacuously",
            referenced.isNotEmpty()
        )

        val registered = registeredRoutes(rootNavHost) + registeredRoutes(innerNavHost)
        assertTrue(
            "no composable(Screen.*.route) destinations found - parsing stopped matching",
            registered.isNotEmpty()
        )

        val unreachable = referenced - registered
        assertTrue(
            "search indexes screens that no NavHost registers, so tapping the " +
                "result throws instead of navigating: $unreachable",
            unreachable.isEmpty()
        )
    }

    @Test
    fun `account level screens stay in the root navhost`() {
        val inner = registeredRoutes(innerNavHost)
        val rootOnly = setOf("AccountSwitcher", "ChangePassword", "Login")
        assertTrue(
            "these belong to the root NavHost; SearchResultNavigation routes " +
                "them there, so a copy in the inner graph would mean two live " +
                "copies of the same screen: ${rootOnly.intersect(inner)}",
            rootOnly.intersect(inner).isEmpty()
        )
    }
}
