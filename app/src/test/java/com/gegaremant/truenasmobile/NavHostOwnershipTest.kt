package com.gegaremant.truenasmobile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards NavHost ownership.
 *
 * The app runs two NavHosts that are **siblings**, not parent and child:
 *
 *  - the root one in `MainActivity`, holding the pre-login and account level
 *    screens (login, account switcher, settings, change password, `main`);
 *  - the inner one inside `MainScreen`, holding the tabs and their detail
 *    screens, starting at `home`.
 *
 * Neither graph can see the other's destinations, so asking the wrong one for a
 * back stack entry is a runtime failure and not a compile error. That is exactly
 * how the shared dashboard ViewModel took the app down: it asked the inner
 * controller for the `main` entry, which only the root graph has, and
 * `getBackStackEntry` threw during composition - silently, because the app had
 * not drawn its first frame yet. Every build, test and APK check passed anyway;
 * nothing in this repo runs Compose navigation.
 *
 * A source-level check is the only guard available offline, and the same
 * technique the other invariants use.
 */
class NavHostOwnershipTest {

    private val pkgDir = File("src/main/java/com/gegaremant/truenasmobile")

    /** Screen object names registered as destinations in a composable() call. */
    private fun registeredScreens(file: File): Set<String> {
        val text = file.readText()
        val names = mutableSetOf<String>()
        for (match in Regex("""composable\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL).findAll(text)) {
            names += Regex("""Screen\.(\w+)\.route""").findAll(match.groupValues[1])
                .map { it.groupValues[1] }
        }
        return names
    }

    private val rootScreens = registeredScreens(File(pkgDir, "MainActivity.kt"))
    private val innerScreens = registeredScreens(File(pkgDir, "ui/MainScreen.kt"))

    @Test
    fun `the two graphs are non-empty and disjoint where it matters`() {
        assertTrue("no destinations parsed from the root NavHost", rootScreens.isNotEmpty())
        assertTrue("no destinations parsed from the inner NavHost", innerScreens.isNotEmpty())
        assertTrue(
            "the root NavHost is expected to own the `main` destination, it is " +
                "where MainScreen is hosted",
            rootScreens.contains("Main")
        )
        assertTrue(
            "the inner NavHost is expected to start at `home`",
            innerScreens.contains("Home")
        )
    }

    @Test
    fun `no back stack entry is requested from a graph that does not own it`() {
        val offenders = mutableListOf<String>()

        pkgDir.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                val call = Regex("""(\w+)\.getBackStackEntry\(\s*Screen\.(\w+)\.route""")
                for (match in call.findAll(file.readText())) {
                    val (receiver, screen) = match.groupValues[1] to match.groupValues[2]
                    // A receiver whose name mentions "root" is the root graph's.
                    val owned = if (receiver.contains("root", ignoreCase = true)) {
                        rootScreens
                    } else {
                        innerScreens
                    }
                    if (!owned.contains(screen)) {
                        offenders += "${file.name}: ${receiver}.getBackStackEntry(Screen.$screen.route) " +
                            "but that graph does not register `$screen`"
                    }
                }
            }

        assertTrue(
            "these look up a back stack entry on a NavHost that does not " +
                "register the destination, which throws during composition and " +
                "takes the app down with no error shown:\n" + offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }
}
