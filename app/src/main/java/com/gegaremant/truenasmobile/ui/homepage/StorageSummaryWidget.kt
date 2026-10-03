package com.gegaremant.truenasmobile.ui.homepage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.models.System
import java.text.DecimalFormat

/**
 * The widget at the very top of the Details tab: the numbers, and nothing else.
 *
 * The owner asked for it in figures rather than as a card with prose: how many
 * disks of which type and at what RAID level, how much there is in total, how
 * much is used and how much is free, plus the live CPU / memory / temperature
 * load. The detailed cards stay below it - this one answers "what is it right
 * now" at a glance.
 */
@Composable
fun StorageSummaryWidget(
    pools: List<System.Pool>,
    disks: List<System.DiskDetails>,
    cpuData: List<System.ReportingGraphResponse>?,
    memoryData: List<System.ReportingGraphResponse>?,
    temperatureData: List<System.ReportingGraphResponse>?,
    /** [System.SystemInfo.physmem] - the RAM to turn used bytes into a share. */
    physicalMemoryBytes: Long,
    modifier: Modifier = Modifier
) {
    val totalSize = pools.sumOf { it.size ?: 0L }
    val allocated = pools.sumOf { it.allocated ?: 0L }
    val free = pools.sumOf { it.free ?: 0L }
    val usedFraction =
        if (totalSize > 0) (allocated.toDouble() / totalSize).coerceIn(0.0, 1.0) else null

    val hddCount = disks.count { it.type.equals("HDD", ignoreCase = true) }
    val ssdCount = disks.count { it.type.equals("SSD", ignoreCase = true) }
    val memoryUsedBytes = memoryData?.firstOrNull().latestValue()
    // The RAID level is not on the pool itself: the top-level topology device
    // is the pool, and its children are the vdevs, whose own type is the level.
    val raidLevel = pools.asSequence()
        .mapNotNull { it.topology?.data?.firstOrNull() }
        .mapNotNull { it.children.firstOrNull()?.type }
        .firstOrNull { it.isNotBlank() }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when {
                        disks.isEmpty() -> stringResource(R.string.summary_no_disks)
                        hddCount == 0 && ssdCount > 0 -> stringResource(R.string.summary_disks_ssd, ssdCount)
                        ssdCount == 0 -> stringResource(R.string.summary_disks_hdd, hddCount)
                        else -> stringResource(R.string.summary_disks_mixed, hddCount, ssdCount)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (raidLevel != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = raidLevel.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            if (totalSize > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryFigure(
                        label = stringResource(R.string.summary_total),
                        value = formatBytesShort(totalSize)
                    )
                    SummaryFigure(
                        label = stringResource(R.string.summary_used),
                        value = formatBytesShort(allocated)
                    )
                    SummaryFigure(
                        label = stringResource(R.string.summary_free),
                        value = formatBytesShort(free)
                    )
                }

                usedFraction?.let { fraction ->
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { fraction.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = when {
                            fraction > 0.85f -> MaterialTheme.colorScheme.error
                            fraction > 0.65f -> Color(0xFFF57C00)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LoadPill(
                    icon = Icons.Default.Speed,
                    label = stringResource(R.string.details_metric_cpu),
                    value = cpuData?.firstOrNull().latestValue(averageAcrossCores = true),
                    unit = "%",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                LoadPill(
                    icon = Icons.Default.Memory,
                    label = stringResource(R.string.summary_memory_used),
                    value = memoryUsedBytes?.let {
                        if (physicalMemoryBytes > 0) it / physicalMemoryBytes * 100.0 else null
                    },
                    unit = "%",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                LoadPill(
                    icon = Icons.Default.DeviceThermostat,
                    label = stringResource(R.string.details_metric_temperature),
                    value = temperatureData?.firstOrNull().latestValue(),
                    unit = "°C",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/**
 * One live number in a pill. [value] is already in the unit the pill shows:
 * percentages for CPU and memory, degrees for temperature.
 */
@Composable
private fun LoadPill(
    icon: ImageVector,
    label: String,
    value: Double?,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (value == null) {
                "—"
            } else {
                String.format(java.util.Locale.US, "%.0f%s", value.coerceAtLeast(0.0), unit)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (value == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                color
            }
        )
    }
}

/** "1,02 TB" - one decimal, the same shape the pool card uses. */
private fun formatBytesShort(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024.0
        unitIndex++
    }
    return "${DecimalFormat("#.#").format(size)} ${units[unitIndex]}"
}