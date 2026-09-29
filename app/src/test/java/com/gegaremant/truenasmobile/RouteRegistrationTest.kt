package com.gegaremant.truenasmobile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every route the app navigates to must actually exist as a destination.
 *
 * The app has two NavHost graphs, declared in `MainActivity` (account level) and
 * inside `MainScreen` (the tabs). A `navigate` to a route neither of them
 * registers throws `IllegalArgumentException: Navigation destination that
 * matches route X cannot be found` at the moment the user taps - the same shape
 * of bug as the search results pointing at the wrong graph, which crashed on
 * tap and could not be caught by compiling or linting.
 *
 * This is the whole-app version of that check, plus a report of `Screen` objects
 * that are neither registered nor used as a route prefix. One of those exists on
 * purpose: `Screen.Files` is a route *prefix* for the dataset explorer, not a
 * destination of its own (see TODO 27), so it is listed rather than failed.
 */
class RouteRegistrationTest {

    private val sourceDir = File("src/main/java/com/gegaremant/truenasmobile")
    private val mainActivity = File(sourceDir, "MainActivity.kt")
    private val mainScreen = File(sourceDir, "ui/MainScreen.kt")
    private val screenCatalogue = File(sourceDir, "ui/Screen.kt")

    private fun destinationsIn(file: File): Set<String> {
        val names = mutableSetOf<String>()
        for (match in Regex("""composable\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
            .findAll(file.readText())) {
            names += Regex("""Screen\.(\w+)\.route""").findAll(match.groupValues[1])
                .map { it.groupValues[1] }
        }
        return names
    }

    private fun allKotlinFiles(): List<File> =
        sourceDir.walkTopDown().filter { it.extension == "kt" }.toList()

    @Test
    fun `every navigate target is a registered destination`() {
        val registered = destinationsIn(mainActivity) + destinationsIn(mainScreen)
        assertTrue("no destinations parsed from either NavHost", registered.isNotEmpty())

        val targets = mutableSetOf<String>()
        allKotlinFiles().forEach { file ->
            for (call in Regex("""\.navigate\(([^;]{0,160})""").findAll(file.readText())) {
                targets += Regex("""Screen\.(\w+)\.route""").findAll(call.groupValues[1])
                    .map { it.groupValues[1] }
            }
        }
        assertTrue("no navigate() targets parsed - the pattern has drifted", targets.isNotEmpty())

        val missing = sortedSetOf<String>()
        targets.forEach { screen ->
            if (screen !in registered) {
                val where = allKotlinFiles().filter { it.readText().contains("Screen.$screen.route") }
                    .map { it.name }
                    .distinct()
                missing += "Screen.$screen (referenced from ${where.joinToString(", ")})"
            }
        }

        assertTrue(
            "the app navigates to routes no NavHost registers, which throws on " +
                "the user's tap:\n" + missing.joinToString("\n"),
            missing.isEmpty()
        )
    }

    @Test
    fun `every Screen object is either a destination or a route prefix`() {
        val registered = destinationsIn(mainActivity) + destinationsIn(mainScreen)
        val catalogue = screenCatalogue.readText()
        val objects = Regex("""object\s+(\w+)\s*:""").findAll(catalogue)
            .map { it.groupValues[1] }
            .toSet()

        // A Screen object may be a prefix of another route rather than a
        // destination itself; those are legitimate and must not be reported.
        val prefixObjects = Regex(""""\$\{(Files|\w+)\.route\}""").findAll(catalogue)
            .map { it.groupValues[1] }
            .toSet()

        val orphans = (objects - registered - prefixObjects).sorted()
        assertTrue(
            "these Screen objects are neither a registered destination nor a " +
                "route prefix, so nothing can ever open them:\n" + orphans.joinToString("\n"),
            orphans.isEmpty()
        )
    }
}
