package com.gegaremant.truenasmobile.ui.homepage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.homepage.details.LineChartView
import java.util.Locale

/**
 * The three live metrics of the Details screen: CPU, memory, temperature.
 *
 * Each is a collapsed row showing its current value; tapping expands that row
 * into its own chart. Only one is open at a time on purpose - stacking three
 * charts turns the screen into a wall of graphs, and the point of the row is to
 * answer "how is it doing right now" first, "why" second.
 *
 * The health banner and the load-percentage block that used to sit at the top of
 * the Graphs screen are deliberately not reproduced here: the owner asked for
 * the rows without that top section.
 */
@Composable
fun DetailsMetricsSection(
    cpuData: List<System.ReportingGraphResponse>?,
    memoryData: List<System.ReportingGraphResponse>?,
    temperatureData: List<System.ReportingGraphResponse>?,
    modifier: Modifier = Modifier,
) {
    // Remembered by name so rotation keeps the open row open.
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val metrics = listOf(
        MetricRow(
            key = "cpu",
            labelRes = R.string.details_metric_cpu,
            icon = Icons.Filled.Speed,
            normalColor = MaterialTheme.colorScheme.primary,
            data = cpuData?.firstOrNull(),
            unit = "%",
            averageAcrossCores = true,
            thresholds = LoadLevels.cpu
        ),
        MetricRow(
            key = "memory",
            labelRes = R.string.details_metric_memory,
            icon = Icons.Filled.Memory,
            normalColor = MaterialTheme.colorScheme.tertiary,
            data = memoryData?.firstOrNull(),
            unit = "GB",
            // Memory is reported in bytes; the charts label it as GB.
            scaleToGigabytes = true,
            thresholds = LoadLevels.memory
        ),
        MetricRow(
            key = "temperature",
            labelRes = R.string.details_metric_temperature,
            icon = Icons.Filled.DeviceThermostat,
            normalColor = MaterialTheme.colorScheme.primary,
            data = temperatureData?.firstOrNull(),
            unit = "°C",
            thresholds = LoadLevels.cpuTemperature
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.details_metrics_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        metrics.forEach { metric ->
            val isExpanded = expandedKey == metric.key
            val hasData = !metric.data?.data.isNullOrEmpty()
            val metricColor = metric.severityColor()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasData) { expandedKey = if (isExpanded) null else metric.key }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = metric.icon,
                        contentDescription = null,
                        tint = metricColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(metric.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = metric.currentValueLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = metricColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (hasData) {
                        Icon(
                            imageVector = if (isExpanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = stringResource(
                                if (isExpanded) {
                                    R.string.details_metrics_collapse_cd
                                } else {
                                    R.string.details_metrics_expand_cd
                                }
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded && hasData) {
                    metric.data?.let { graph ->
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                            LineChartView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp),
                                data = graph,
                                color = metricColor,
                                unit = metric.unit,
                                isMemory = metric.scaleToGigabytes
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MetricRow(
    val key: String,
    @androidx.annotation.StringRes val labelRes: Int,
    val icon: ImageVector,
    val normalColor: Color,
    val data: System.ReportingGraphResponse?,
    val unit: String,
    val scaleToGigabytes: Boolean = false,
    val thresholds: LoadThresholds? = null,
    /** CPU arrives as one column per core, so the row shows the whole-SoC load. */
    val averageAcrossCores: Boolean = false
) {
    /**
     * Last point of the series, formatted, or a dash when there is no data yet.
     *
     * The reading itself lives in [latestValue] because every consumer of a
     * `system.report` series has to skip the timestamp column.
     */
    /** Newest reading in display units - what the severity is judged on. */
    fun currentValue(): Double? {
        val raw = data.latestValue(averageAcrossCores) ?: return null
        return if (scaleToGigabytes) raw / (1024.0 * 1024.0 * 1024.0) else raw
    }

    /**
     * Blue while the reading is normal, amber when it deserves a look, red when
     * it is bad. A metric without a scale keeps the theme colour.
     */
    @Composable
    fun severityColor(): Color {
        val scale = thresholds ?: return normalColor
        return LoadLevels.colorOf(currentValue(), unit, scale, normalColor)
    }

    fun currentValueLabel(): String {
        val raw = data.latestValue(averageAcrossCores) ?: return "—"
        val value = when {
            scaleToGigabytes -> raw / (1024.0 * 1024.0 * 1024.0)
            // Percentages past 100 are not a load, and a core average cannot go
            // below zero; either one means the series is not what we think.
            unit == "%" -> raw.coerceIn(0.0, 100.0)
            else -> raw
        }
        return if (scaleToGigabytes) {
            String.format(Locale.US, "%.1f %s", value, unit)
        } else if (unit == "%") {
            String.format(Locale.US, "%.0f%s", value, unit)
        } else {
            String.format(Locale.US, "%.0f%s", value, unit)
        }
    }
}