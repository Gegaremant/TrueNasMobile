package com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
fun AdvancedSystemSettingsEditScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {}
) {
    val vm: AdvancedSystemSettingsViewModel = viewModel(
        factory = AdvancedSystemSettingsViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.loadAll() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.toast_settings_saved))
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); vm.clearError() }
    }

    val config = uiState.config

    var consolemenu by remember(config) { mutableStateOf(config?.consolemenu ?: false) }
    var consolemsg by remember(config) { mutableStateOf(config?.consolemsg ?: false) }
    var serialconsole by remember(config) { mutableStateOf(config?.serialconsole ?: false) }
    var selectedSerialPort by remember(config) { mutableStateOf(config?.serialport ?: "") }
    var selectedSerialSpeed by remember(config) { mutableStateOf(config?.serialspeed ?: "") }

    var debugkernel by remember(config) { mutableStateOf(config?.debugkernel ?: false) }
    var kdumpEnabled by remember(config) { mutableStateOf(config?.kdump_enabled ?: false) }
    var autotune by remember(config) { mutableStateOf(config?.autotune ?: false) }
    var advancedmode by remember(config) { mutableStateOf(config?.advancedmode ?: false) }
    var kernelExtraOptions by remember(config) { mutableStateOf(config?.kernel_extra_options ?: "") }

    var traceback by remember(config) { mutableStateOf(config?.traceback ?: false) }
    var uploadcrash by remember(config) { mutableStateOf(config?.uploadcrash ?: false) }
    var anonstats by remember(config) { mutableStateOf(config?.anonstats ?: false) }
    var fqdnSyslog by remember(config) { mutableStateOf(config?.fqdn_syslog ?: false) }
    var syslogAudit by remember(config) { mutableStateOf(config?.syslog_audit ?: false) }
    var selectedSyslogLevel by remember(config) { mutableStateOf(config?.sysloglevel ?: "") }

    var powerdaemon by remember(config) { mutableStateOf(config?.powerdaemon ?: false) }
    var bootScrub by remember(config) { mutableStateOf(config?.boot_scrub?.toString() ?: "7") }
    var overprovision by remember(config) { mutableStateOf(config?.overprovision?.toString() ?: "") }

    var motd by remember(config) { mutableStateOf(config?.motd ?: "") }
    var loginBanner by remember(config) { mutableStateOf(config?.login_banner ?: "") }

    val syslogServers = remember(config) {
        mutableStateListOf<System.SyslogServer>().apply {
            config?.syslogservers?.forEach { add(it) }
            if (isEmpty()) add(System.SyslogServer(host = "", transport = "UDP"))
        }
    }

    val serialSpeedOptions = mapOf(
        "9600" to "9600", "19200" to "19200", "38400" to "38400",
        "57600" to "57600", "115200" to "115200", "230400" to "230400"
    )
    val syslogLevelOptions = mapOf(
        "FATAL" to "FATAL", "ERROR" to "ERROR", "WARNING" to "WARNING",
        "INFO" to "INFO", "DEBUG" to "DEBUG", "NOTICE" to "NOTICE", "TRACE" to "TRACE"
    )

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
                                val update = System.SystemAdvancedUpdateArgs(
                                    advancedmode = advancedmode,
                                    autotune = autotune,
                                    kdump_enabled = kdumpEnabled,
                                    boot_scrub = bootScrub.toIntOrNull(),
                                    consolemenu = consolemenu,
                                    consolemsg = consolemsg,
                                    debugkernel = debugkernel,
                                    fqdn_syslog = fqdnSyslog,
                                    motd = motd.ifBlank { null },
                                    login_banner = loginBanner.ifBlank { null },
                                    powerdaemon = powerdaemon,
                                    serialconsole = serialconsole,
                                    serialport = selectedSerialPort.ifBlank { null },
                                    serialspeed = selectedSerialSpeed.ifBlank { null },
                                    overprovision = overprovision.toIntOrNull(),
                                    traceback = traceback,
                                    uploadcrash = uploadcrash,
                                    anonstats = anonstats,
                                    sysloglevel = selectedSyslogLevel.ifBlank { null },
                                    syslogservers = syslogServers.filter { it.host.isNotBlank() }.ifEmpty { null },
                                    syslog_audit = syslogAudit,
                                    kernel_extra_options = kernelExtraOptions.ifBlank { null }
                                )
                                vm.updateSettings(update)
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
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
                title = stringResource(R.string.advancedsettings_edit_title),
                subtitle = stringResource(R.string.advancedsettings_edit_subtitle),
                isLoading = uiState.isLoading,
                isRefreshing = false,
                error = null,
                onDismissError = {},
                manager = manager,
                onBackPressed = onNavigateBack
            )
            when {
                uiState.isLoading -> LoadingScreen(stringResource(R.string.advancedsettings_loading))
                config != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            ExpressiveSection(title = stringResource(R.string.advancedsettings_section_console_serial), icon = Icons.Default.Menu) {
                                ExpressiveInfoCard {
                                    ToggleRow(stringResource(R.string.attr_console_menu), stringResource(R.string.advancedsettings_console_menu_desc), consolemenu) { consolemenu = it }
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(stringResource(R.string.attr_console_messages), stringResource(R.string.advancedsettings_console_msg_display_desc), consolemsg) { consolemsg = it }
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(stringResource(R.string.attr_serial_console), stringResource(R.string.advancedsettings_serial_console_desc), serialconsole) { serialconsole = it }
                                    Spacer(Modifier.height(12.dp))
                                    SearchableDropdown(
                                        label = stringResource(R.string.attr_serial_port),
                                        selectedKey = selectedSerialPort,
                                        choices = uiState.serialPortChoices,
                                        isLoading = uiState.serialPortChoices.isEmpty() && uiState.isLoading,
                                        onSelected = { selectedSerialPort = it }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    SearchableDropdown(
                                        label = stringResource(R.string.attr_serial_speed),
                                        selectedKey = selectedSerialSpeed,
                                        choices = serialSpeedOptions,
                                        isLoading = false,
                                        onSelected = { selectedSerialSpeed = it }
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.advancedsettings_section_kernel_debug), icon = Icons.Default.BugReport) {
                                ExpressiveInfoCard {
                                    ToggleRow(stringResource(R.string.attr_debug_kernel), stringResource(R.string.advancedsettings_debug_kernel_desc), debugkernel) { debugkernel = it }
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(stringResource(R.string.attr_kdump), stringResource(R.string.advancedsettings_kdump_desc), kdumpEnabled) { kdumpEnabled = it }
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(stringResource(R.string.attr_autotune), stringResource(R.string.advancedsettings_autotune_desc), autotune) { autotune = it }
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(stringResource(R.string.attr_advanced_mode), stringResource(R.string.advancedsettings_advanced_mode_desc), advancedmode) { advancedmode = it }
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = kernelExtraOptions,
                                        onValueChange = { kernelExtraOptions = it },
                                        label = { Text(stringResource(R.string.attr_extra_kernel_options)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = false,
                                        minLines = 2,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.advancedsettings_section_syslog), icon = Icons.Default.Dns) {
                                ExpressiveInfoCard {
                                    ToggleRow(stringResource(R.string.attr_fqdn_syslog), stringResource(R.string.advancedsettings_fqdn_desc), fqdnSyslog) { fqdnSyslog = it }
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(stringResource(R.string.attr_syslog_audit), stringResource(R.string.advancedsettings_syslog_audit_desc), syslogAudit) { syslogAudit = it }
                                    Spacer(Modifier.height(12.dp))
                                    SearchableDropdown(
                                        label = stringResource(R.string.attr_syslog_level),
                                        selectedKey = selectedSyslogLevel,
                                        choices = syslogLevelOptions,
                                        isLoading = false,
                                        onSelected = { selectedSyslogLevel = it }
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        stringResource(R.string.attr_syslog_servers),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    syslogServers.forEachIndexed { index, server ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = server.host,
                                                onValueChange = { newHost -> syslogServers[index] = server.copy(host = newHost) },
                                                label = { Text(stringResource(R.string.advancedsettings_host_port)) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                placeholder = { Text(stringResource(R.string.advancedsettings_syslog_placeholder)) }
                                            )
                                            IconButton(
                                                onClick = { syslogServers.removeAt(index) },
                                                enabled = syslogServers.size > 1
                                            ) {
                                                Icon(Icons.Default.Close, stringResource(R.string.common_remove), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { syslogServers.add(System.SyslogServer(host = "", transport = "UDP")) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.advancedsettings_add_server))
                                    }
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.advancedsettings_section_messaging), icon = Icons.Default.Mail) {
                                ExpressiveInfoCard {
                                    OutlinedTextField(
                                        value = loginBanner,
                                        onValueChange = { loginBanner = it },
                                        label = { Text(stringResource(R.string.attr_login_banner)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = false,
                                        minLines = 2,
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.advancedsettings_login_banner_desc)) }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = motd,
                                        onValueChange = { motd = it },
                                        label = { Text(stringResource(R.string.attr_message_of_the_day)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = false,
                                        minLines = 2,
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.advancedsettings_motd_desc)) }
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.advancedsettings_section_crash_reporting), icon = Icons.Default.Report) {
                                ExpressiveInfoCard {
                                    ToggleRow(stringResource(R.string.attr_traceback), stringResource(R.string.advancedsettings_traceback_desc), traceback) { traceback = it }
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(stringResource(R.string.attr_upload_crashes), stringResource(R.string.advancedsettings_upload_desc), uploadcrash) { uploadcrash = it }
                                    Spacer(Modifier.height(12.dp))
                                    ToggleRow(stringResource(R.string.attr_anon_stats), stringResource(R.string.advancedsettings_anon_stats_desc), anonstats) { anonstats = it }
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.advancedsettings_section_power), icon = Icons.Default.PowerSettingsNew) {
                                ExpressiveInfoCard {
                                    ToggleRow(stringResource(R.string.attr_power_daemon), stringResource(R.string.advancedsettings_power_daemon_desc), powerdaemon) { powerdaemon = it }
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = bootScrub,
                                        onValueChange = { bootScrub = it },
                                        label = { Text(stringResource(R.string.advancedsettings_boot_scrub_interval)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.advancedsettings_section_storage), icon = Icons.Default.SdStorage) {
                                ExpressiveInfoCard {
                                    OutlinedTextField(
                                        value = overprovision,
                                        onValueChange = { overprovision = it },
                                        label = { Text(stringResource(R.string.advancedsettings_overprovision_gb)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.advancedsettings_leave_empty_default)) }
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
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
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
                    text = { Text(if (key == name) key else "$key ($name)") },
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
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}