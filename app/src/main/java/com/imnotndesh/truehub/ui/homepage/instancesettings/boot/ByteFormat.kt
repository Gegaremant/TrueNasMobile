package com.imnotndesh.truehub.ui.homepage.instancesettings.boot

import java.util.Locale

/**
 * Formats a byte count into a human-readable string (e.g. "156.3 GB").
 *
 * Handles null gracefully, returning "—".
 */
internal fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "—"
    if (bytes < 1024) return "$bytes B"

    val units = arrayOf("KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
}
