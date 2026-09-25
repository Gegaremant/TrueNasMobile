package com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.ui.components.ExpressiveFAB
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.PullToRefreshContent
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader

@Composable
fun AdvancedSystemSettingsScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: () -> Unit = {}
) {
    val vm: AdvancedSystemSettingsViewModel = viewModel(
        factory = AdvancedSystemSettingsViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val isFabVisible = !scrollState.isScrollInProgress
    val enabled = stringResource(R.string.common_enabled)
    val disabled = stringResource(R.string.common_disabled)
    val yes = stringResource(R.string.common_yes)
    val no = stringResource(R.string.common_no)
    val noneVal = stringResource(R.string.common_none)
    val dash = stringResource(R.string.common_dash)
    val notConfigured = stringResource(R.string.generalsettings_not_configured)
    val notSet = stringResource(R.string.network_not_set)

    LaunchedEffect(Unit) { vm.loadAll() }

    Scaffold(
        topBar = {
            UnifiedScreenHeader(
                title = stringResource(R.string.advancedsettings_title),
                subtitle = stringResource(R.string.advancedsettings_subtitle),
                isLoading = uiState.isLoading,
                isRefreshing = false,
                error = uiState.error,
                onDismissError = { vm.clearError() },
                manager = manager,
                onBackPressed = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExpressiveFAB(
                onClick = onNavigateToEdit,
                visible = isFabVisible,
                initiallyExpanded = true,
                expandedDurationMillis = 2500
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen(stringResource(R.string.advancedsettings_loading))
            uiState.config != null -> {
                val config = uiState.config!!
                PullToRefreshContent(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { vm.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (config.login_banner.isNotBlank() || config.motd.isNotBlank()) {
                        ExpressiveSection(title = stringResource(R.string.advancedsettings_section_messaging), icon = Icons.Default.Mail) {
                            MessageBannerCard(loginBanner = config.login_banner, motd = config.motd)
                        }
                    }

                    ExpressiveSection(title = stringResource(R.string.advancedsettings_section_console_serial), icon = Icons.Default.Menu) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_console_menu), value = if (config.consolemenu) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_console_messages), value = if (config.consolemsg) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_serial_console), value = if (config.serialconsole) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_serial_port), value = config.serialport.ifEmpty { dash })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_serial_speed), value = config.serialspeed.ifEmpty { dash })
                        }
                        // ❌ SectionEditButton removed
                    }

                    ExpressiveSection(title = stringResource(R.string.advancedsettings_section_kernel_debug), icon = Icons.Default.BugReport) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_debug_kernel), value = if (config.debugkernel) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_kdump), value = if (config.kdump_enabled) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_autotune), value = if (config.autotune) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_advanced_mode), value = if (config.advancedmode) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_extra_kernel_options), value = config.kernel_extra_options.ifEmpty { noneVal })
                        }
                    }

                    ExpressiveSection(title = stringResource(R.string.advancedsettings_section_syslog), icon = Icons.Default.Dns) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_syslog_level), value = config.sysloglevel.ifEmpty { dash })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_fqdn_syslog), value = if (config.fqdn_syslog) yes else no)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(
                                label = stringResource(R.string.attr_syslog_audit),
                                value = when (config.syslog_audit) {
                                    true -> enabled
                                    false -> disabled
                                    null -> notConfigured
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            val serverCount = config.syslogservers?.size ?: 0
                            InfoRow(label = stringResource(R.string.attr_syslog_servers), value = if (serverCount > 0) stringResource(R.string.advancedsettings_servers_configured, serverCount) else noneVal)
                        }
                    }

                    ExpressiveSection(title = stringResource(R.string.advancedsettings_section_crash_reporting), icon = Icons.Default.Report) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_traceback), value = if (config.traceback) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_upload_crashes), value = if (config.uploadcrash) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_anon_stats), value = if (config.anonstats) enabled else disabled)
                        }
                    }

                    ExpressiveSection(title = stringResource(R.string.advancedsettings_section_gpu_isolation), icon = Icons.Default.DeveloperBoard) {
                        ExpressiveInfoCard {
                            val gpuCount = config.isolated_gpu_pci_ids.size
                            InfoRow(label = stringResource(R.string.attr_isolated_gpus), value = if (gpuCount > 0) stringResource(R.string.advancedsettings_gpu_isolated, gpuCount) else noneVal)
                        }
                    }

                    ExpressiveSection(title = stringResource(R.string.advancedsettings_section_power), icon = Icons.Default.PowerSettingsNew) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_power_daemon), value = if (config.powerdaemon) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_boot_scrub), value = "${config.boot_scrub}")
                        }
                    }

                    ExpressiveSection(title = stringResource(R.string.advancedsettings_section_sed), icon = Icons.Default.Key) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_sed_user), value = config.sed_user.ifEmpty { dash })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_sed_password_set), value = if (uiState.sedPasswordIsSet) yes else no)
                        }
                    }

                    ExpressiveSection(title = stringResource(R.string.advancedsettings_section_storage), icon = Icons.Default.SdStorage) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_overprovision), value = config.overprovision?.toString() ?: notSet)
                        }
                    }
                    Spacer(modifier = Modifier.height(64.dp))
                }
                }
            }
        }
    }
}

@Composable
private fun MessageBannerCard(loginBanner: String, motd: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loginBanner.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.attr_login_banner),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            loginBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            if (motd.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Default.Mail, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.attr_message_of_the_day),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            motd,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExpressiveSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(content = content)
    }
}

@Composable
internal fun ExpressiveInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
internal fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}