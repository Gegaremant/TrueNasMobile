package com.gegaremant.truenasmobile.ui.homepage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.homepage.details.LineChartView

/**
 * The load charts, on the Details tab itself.
 *
 * The owner found the Performance screen through search and asked for it back
 * in the interface - and for the graphs to be right here, on the main screen,
 * rather than one more tap away. So: a collapsed card that opens the three
 * graphs, plus a link to the full Performance screen.
 */
@Composable
fun LoadChartsCard(
    cpuData: List<System.ReportingGraphResponse>?,
    memoryData: List<System.ReportingGraphResponse>?,
    temperatureData: List<System.ReportingGraphResponse>?,
    onOpenPerformance: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val hasData = !cpuData.isNullOrEmpty() || !memoryData.isNullOrEmpty()
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(200),
        label = "chartsExpand"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = hasData) { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.details_charts_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onOpenPerformance != null) {
                TextButton(onClick = onOpenPerformance) {
                    // The row itself toggles; this button opens the full screen.
                    Text(
                        text = stringResource(R.string.details_charts_all),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            if (hasData) {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = stringResource(
                        if (expanded) {
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

        AnimatedVisibility(
            visible = expanded && hasData,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ChartEntry(
                    labelRes = R.string.details_metric_cpu,
                    data = cpuData?.firstOrNull(),
                    color = MaterialTheme.colorScheme.primary,
                    unit = "%"
                )
                ChartEntry(
                    labelRes = R.string.details_metric_memory,
                    data = memoryData?.firstOrNull(),
                    color = MaterialTheme.colorScheme.tertiary,
                    unit = "GB",
                    isMemory = true
                )
                ChartEntry(
                    labelRes = R.string.details_metric_temperature,
                    data = temperatureData?.firstOrNull(),
                    color = MaterialTheme.colorScheme.error,
                    unit = "°C"
                )
            }
        }

        if (!hasData) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.details_charts_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartEntry(
    labelRes: Int,
    data: System.ReportingGraphResponse?,
    color: Color,
    unit: String,
    isMemory: Boolean = false
) {
    if (data == null) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LineChartView(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            data = data,
            color = color,
            unit = unit,
            isMemory = isMemory
        )
    }
}