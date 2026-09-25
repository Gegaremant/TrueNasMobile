package com.gegaremant.truenasmobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

/**
 * Guards the localisation contract of the app.
 *
 * The app ships two locale dictionaries - `res/values/strings.xml` (English,
 * the base) and `res/values/values-ru/strings.xml` (Russian). A bug that
 * shipped once already: strings were hardcoded in Kotlin, so a Russian
 * device saw a half-translated UI. These tests make that failure mode
 * impossible to reintroduce silently.
 */
class LocalizationTest {

    private val resDir = File("src/main/res")
    private val enFile = File(resDir, "values/strings.xml")
    private val ruFile = File(resDir, "values-ru/strings.xml")
    private val sourceDir = File("src/main/java")

    private data class Entry(
        val name: String,
        val items: List<Pair<String, String>>,
        val isPlural: Boolean
    )

    private fun parse(file: File): List<Entry> {
        val text = file.readText()
        val entries = mutableListOf<Entry>()

        val plural = Pattern.compile(
            """<plurals\s+name="([^"]+)"\s*>(.*?)</plurals>""",
            Pattern.DOTALL
        ).matcher(text)
        while (plural.find()) {
            val name = plural.group(1)
            val inner = plural.group(2)
            // A plurals entry is a set of quantity -> text pairs. Keeping them
            // separate (rather than one flattened blob) is what lets the
            // quantity and placeholder checks below be exact.
            val items = mutableListOf<Pair<String, String>>()
            val quantities = Pattern.compile("""<item\s+quantity="([^"]+)"\s*>(.*?)</item>""", Pattern.DOTALL)
                .matcher(inner)
            while (quantities.find()) {
                items += quantities.group(1) to quantities.group(2)
            }
            entries += Entry(name, items, isPlural = true)
        }

        val withoutPlurals = text.replace(
            Regex("""<plurals.*?</plurals>""", RegexOption.DOT_MATCHES_ALL), ""
        )
        val string = Pattern.compile(
            """<string\s+name="([^"]+)"\s*>(.*?)</string>""",
            Pattern.DOTALL
        ).matcher(withoutPlurals)
        while (string.find()) {
            entries += Entry(string.group(1), listOf("" to string.group(2)), isPlural = false)
        }
        return entries
    }

    private fun placeholders(body: String): Set<String> {
        val out = mutableSetOf<String>()
        val p = Pattern.compile("""%(\d+\$[a-zA-Z])""").matcher(body)
        while (p.find()) out += p.group(1)
        return out
    }

    @Test
    fun `both dictionaries exist`() {
        assertTrue("missing ${enFile.path}", enFile.isFile)
        assertTrue("missing ${ruFile.path}", ruFile.isFile)
    }

    @Test
    fun `no duplicate keys in either dictionary`() {
        listOf(enFile, ruFile).forEach { file ->
            val dupes = parse(file)
                .groupBy { it.name }
                .filterValues { it.size > 1 }
                .keys
            assertTrue(
                "duplicate string keys in ${file.path}: ${dupes.joinToString()}",
                dupes.isEmpty()
            )
        }
    }

    @Test
    fun `russian dictionary covers every english key`() {
        val en = parse(enFile).associateBy { it.name }
        val ru = parse(ruFile).associateBy { it.name }

        val missing = en.keys - ru.keys
        val extra = ru.keys - en.keys

        assertTrue("keys missing a RU translation: ${missing.sorted()}", missing.isEmpty())
        assertTrue("RU keys with no EN base: ${extra.sorted()}", extra.isEmpty())
    }

    @Test
    fun `plural kinds are valid for each locale`() {
        val en = parse(enFile).filter { it.isPlural }.associateBy { it.name }
        val ru = parse(ruFile).filter { it.isPlural }.associateBy { it.name }

        en.forEach { (name, entry) ->
            val russian = ru[name] ?: return@forEach

            val enKinds = entry.items.map { it.first }.filter { it.isNotBlank() }
            val ruKinds = russian.items.map { it.first }.filter { it.isNotBlank() }

            // English needs only `other`; Russian adds `one`/`few`/`many`.
            // What must hold is that every quantity used is legal for its own
            // locale and that both sides keep `other` as the fallback.
            val enLegal = setOf("zero", "one", "two", "few", "many", "other")
            val ruLegal = setOf("one", "few", "many", "other")

            assertTrue(
                "plurals '$name' uses quantities English does not support: $enKinds",
                enKinds.all { it in enLegal }
            )
            assertTrue(
                "RU plurals '$name' uses quantities Android does not support: $ruKinds",
                ruKinds.all { it in ruLegal }
            )
            assertTrue("plurals '$name' has no 'other' quantity in EN", "other" in enKinds)
            assertTrue("RU plurals '$name' has no 'other' fallback", "other" in ruKinds)
        }
    }

    @Test
    fun `format placeholders match between locales`() {
        val en = parse(enFile).associateBy { it.name }
        val ru = parse(ruFile).associateBy { it.name }

        en.forEach { (name, entry) ->
            val russian = ru[name] ?: return@forEach
            val enArgs = entry.items.flatMap { placeholders(it.second) }.toSet()
            val ruArgs = russian.items.flatMap { placeholders(it.second) }.toSet()
            if (enArgs.isEmpty() && ruArgs.isEmpty()) return@forEach
            assertEquals(
                "format placeholders differ for '$name' (EN $enArgs vs RU $ruArgs)",
                enArgs,
                ruArgs
            )
        }
    }

    @Test
    fun `no untranslated Russian text hardcoded in Kotlin sources`() {
        val cyrillic = Pattern.compile("\"[^\"\\n]*[А-Яа-яЁё][^\"\\n]*\"")
        val offenders = mutableListOf<String>()

        sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                        return@forEachLine
                    }
                    val match = cyrillic.matcher(line)
                    while (match.find()) {
                        offenders += "${file.path}: ${match.group()}"
                    }
                }
            }

        assertTrue(
            "hardcoded Cyrillic string literals in Kotlin - move them into the " +
                "locale dictionaries (res/values/strings.xml + res/values-ru/strings.xml):\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }
}
