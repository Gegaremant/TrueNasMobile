package com.gegaremant.truenasmobile.ui.homepage.instancesettings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSystemSettingsEditScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {},
    onCheckinNavigateBack: () -> Unit = {}
) {
    val vm: GeneralSystemSettingsViewModel = viewModel(
        factory = GeneralSystemSettingsViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.loadAll() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.toast_settings_saved))
            if (uiState.checkinWaiting != null) {
                vm.doCheckin()
            }
            onCheckinNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); vm.clearError() }
    }

    val config = uiState.config

    var uiPort by remember(config) { mutableStateOf(config?.uiPort?.toString() ?: "80") }
    var uiHttpsPort by remember(config) { mutableStateOf(config?.uiHttpsPort?.toString() ?: "443") }
    var uiHttpsRedirect by remember(config) { mutableStateOf(config?.uiHttpsRedirect ?: false) }
    var uiConsoleMsg by remember(config) { mutableStateOf(config?.uiConsoleMsg ?: false) }
    var uiXFrameOptions by remember(config) { mutableStateOf(config?.uiXFrameOptions ?: "SAMEORIGIN") }

    var selectedTimezone by remember(config) { mutableStateOf(config?.timezone ?: "") }
    var selectedKbdMap by remember(config) { mutableStateOf(config?.kbdMap ?: "") }

    var uiAddressText by remember(config) {
        mutableStateOf(config?.uiAddress?.joinToString(", ") ?: "0.0.0.0")
    }
    var uiV6AddressText by remember(config) {
        mutableStateOf(config?.uiV6Address?.joinToString(", ") ?: "::")
    }
    var uiAllowlistText by remember(config) {
        mutableStateOf(config?.uiAllowlist?.joinToString(", ") ?: "")
    }
    var selectedHttpsProtocols by remember(config) {
        mutableStateOf(config?.uiHttpsProtocols?.joinToString(", ") ?: "")
    }
    var dsAuth by remember(config) { mutableStateOf(config?.dsAuth ?: false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        bottomBar = {
            if (config != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            enabled = !uiState.isSaving
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.common_discard), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val settings = buildSettings(
                                    uiPort = uiPort,
                                    uiHttpsPort = uiHttpsPort,
                                    uiHttpsRedirect = uiHttpsRedirect,
                                    uiConsoleMsg = uiConsoleMsg,
                                    uiXFrameOptions = uiXFrameOptions,
                                    uiAddress = uiAddressText,
                                    uiV6Address = uiV6AddressText,
                                    uiAllowlist = uiAllowlistText,
                                    timezone = selectedTimezone,
                                    kbdMap = selectedKbdMap,
                                    httpsProtocols = selectedHttpsProtocols,
                                    dsAuth = dsAuth
                                )
                                vm.updateSettings(settings)
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.common_save_changes), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
            UnifiedScreenHeader(
                title = stringResource(R.string.generalsettings_edit_title),
                subtitle = stringResource(R.string.generalsettings_edit_subtitle),
                isLoading = uiState.isLoading,
                isRefreshing = false,
                error = null,
                onDismissError = {},
                manager = manager,
                onBackPressed = onNavigateBack
            )
            when {
                uiState.isLoading -> LoadingScreen(stringResource(R.string.generalsettings_edit_loading))
                config != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (uiState.checkinWaiting != null) {
                            item { RollbackInfoBanner(seconds = uiState.checkinWaiting!!, onCheckIn = { vm.doCheckin() }) }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.generalsettings_section_web_interface), icon = Icons.Default.Visibility) {
                                ExpressiveInfoCard {
                                    OutlinedTextField(
                                        value = uiPort,
                                        onValueChange = { uiPort = it },
                                        label = { Text(stringResource(R.string.attr_http_port)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = uiHttpsPort,
                                        onValueChange = { uiHttpsPort = it },
                                        label = { Text(stringResource(R.string.attr_https_port)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(
                                        label = stringResource(R.string.attr_https_redirect),
                                        description = stringResource(R.string.generalsettings_https_redirect_desc),
                                        checked = uiHttpsRedirect,
                                        onCheckedChange = { uiHttpsRedirect = it }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = selectedHttpsProtocols,
                                        onValueChange = { selectedHttpsProtocols = it },
                                        label = { Text(stringResource(R.string.attr_https_protocols)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.generalsettings_https_protocols_hint)) }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(
                                        label = stringResource(R.string.attr_console_messages),
                                        description = stringResource(R.string.generalsettings_console_msg_desc),
                                        checked = uiConsoleMsg,
                                        onCheckedChange = { uiConsoleMsg = it }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = uiXFrameOptions,
                                        onValueChange = { uiXFrameOptions = it },
                                        label = { Text(stringResource(R.string.attr_x_frame_options)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.generalsettings_xframe_hint)) }
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.generalsettings_section_network_addresses), icon = Icons.Default.SettingsEthernet) {
                                ExpressiveInfoCard {
                                    OutlinedTextField(
                                        value = uiAddressText,
                                        onValueChange = { uiAddressText = it },
                                        label = { Text(stringResource(R.string.attr_ipv4_addresses)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.generalsettings_ipv4_hint)) }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = uiV6AddressText,
                                        onValueChange = { uiV6AddressText = it },
                                        label = { Text(stringResource(R.string.attr_ipv6_addresses)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.generalsettings_ipv6_hint)) }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = uiAllowlistText,
                                        onValueChange = { uiAllowlistText = it },
                                        label = { Text(stringResource(R.string.attr_allow_list)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.generalsettings_allowlist_hint)) }
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.generalsettings_section_localization), icon = Icons.Default.Language) {
                                ExpressiveInfoCard {
                                    SearchableDropdown(
                                        label = stringResource(R.string.attr_timezone),
                                        selectedKey = selectedTimezone,
                                        choices = uiState.timezoneChoices,
                                        isLoading = uiState.timezoneChoices.isEmpty() && uiState.isLoading,
                                        onSelected = { selectedTimezone = it }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    SearchableDropdown(
                                        label = stringResource(R.string.attr_keyboard_map),
                                        selectedKey = selectedKbdMap,
                                        choices = uiState.kbdmapChoices,
                                        isLoading = uiState.kbdmapChoices.isEmpty() && uiState.isLoading,
                                        onSelected = { selectedKbdMap = it }
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.generalsettings_section_other), icon = Icons.Default.Security) {
                                ExpressiveInfoCard {
                                    ToggleRow(
                                        label = stringResource(R.string.attr_ds_auth),
                                        description = stringResource(R.string.generalsettings_ds_auth_desc),
                                        checked = dsAuth,
                                        onCheckedChange = { dsAuth = it }
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RollbackInfoBanner(seconds: Int, onCheckIn: () -> Unit) {
    ExpressiveSection(title = stringResource(R.string.generalsettings_section_rollback), icon = Icons.Default.Info) {
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.generalsettings_rollback_restart),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        stringResource(R.string.generalsettings_rollback_auto, seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onCheckIn, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.generalsettings_checkin), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableDropdown(
    label: String,
    selectedKey: String,
    choices: Map<String, String>,
    isLoading: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val displayValue = choices[selectedKey] ?: selectedKey

    val filtered = remember(choices, query) {
        if (query.isBlank()) choices.entries.toList()
        else choices.entries.filter {
            it.key.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && !isLoading }
    ) {
        OutlinedTextField(
            value = if (expanded) query else displayValue,
            onValueChange = { query = it; if (!expanded) expanded = true },
            readOnly = !expanded,
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.generalsettings_search_placeholder, label)) },
            leadingIcon = if (expanded) {
                { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null,
            trailingIcon = {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded && !isLoading,
            onDismissRequest = { expanded = false; query = "" }
        ) {
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text(stringResource(R.string.generalsettings_no_matches)) }, onClick = {}, enabled = false)
            }
            filtered.take(200).forEach { (key, name) ->
                DropdownMenuItem(
                    text = { Text("$key ($name)") },
                    onClick = {
                        onSelected(key)
                        query = ""
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun buildSettings(
    uiPort: String,
    uiHttpsPort: String,
    uiHttpsRedirect: Boolean,
    uiConsoleMsg: Boolean,
    uiXFrameOptions: String,
    uiAddress: String,
    uiV6Address: String,
    uiAllowlist: String,
    timezone: String,
    kbdMap: String,
    httpsProtocols: String,
    dsAuth: Boolean
): System.SystemGeneralUpdateArgs {
    return System.SystemGeneralUpdateArgs(
        uiPort = uiPort.toIntOrNull(),
        uiHttpsPort = uiHttpsPort.toIntOrNull(),
        uiHttpsRedirect = uiHttpsRedirect,
        uiConsoleMsg = uiConsoleMsg,
        uiXFrameOptions = uiXFrameOptions.ifBlank { null },
        uiAddress = uiAddress.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { null },
        uiV6Address = uiV6Address.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { null },
        uiAllowlist = uiAllowlist.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { null },
        timezone = timezone.ifBlank { null },
        kbdMap = kbdMap.ifBlank { null },
        uiHttpsProtocols = httpsProtocols.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { null },
        dsAuth = dsAuth
    )
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}