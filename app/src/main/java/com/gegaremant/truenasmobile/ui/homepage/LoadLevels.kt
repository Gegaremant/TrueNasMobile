package com.gegaremant.truenasmobile.ui.homepage

import androidx.compose.ui.graphics.Color

/**
 * Whether a reading is fine, worth watching, or bad.
 *
 * The charts used to take their colour from the theme, which says nothing about
 * the number it draws: a red line could be a cool machine and a blue one a
 * boiling one. The owner asked for the colour to answer "is this normal", so
 * every live metric picks its colour from its own thresholds.
 */
enum class LoadSeverity { NORMAL, ELEVATED, HIGH }

/** Thresholds per metric: above [elevated] is worth a look, above [critical] is bad. */
data class LoadThresholds(val elevated: Double, val critical: Double)

object LoadLevels {
    /** A share of the cores busy. */
    val cpu = LoadThresholds(elevated = 60.0, critical = 85.0)

    /** A share of RAM in use. Memory runs hotter than CPU by nature. */
    val memory = LoadThresholds(elevated = 75.0, critical = 90.0)

    /** CPU package temperature in Celsius. */
    val cpuTemperature = LoadThresholds(elevated = 70.0, critical = 85.0)

    /** Drive temperature: drives are meant to run cool. */
    val diskTemperature = LoadThresholds(elevated = 45.0, critical = 55.0)

    /**
     * [unit] and [thresholds] mirror the series being drawn: a graph of memory
     * in GB must not be judged against a percentage.
     */
    fun of(value: Double?, unit: String, thresholds: LoadThresholds): LoadSeverity {
        if (value == null) return LoadSeverity.NORMAL
        return when {
            value >= thresholds.critical -> LoadSeverity.HIGH
            value >= thresholds.elevated -> LoadSeverity.ELEVATED
            else -> LoadSeverity.NORMAL
        }
    }

    /**
     * Blue when it is normal, amber when it deserves attention, red when it is
     * bad - and the theme's accent only for a metric we have no scale for.
     */
    fun colorOf(
        value: Double?,
        unit: String,
        thresholds: LoadThresholds,
        normal: Color,
        elevated: Color = Color(0xFFF9A825),
        high: Color = Color(0xFFD81B60)
    ): Color = when (of(value, unit, thresholds)) {
        LoadSeverity.NORMAL -> normal
        LoadSeverity.ELEVATED -> elevated
        LoadSeverity.HIGH -> high
    }
}