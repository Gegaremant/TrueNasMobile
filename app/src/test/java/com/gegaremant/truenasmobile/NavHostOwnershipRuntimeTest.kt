package com.gegaremant.truenasmobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gegaremant.truenasmobile.ui.Screen
import com.gegaremant.truenasmobile.ui.dashboardViewModelOwner
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Walks the real navigation, in a real composition, on the JVM.
 *
 * `NavHostOwnershipTest` reads the sources and reasons about which graph
 * registers which destination. That is a fine cheap guard, but it cannot prove
 * the behaviour: what it protects against is a `getBackStackEntry` on a graph
 * that does not register the route, and that throws only when the lookup
 * actually runs.
 *
 * So the graphs are really composed here - real `NavHost`s, real
 * `NavHostController`s, the real `composable` DSL - and the real lookup
 * function is really called. Point the dashboard at the inner controller again
 * and this fails by throwing, which is exactly what a device did: the app closed
 * silently after login, and no amount of compiling, linting or unit testing
 * noticed.
 *
 * Hand-building the graphs with the plain `destination` DSL was tried first and
 * is a dead end: `composable` destinations need Compose's navigator, which only
 * the `NavHost` composable registers.
 *
 * Division of labour with `NavHostOwnershipTest`, established by reintroducing
 * the bug and watching which test noticed: this file guards the *behaviour* (the
 * lookup succeeds on the graph that registers `main`, and throws on the one that
 * does not), and the source test guards the *call site*, since this file passes
 * the controller in itself and therefore cannot see which one production hands
 * over. Both are needed: this one cannot catch a swapped argument, that one
 * cannot catch a change in what the API does.
 */
@RunWith(RobolectricTestRunner::class)
// The base Application on purpose. The app's own schedules a WorkManager job from
// onCreate, and WorkManager refuses to initialise unless the Application is a
// Configuration.Provider - a production concern no navigation test should
// inherit. See docs/TESTING.md.
@Config(sdk = [34], application = android.app.Application::class)
class NavHostOwnershipRuntimeTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var root: NavHostController
    private lateinit var inner: NavHostController

    /** Mirrors the real structure: account-level screens in one graph, tabs in another. */
    private fun setUpTwoGraphs() {
        compose.setContent {
            root = rememberNavController()
            inner = rememberNavController()

            NavHost(navController = root, startDestination = Screen.Login.route) {
                composable(Screen.Login.route) { }
                composable(Screen.AccountSwitcher.route) { }
                composable(Screen.Settings.route) { }
                composable(Screen.ChangePassword.route) { }
                composable(Screen.Main.route) {
                    androidx.compose.material3.Text("main")
                }
            }

            NavHost(navController = inner, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) { }
                composable(Screen.Storage.route) { }
                composable(Screen.Performance.route) { }
                composable(Screen.Apps.route) { }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the dashboard owner resolves once the app has navigated to main`() {
        setUpTwoGraphs()
        // What the app does after a successful login.
        compose.runOnIdle { root.navigate(Screen.Main.route) }
        compose.waitForIdle()

        val entry = dashboardViewModelOwner(root)
        assertNotNull("the owner must be the `main` entry", entry)
        assertTrue(
            "expected the `main` entry, got ${entry.destination.route}",
            entry.destination.route == Screen.Main.route
        )
    }

    @Test
    fun `resolving the same owner on the inner graph fails, as it must`() {
        setUpTwoGraphs()
        compose.runOnIdle { inner.navigate(Screen.Storage.route) }
        compose.waitForIdle()

        val failure = runCatching { dashboardViewModelOwner(inner) }.exceptionOrNull()
        assertTrue(
            "expected the inner graph to reject the `main` entry, got: $failure. " +
                "If this ever stops throwing, the negative case has stopped " +
                "proving the test is sensitive.",
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `the graphs keep their destinations apart`() {
        setUpTwoGraphs()
        compose.runOnIdle { root.navigate(Screen.Main.route) }
        compose.waitForIdle()

        assertNotNull("root graph lost `main`", root.graph.findNode(Screen.Main.route))
        assertNotNull("root graph lost `login`", root.graph.findNode(Screen.Login.route))
        assertNotNull("inner graph lost `home`", inner.graph.findNode(Screen.Home.route))
        assertTrue(
            "the inner graph must not know the account-level destinations",
            inner.graph.findNode(Screen.Main.route) == null &&
                inner.graph.findNode(Screen.Login.route) == null
        )
    }

    @Test
    fun `a real NavHost composes and renders its destination`() {
        // Proves the harness itself is honest: if composition were broken here,
        // the three tests above would pass for the wrong reason.
        compose.setContent {
            NavHost(navController = rememberNavController(), startDestination = Screen.Login.route) {
                composable(Screen.Login.route) {
                    androidx.compose.material3.Text("sign in screen")
                }
            }
        }
        compose.onNodeWithText("sign in screen").assertExists()
        assertTrue("the node should exist", compose.onAllNodesWithText("sign in screen").fetchSemanticsNodes().isNotEmpty())
    }
}
