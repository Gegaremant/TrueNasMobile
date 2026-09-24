package com.gegaremant.truenasmobile.ui.alerts

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsBottomSheet(
    uiState: AlertsUiState,
    onDismiss: () -> Unit,
    onDismissAlert: (String) -> Unit,
    onRestoreAlert: (String) -> Unit,
    onRefresh: () -> Unit,
    onClearError: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(AlertFilter.ALL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
        ,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp, top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.alerts_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.unreadCount > 0) {
                        Text(
                            text = stringResource(R.string.alerts_unread, uiState.unreadCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.common_refresh_cd),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlertFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = stringResource(filter.labelRes),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = if (selectedFilter == filter) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            // Error Message
            if (uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearError) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.common_dismiss),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingScreen(stringResource(R.string.alerts_loading))
                    }
                }
                else -> {
                    val filteredAlerts = when (selectedFilter) {
                        AlertFilter.ALL -> uiState.alerts
                        AlertFilter.UNREAD -> uiState.alerts.filter { !it.dismissed }
                        AlertFilter.NOTICE -> uiState.alerts.filter {
                            it.level.equals("NOTICE", ignoreCase = true) ||
                                    it.level.equals("ALERT", ignoreCase = true)
                        }
                        AlertFilter.INFO -> uiState.alerts.filter {
                            it.level.equals("INFO", ignoreCase = true)
                        }
                    }

                    if (filteredAlerts.isEmpty()) {
                        EmptyAlertsContent(filter = selectedFilter)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 24.dp
                            )
                        ) {
                            items(filteredAlerts, key = { it.uuid }) { alert ->
                                AlertItem(
                                    alert = alert,
                                    onDismiss = { onDismissAlert(alert.uuid) },
                                    onRestore = { onRestoreAlert(alert.uuid) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAlertsContent(filter: AlertFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (filter) {
                AlertFilter.ALL -> stringResource(R.string.alerts_empty_all)
                AlertFilter.UNREAD -> stringResource(R.string.alerts_empty_unread)
                AlertFilter.NOTICE -> stringResource(R.string.alerts_empty_notice)
                AlertFilter.INFO -> stringResource(R.string.alerts_empty_info)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.alerts_all_clear),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlertItem(
    alert: System.AlertResponse,
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.dismissed)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (alert.dismissed) 0.dp else 2.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Alert Level Indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(getAlertLevelColor(alert.level).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAlertLevelIcon(alert.level),
                        contentDescription = null,
                        tint = getAlertLevelColor(alert.level),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = getAlertLevelColor(alert.level).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = alert.level.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = getAlertLevelColor(alert.level),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = formatTimeAgo(context, alert.datetime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = alert.formatted ?: alert.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (alert.dismissed) FontWeight.Normal else FontWeight.Medium,
                        color = if (alert.dismissed)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!expanded && alert.text.length > 100) {
                        TextButton(
                            onClick = { expanded = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.common_show_more),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Additional Info when expanded
                    if (expanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(stringResource(R.string.attr_source), alert.source)
                        InfoRow(stringResource(R.string.attr_node), alert.node)
                        InfoRow(stringResource(R.string.attr_last_occurrence), formatDateTime(context, alert.last_occurrence))

                        if (expanded) {
                            TextButton(
                                onClick = { expanded = false },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.common_show_less),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (alert.dismissed) {
                    TextButton(onClick = onRestore) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.common_restore))
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.common_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun getAlertLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "CRITICAL", "ALERT" -> Color(0xFFD32F2F)
        "ERROR", "CRIT" -> Color(0xFFE64A19)
        "WARNING", "WARN" -> Color(0xFFF57C00)
        "NOTICE" -> Color(0xFF1976D2)
        "INFO" -> Color(0xFF388E3C)
        "DEBUG" -> Color(0xFF757575)
        else -> MaterialTheme.colorScheme.outline
    }
}

private fun getAlertLevelIcon(level: String): ImageVector {
    return when (level.uppercase()) {
        "CRITICAL", "ALERT", "ERROR", "CRIT" -> Icons.Default.Error
        "WARNING", "WARN" -> Icons.Default.Warning
        "NOTICE", "INFO" -> Icons.Default.Info
        else -> Icons.Default.Notifications
    }
}

private fun formatTimeAgo(context: Context, mongoDate: System.MongoDate): String {
    return try {
        val instant = Instant.ofEpochMilli(mongoDate.date)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)

        when {
            minutes < 1 -> context.getString(R.string.time_just_now)
            minutes < 60 -> context.getString(R.string.time_minutes_ago, minutes)
            hours < 24 -> context.getString(R.string.time_hours_ago, hours)
            days < 7 -> context.getString(R.string.time_days_ago, days)
            else -> formatDateTime(context, mongoDate)
        }
    } catch (_: Exception) {
        context.getString(R.string.common_invalid_date)
    }
}

private fun formatDateTime(context: Context, mongoDate: System.MongoDate): String {
    return try {
        val instant = Instant.ofEpochMilli(mongoDate.date)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        context.getString(R.string.common_invalid_date)
    }
}

enum class AlertFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    UNREAD(R.string.filter_unread),
    NOTICE(R.string.filter_notice),
    INFO(R.string.filter_info)
}