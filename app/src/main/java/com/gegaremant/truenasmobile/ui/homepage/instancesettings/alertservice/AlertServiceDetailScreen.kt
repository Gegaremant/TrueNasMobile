package com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.Alerts
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertsservice.AlertServiceDetailViewModel

@Composable
fun AlertServiceDetailScreen(
    serviceId: Int,
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: AlertServiceDetailViewModel = viewModel(
        factory = AlertServiceDetailViewModel.AlertServiceDetailViewModelFactory(manager, serviceId),
        key = serviceId.toString()
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editLevel by remember { mutableStateOf(Alerts.AlertLevels.WARNING) }
    var editEnabled by remember { mutableStateOf(true) }
    var editAttrs by remember { mutableStateOf(Alerts.AlertServiceAttributes()) }

    fun enterEditMode(service: Alerts.AlertServiceEntry) {
        editName = service.name
        editLevel = service.level
        editEnabled = service.enabled
        editAttrs = service.attributes
        isEditing = true
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.deleteResult) {
        if (uiState.deleteResult == true) onNavigateBack()
    }
    LaunchedEffect(uiState.testResult) {
        uiState.testResult?.let { success ->
            snackbarHostState.showSnackbar(if (success) context.getString(R.string.toast_test_alert_sent) else context.getString(R.string.toast_test_alert_failed))
            viewModel.clearTestResult()
        }
    }
    LaunchedEffect(uiState.updateResult) {
        if (uiState.updateResult == true) {
            isEditing = false
            snackbarHostState.showSnackbar(context.getString(R.string.toast_service_updated))
        }
    }

    val current = uiState.service
    var showDeleteDialog by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(
        targetValue = if (current?.enabled == true) Color(0xFF2E7D32)
        else MaterialTheme.colorScheme.outline,
        label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceContainer)
                )
            )
    ) {
        UnifiedScreenHeader(
            title = current?.name ?: stringResource(R.string.alertservice_title),
            subtitle = current?.type__title ?: stringResource(R.string.alertservice_subtitle),
            isLoading = uiState.isLoading,
            isRefreshing = false,
            error = null,
            onDismissError = {},
            manager = manager,
            onBackPressed = onNavigateBack
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> LoadingScreen(stringResource(R.string.alertservice_loading))
                current != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status header card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, null, tint = statusColor, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(current.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(current.type__title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (current.enabled) stringResource(R.string.common_enabled) else stringResource(R.string.common_disabled), style = MaterialTheme.typography.labelMedium, color = statusColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (isEditing) {
                        EditServiceForm(
                            name = editName, onNameChange = { editName = it },
                            level = editLevel, onLevelChange = { editLevel = it },
                            enabled = editEnabled, onEnabledChange = { editEnabled = it },
                            attrs = editAttrs, onAttrsChange = { editAttrs = it }
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.common_cancel))
                            }
                            Button(
                                onClick = {
                                    viewModel.updateService(
                                        Alerts.AlertServiceUpdate(
                                            name = editName, attributes = editAttrs,
                                            level = editLevel, enabled = editEnabled
                                        )
                                    )
                                },
                                enabled = !uiState.isUpdating && editName.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isUpdating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.common_save))
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.alertservice_attributes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                AlertServiceAttributesSummary(current.attributes)
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.alertservice_min_level), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(current.level.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { enterEditMode(current) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.common_edit))
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.testService(
                                        Alerts.AlertServiceCreate(current.name, current.attributes, current.level, current.enabled)
                                    )
                                },
                                enabled = !uiState.isTesting,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isTesting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.common_test))
                            }
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !uiState.isDeleting,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isDeleting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.common_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.alertservice_delete_title)) },
            text = { Text(stringResource(R.string.alertservice_delete_message, current?.name.orEmpty())) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.deleteService() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditServiceForm(
    name: String, onNameChange: (String) -> Unit,
    level: Alerts.AlertLevels, onLevelChange: (Alerts.AlertLevels) -> Unit,
    enabled: Boolean, onEnabledChange: (Boolean) -> Unit,
    attrs: Alerts.AlertServiceAttributes, onAttrsChange: (Alerts.AlertServiceAttributes) -> Unit
) {
    var levelExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name, onValueChange = onNameChange,
                label = { Text(stringResource(R.string.alertservice_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            ExposedDropdownMenuBox(expanded = levelExpanded, onExpandedChange = { levelExpanded = it }) {
                OutlinedTextField(
                    value = level.name, onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.alertservice_min_level)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = levelExpanded, onDismissRequest = { levelExpanded = false }) {
                    Alerts.AlertLevels.entries.forEach { lvl ->
                        DropdownMenuItem(text = { Text(lvl.name) }, onClick = { onLevelChange(lvl); levelExpanded = false })
                    }
                }
            }

            // Per-type attribute fields — mirrors AlertServiceCreateScreen's pattern
            when (attrs.type) {
                "Slack" -> OutlinedTextField(value = attrs.url ?: "", onValueChange = { onAttrsChange(attrs.copy(url = it)) }, label = { Text(stringResource(R.string.attr_webhook_url)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                "Mail" -> OutlinedTextField(value = attrs.email ?: "", onValueChange = { onAttrsChange(attrs.copy(email = it)) }, label = { Text(stringResource(R.string.attr_email)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                "Mattermost" -> {
                    OutlinedTextField(value = attrs.url ?: "", onValueChange = { onAttrsChange(attrs.copy(url = it)) }, label = { Text(stringResource(R.string.attr_webhook_url)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.username ?: "", onValueChange = { onAttrsChange(attrs.copy(username = it)) }, label = { Text(stringResource(R.string.settings_username)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.channel ?: "", onValueChange = { onAttrsChange(attrs.copy(channel = it)) }, label = { Text(stringResource(R.string.attr_channel_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "PagerDuty" -> {
                    OutlinedTextField(value = attrs.service_key ?: "", onValueChange = { onAttrsChange(attrs.copy(service_key = it)) }, label = { Text(stringResource(R.string.attr_service_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.client_name ?: "", onValueChange = { onAttrsChange(attrs.copy(client_name = it)) }, label = { Text(stringResource(R.string.attr_client_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "Telegram" -> {
                    OutlinedTextField(value = attrs.bot_token ?: "", onValueChange = { onAttrsChange(attrs.copy(bot_token = it)) }, label = { Text(stringResource(R.string.attr_bot_token)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(
                        value = attrs.chat_ids?.joinToString(", ") ?: "",
                        onValueChange = { onAttrsChange(attrs.copy(chat_ids = it.split(",").mapNotNull { id -> id.trim().toIntOrNull() })) },
                        label = { Text(stringResource(R.string.attr_chat_ids)) }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                "OpsGenie" -> {
                    OutlinedTextField(value = attrs.api_key ?: "", onValueChange = { onAttrsChange(attrs.copy(api_key = it)) }, label = { Text(stringResource(R.string.settings_api_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.api_url ?: "", onValueChange = { onAttrsChange(attrs.copy(api_url = it)) }, label = { Text(stringResource(R.string.attr_api_url_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "VictorOps" -> {
                    OutlinedTextField(value = attrs.api_key ?: "", onValueChange = { onAttrsChange(attrs.copy(api_key = it)) }, label = { Text(stringResource(R.string.settings_api_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.routing_key ?: "", onValueChange = { onAttrsChange(attrs.copy(routing_key = it)) }, label = { Text(stringResource(R.string.attr_routing_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "AWSSNS" -> {
                    OutlinedTextField(value = attrs.region ?: "", onValueChange = { onAttrsChange(attrs.copy(region = it)) }, label = { Text(stringResource(R.string.attr_region)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.topic_arn ?: "", onValueChange = { onAttrsChange(attrs.copy(topic_arn = it)) }, label = { Text(stringResource(R.string.attr_topic_arn)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.aws_access_key_id ?: "", onValueChange = { onAttrsChange(attrs.copy(aws_access_key_id = it)) }, label = { Text(stringResource(R.string.attr_access_key_id)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.aws_secret_access_key ?: "", onValueChange = { onAttrsChange(attrs.copy(aws_secret_access_key = it)) }, label = { Text(stringResource(R.string.attr_secret_access_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "InfluxDB" -> {
                    OutlinedTextField(value = attrs.host ?: "", onValueChange = { onAttrsChange(attrs.copy(host = it)) }, label = { Text(stringResource(R.string.attr_host)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.username ?: "", onValueChange = { onAttrsChange(attrs.copy(username = it)) }, label = { Text(stringResource(R.string.settings_username)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.password ?: "", onValueChange = { onAttrsChange(attrs.copy(password = it)) }, label = { Text(stringResource(R.string.settings_password_field)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.database ?: "", onValueChange = { onAttrsChange(attrs.copy(database = it)) }, label = { Text(stringResource(R.string.attr_database)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.series_name ?: "", onValueChange = { onAttrsChange(attrs.copy(series_name = it)) }, label = { Text(stringResource(R.string.attr_series_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "SNMPTrap" -> {
                    OutlinedTextField(value = attrs.host ?: "", onValueChange = { onAttrsChange(attrs.copy(host = it)) }, label = { Text(stringResource(R.string.attr_host)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.port?.toString() ?: "", onValueChange = { onAttrsChange(attrs.copy(port = it.toIntOrNull())) }, label = { Text(stringResource(R.string.attr_port)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = attrs.community ?: "", onValueChange = { onAttrsChange(attrs.copy(community = it)) }, label = { Text(stringResource(R.string.attr_community)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                else -> Text(stringResource(R.string.alertservice_no_fields, attrs.type.orEmpty()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.common_enabled), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }
    }
}

@Composable
private fun AlertServiceAttributesSummary(attrs: Alerts.AlertServiceAttributes) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (attrs.type) {
            "AWSSNS" -> { DetailRow(stringResource(R.string.attr_region), attrs.region); DetailRow(stringResource(R.string.attr_topic_arn), attrs.topic_arn) }
            "InfluxDB" -> { DetailRow(stringResource(R.string.attr_host), attrs.host); DetailRow(stringResource(R.string.settings_username), attrs.username); DetailRow(stringResource(R.string.attr_database), attrs.database); DetailRow(stringResource(R.string.attr_series_name), attrs.series_name) }
            "Mail" -> DetailRow(stringResource(R.string.attr_email), attrs.email)
            "Mattermost" -> { DetailRow(stringResource(R.string.attr_url), attrs.url); DetailRow(stringResource(R.string.settings_username), attrs.username); DetailRow(stringResource(R.string.attr_channel), attrs.channel) }
            "OpsGenie" -> { DetailRow(stringResource(R.string.settings_api_key), attrs.api_key?.take(8) + "..."); DetailRow(stringResource(R.string.attr_api_url), attrs.api_url) }
            "PagerDuty" -> { DetailRow(stringResource(R.string.attr_service_key), attrs.service_key?.take(8) + "..."); DetailRow(stringResource(R.string.attr_client_name), attrs.client_name) }
            "Slack" -> DetailRow(stringResource(R.string.attr_webhook_url), attrs.url)
            "SNMPTrap" -> { DetailRow(stringResource(R.string.attr_host), attrs.host); DetailRow(stringResource(R.string.attr_port), attrs.port?.toString()); DetailRow("v3", attrs.v3?.toString()) }
            "Telegram" -> { DetailRow(stringResource(R.string.attr_bot_token), attrs.bot_token?.take(8) + "..."); DetailRow(stringResource(R.string.attr_chat_ids_title), attrs.chat_ids?.joinToString(", ")) }
            "VictorOps" -> { DetailRow(stringResource(R.string.settings_api_key), attrs.api_key?.take(8) + "..."); DetailRow(stringResource(R.string.attr_routing_key), attrs.routing_key) }
            else -> DetailRow(stringResource(R.string.attr_type), attrs.type ?: stringResource(R.string.common_unknown))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value != null) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}