package com.gegaremant.truenasmobile.ui.homepage.instancesettings.alertservice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.Alerts
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import kotlinx.coroutines.launch

private val SERVICE_TYPES = listOf(
    "AWSSNS", "InfluxDB", "Mail", "Mattermost", "OpsGenie",
    "PagerDuty", "Slack", "SNMPTrap", "Telegram", "VictorOps", "Webhook"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertServiceCreateScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Slack") }
    var level by remember { mutableStateOf(Alerts.AlertLevels.WARNING) }
    var enabled by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var levelExpanded by remember { mutableStateOf(false) }

    var slackUrl by remember { mutableStateOf("") }
    var mailEmail by remember { mutableStateOf("") }
    var mattermostUrl by remember { mutableStateOf("") }
    var mattermostUsername by remember { mutableStateOf("") }
    var mattermostChannel by remember { mutableStateOf("") }
    var pagerdutyServiceKey by remember { mutableStateOf("") }
    var pagerdutyClientName by remember { mutableStateOf("") }
    var telegramBotToken by remember { mutableStateOf("") }
    var telegramChatIds by remember { mutableStateOf("") }
    var opsgenieApiKey by remember { mutableStateOf("") }
    var opsgenieApiUrl by remember { mutableStateOf("") }
    var victoropsApiKey by remember { mutableStateOf("") }
    var victoropsRoutingKey by remember { mutableStateOf("") }
    var awsRegion by remember { mutableStateOf("") }
    var awsTopicArn by remember { mutableStateOf("") }
    var awsAccessKey by remember { mutableStateOf("") }
    var awsSecretKey by remember { mutableStateOf("") }
    var influxHost by remember { mutableStateOf("") }
    var influxUsername by remember { mutableStateOf("") }
    var influxPassword by remember { mutableStateOf("") }
    var influxDatabase by remember { mutableStateOf("") }
    var influxSeriesName by remember { mutableStateOf("") }
    // SNMPTrap
    var snmpHost by remember { mutableStateOf("") }
    var snmpPort by remember { mutableStateOf("162") }
    var snmpV3 by remember { mutableStateOf(false) }
    var snmpCommunity by remember { mutableStateOf("") }
    // Webhook
    var webhookUrl by remember { mutableStateOf("") }
    var webhookHttpMethod by remember { mutableStateOf("POST") }
    var webhookHttpAuth by remember { mutableStateOf("NONE") }
    var webhookUsername by remember { mutableStateOf("") }
    var webhookPassword by remember { mutableStateOf("") }
    var webhookHeaders by remember { mutableStateOf("") }

    fun buildAttributes(): Alerts.AlertServiceAttributes = when (selectedType) {
        "AWSSNS" -> Alerts.AlertServiceAttributes(
            type = "AWSSNS", region = awsRegion, topic_arn = awsTopicArn,
            aws_access_key_id = awsAccessKey, aws_secret_access_key = awsSecretKey
        )
        "InfluxDB" -> Alerts.AlertServiceAttributes(
            type = "InfluxDB", host = influxHost, username = influxUsername,
            password = influxPassword, database = influxDatabase, series_name = influxSeriesName
        )
        "Mail" -> Alerts.AlertServiceAttributes(
            type = "Mail", email = mailEmail
        )
        "Mattermost" -> Alerts.AlertServiceAttributes(
            type = "Mattermost", url = mattermostUrl, username = mattermostUsername,
            channel = mattermostChannel
        )
        "OpsGenie" -> Alerts.AlertServiceAttributes(
            type = "OpsGenie", api_key = opsgenieApiKey, api_url = opsgenieApiUrl
        )
        "PagerDuty" -> Alerts.AlertServiceAttributes(
            type = "PagerDuty", service_key = pagerdutyServiceKey, client_name = pagerdutyClientName
        )
        "Slack" -> Alerts.AlertServiceAttributes(
            type = "Slack", url = slackUrl
        )
        "SNMPTrap" -> Alerts.AlertServiceAttributes(
            type = "SNMPTrap", host = snmpHost, port = snmpPort.toIntOrNull(),
            v3 = snmpV3, community = snmpCommunity
        )
        "Telegram" -> Alerts.AlertServiceAttributes(
            type = "Telegram", bot_token = telegramBotToken,
            chat_ids = telegramChatIds.split(",").mapNotNull { it.trim().toIntOrNull() }
        )
        "VictorOps" -> Alerts.AlertServiceAttributes(
            type = "VictorOps", api_key = victoropsApiKey, routing_key = victoropsRoutingKey
        )
        "Webhook" -> Alerts.AlertServiceAttributes(
            type = "Webhook", webhook_url = webhookUrl,
            webhook_http_method = webhookHttpMethod, webhook_http_auth = webhookHttpAuth,
            webhook_username = webhookUsername, webhook_password = webhookPassword,
            webhook_headers = webhookHeaders.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val separator = line.indexOf(':')
                    if (separator > 0) Alerts.AlertServiceHeader(
                        name = line.substring(0, separator).trim(),
                        value = line.substring(separator + 1).trim()
                    ) else null
                }
                .toList()
        )
        else -> Alerts.AlertServiceAttributes(type = selectedType)
    }

    fun save() {
        scope.launch {
            isSaving = true
            val create = Alerts.AlertServiceCreate(
                name = name, attributes = buildAttributes(), level = level, enabled = enabled
            )
            when (val result = manager.alertsService.createAlertServiceWithResult(create)) {
                is ApiResult.Success -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_alert_service_created))
                    onNavigateBack()
                }
                is ApiResult.Error -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_error_pattern, result.message))
                }
                else -> {}
            }
            isSaving = false
        }
    }

    fun test() {
        scope.launch {
            isTesting = true
            val create = Alerts.AlertServiceCreate(
                name = name, attributes = buildAttributes(), level = level, enabled = enabled
            )
            when (val result = manager.alertsService.testAlertServiceWithResult(create)) {
                is ApiResult.Success -> {
                    snackbarHostState.showSnackbar(
                        if (result.data) context.getString(R.string.toast_test_successful) else context.getString(R.string.toast_test_failed)
                    )
                }
                is ApiResult.Error -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.toast_test_error_pattern, result.message))
                }
                else -> {}
            }
            isTesting = false
        }
    }

    Scaffold(
        topBar = {
            UnifiedScreenHeader(
                title = stringResource(R.string.alertservice_create_title),
                subtitle = stringResource(R.string.alertservice_create_subtitle),
                isLoading = false,
                isRefreshing = false,
                error = null,
                onDismissError = {},
                manager = manager,
                onBackPressed = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(snackbarData = it) } },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.alertservice_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.alertservice_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    SERVICE_TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = levelExpanded,
                onExpandedChange = { levelExpanded = it }
            ) {
                OutlinedTextField(
                    value = level.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.alertservice_min_level)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = levelExpanded,
                    onDismissRequest = { levelExpanded = false }
                ) {
                    Alerts.AlertLevels.entries.forEach { lvl ->
                        DropdownMenuItem(
                            text = { Text(lvl.name) },
                            onClick = {
                                level = lvl
                                levelExpanded = false
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.alertservice_type_config, selectedType),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    when (selectedType) {
                        "Slack" -> OutlinedTextField(
                            value = slackUrl, onValueChange = { slackUrl = it },
                            label = { Text(stringResource(R.string.attr_webhook_url)) }, modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        "Mail" -> OutlinedTextField(
                            value = mailEmail, onValueChange = { mailEmail = it },
                            label = { Text(stringResource(R.string.attr_email)) }, modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        "Mattermost" -> {
                            OutlinedTextField(value = mattermostUrl, onValueChange = { mattermostUrl = it }, label = { Text(stringResource(R.string.attr_webhook_url)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = mattermostUsername, onValueChange = { mattermostUsername = it }, label = { Text(stringResource(R.string.settings_username)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = mattermostChannel, onValueChange = { mattermostChannel = it }, label = { Text(stringResource(R.string.attr_channel_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        "PagerDuty" -> {
                            OutlinedTextField(value = pagerdutyServiceKey, onValueChange = { pagerdutyServiceKey = it }, label = { Text(stringResource(R.string.attr_service_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = pagerdutyClientName, onValueChange = { pagerdutyClientName = it }, label = { Text(stringResource(R.string.attr_client_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        "Telegram" -> {
                            OutlinedTextField(value = telegramBotToken, onValueChange = { telegramBotToken = it }, label = { Text(stringResource(R.string.attr_bot_token)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = telegramChatIds, onValueChange = { telegramChatIds = it }, label = { Text(stringResource(R.string.attr_chat_ids)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        "OpsGenie" -> {
                            OutlinedTextField(value = opsgenieApiKey, onValueChange = { opsgenieApiKey = it }, label = { Text(stringResource(R.string.settings_api_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = opsgenieApiUrl, onValueChange = { opsgenieApiUrl = it }, label = { Text(stringResource(R.string.attr_api_url_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        "VictorOps" -> {
                            OutlinedTextField(value = victoropsApiKey, onValueChange = { victoropsApiKey = it }, label = { Text(stringResource(R.string.settings_api_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = victoropsRoutingKey, onValueChange = { victoropsRoutingKey = it }, label = { Text(stringResource(R.string.attr_routing_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        "AWSSNS" -> {
                            OutlinedTextField(value = awsRegion, onValueChange = { awsRegion = it }, label = { Text(stringResource(R.string.attr_region)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = awsTopicArn, onValueChange = { awsTopicArn = it }, label = { Text(stringResource(R.string.attr_topic_arn)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = awsAccessKey, onValueChange = { awsAccessKey = it }, label = { Text(stringResource(R.string.attr_access_key_id)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = awsSecretKey, onValueChange = { awsSecretKey = it }, label = { Text(stringResource(R.string.attr_secret_access_key)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        "InfluxDB" -> {
                            OutlinedTextField(value = influxHost, onValueChange = { influxHost = it }, label = { Text(stringResource(R.string.attr_host)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = influxUsername, onValueChange = { influxUsername = it }, label = { Text(stringResource(R.string.settings_username)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = influxPassword, onValueChange = { influxPassword = it }, label = { Text(stringResource(R.string.settings_password_field)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = influxDatabase, onValueChange = { influxDatabase = it }, label = { Text(stringResource(R.string.attr_database)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = influxSeriesName, onValueChange = { influxSeriesName = it }, label = { Text(stringResource(R.string.attr_series_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        "SNMPTrap" -> {
                            OutlinedTextField(value = snmpHost, onValueChange = { snmpHost = it }, label = { Text(stringResource(R.string.attr_host)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = snmpPort, onValueChange = { snmpPort = it }, label = { Text(stringResource(R.string.attr_port)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = snmpCommunity, onValueChange = { snmpCommunity = it }, label = { Text(stringResource(R.string.attr_community)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.alertservice_snmp_v3), modifier = Modifier.weight(1f))
                                Switch(checked = snmpV3, onCheckedChange = { snmpV3 = it })
                            }
                        }
                        "Webhook" -> {
                            OutlinedTextField(value = webhookUrl, onValueChange = { webhookUrl = it }, label = { Text(stringResource(R.string.attr_webhook_url)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(value = webhookHttpMethod, onValueChange = { webhookHttpMethod = it }, label = { Text(stringResource(R.string.attr_http_method)) }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = webhookHttpAuth, onValueChange = { webhookHttpAuth = it }, label = { Text(stringResource(R.string.attr_auth_mode)) }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            OutlinedTextField(value = webhookUsername, onValueChange = { webhookUsername = it }, label = { Text(stringResource(R.string.attr_username_basic_auth)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = webhookPassword, onValueChange = { webhookPassword = it }, label = { Text(stringResource(R.string.attr_password_basic_auth)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(
                                value = webhookHeaders,
                                onValueChange = { webhookHeaders = it },
                                label = { Text(stringResource(R.string.attr_headers)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = stringResource(R.string.attr_webhook_tip),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.common_enabled),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { test() },
                    enabled = !isTesting && !isSaving && name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTesting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.common_test))
                }
                Button(
                    onClick = { save() },
                    enabled = !isSaving && !isTesting && name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.common_create))
                }
            }
        }
    }
}
