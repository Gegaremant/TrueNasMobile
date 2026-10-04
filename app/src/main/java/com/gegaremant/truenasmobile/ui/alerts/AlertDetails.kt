package com.gegaremant.truenasmobile.ui.alerts

import com.gegaremant.truenasmobile.data.models.System

/**
 * What an alert actually says.
 *
 * `formatted` is the template with its own placeholders filled - "Backup of
 * dataset X failed". The part that explains the failure lives in `args`, and it
 * was never shown: the notification said the backup failed and stopped there,
 * so "why" was unavailable. This renders the arguments as readable lines.
 */
object AlertDetails {

    /** One line per argument: `key: value`, one level deeper for nested maps. */
    fun lines(alert: System.AlertResponse): List<String> =
        flatten(alert.args, prefix = null)
            .filter { (_, value) -> value.isNotBlank() }
            .map { (key, value) -> "$key: $value" }

    /**
     * The body for a notification or a detail screen: the alert's own words,
     * then whatever the stand told us about it.
     */
    fun describe(alert: System.AlertResponse): String {
        val headline = alert.formatted?.takeIf { it.isNotBlank() }
            ?: alert.text.takeIf { it.isNotBlank() }
            ?: alert.klass
        val details = lines(alert)
        return if (details.isEmpty()) headline else headline + "\n" + details.joinToString("\n")
    }

    /** Class name turned into words: `BackupTaskFailed` -> `Backup task failed`. */
    fun readableClass(klass: String): String {
        if (klass.isBlank()) return ""
        val spaced = klass
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
        return spaced.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun flatten(value: Any?, prefix: String?): List<Pair<String, String>> = when (value) {
        // args arrive as Map/List/primitives from Moshi; render them as text.
        null -> emptyList()
        is Map<*, *> -> value.entries.flatMap { (key, entry) ->
            flatten(entry, prefix = if (prefix == null) key?.toString() else "$prefix.$key")
        }
        is Collection<*> -> value.flatMapIndexed { index, entry ->
            flatten(entry, prefix = prefix ?: "item")
                .map { it.first to it.second }
                .let { list ->
                    if (prefix == null) list.map { pair ->
                        pair.first to "${index + 1}) ${pair.second}"
                    } else list
                }
        }
        is Array<*> -> value.flatMapIndexed { index, entry ->
            flatten(entry, prefix = "$prefix[${index + 1}]")
        }
        else -> listOf((prefix ?: "detail") to value.toString())
    }
}