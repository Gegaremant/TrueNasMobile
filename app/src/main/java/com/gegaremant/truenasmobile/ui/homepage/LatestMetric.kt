package com.gegaremant.truenasmobile.ui.homepage

import com.gegaremant.truenasmobile.data.models.System

/**
 * Reads the newest value out of a `system.report` series.
 *
 * Every point is `[timestamp, value, ...]`, and the first column is a Unix
 * timestamp in **milliseconds**. Taking it as the value is how the Details
 * screen used to print `1753567890123%` and a temperature in the billions, so
 * every reader of these series goes through this function instead of indexing
 * the point itself.
 *
 * @param averageAcrossCores CPU arrives as one column per core, so the load of
 *   the whole system is their average. Memory and temperature report a single
 *   column after the timestamp.
 * @return the newest value, or `null` when the series has no usable point.
 */
internal fun System.ReportingGraphResponse?.latestValue(
    averageAcrossCores: Boolean = false
): Double? {
    val point = this?.data?.lastOrNull() ?: return null
    return if (averageAcrossCores) {
        point.drop(1).takeIf { it.isNotEmpty() }?.average()
    } else {
        point.getOrNull(1)
    }
}