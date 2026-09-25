package com.gegaremant.truenasmobile.ui.homepage.instancesettings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.ui.components.ExpressiveFAB
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.PullToRefreshContent
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader

@Composable
fun GeneralSystemSettingsScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: () -> Unit = {}
) {
    val vm: GeneralSystemSettingsViewModel = viewModel(
        factory = GeneralSystemSettingsViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val isFabVisible = !scrollState.isScrollInProgress
    val defaultVal = stringResource(R.string.generalsettings_default)
    val allIpv4 = stringResource(R.string.generalsettings_all_ipv4)
    val allIpv6 = stringResource(R.string.generalsettings_all_ipv6)
    val noneVal = stringResource(R.string.common_none)
    val notConfiguredCap = stringResource(R.string.generalsettings_not_configured)
    val notConfigured = stringResource(R.string.generalsettings_not_configured)
    val configured = stringResource(R.string.generalsettings_configured)
    val enabled = stringResource(R.string.common_enabled)
    val disabled = stringResource(R.string.common_disabled)
    val yes = stringResource(R.string.common_yes)
    val no = stringResource(R.string.common_no)

    LaunchedEffect(Unit) { vm.loadAll() }

    Scaffold(
        topBar = {
            UnifiedScreenHeader(
                title = stringResource(R.string.generalsettings_title),
                subtitle = stringResource(R.string.generalsettings_subtitle),
                isLoading = uiState.isLoading,
                isRefreshing = false,
                error = uiState.error,
                onDismissError = { vm.clearError() },
                manager = manager,
                onBackPressed = onNavigateBack
            )
        },
        floatingActionButton = {
            ExpressiveFAB(
                modifier = Modifier.offset(y = (-20).dp),
                onClick = onNavigateToEdit,
                visible = isFabVisible,
                initiallyExpanded = true,
                expandedDurationMillis = 2500
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen(stringResource(R.string.generalsettings_loading))
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
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    uiState.checkinWaiting?.let { seconds ->
                        RollbackWaitingCard(seconds = seconds, onCheckIn = { vm.doCheckin() })
                    }
                    ExpressiveSection(title = stringResource(R.string.generalsettings_section_web_interface), icon = Icons.Default.Visibility) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_http_port), value = config.uiPort.toString())
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_https_port), value = config.uiHttpsPort.toString())
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_https_redirect), value = if (config.uiHttpsRedirect == true) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_https_protocols), value = config.uiHttpsProtocols?.joinToString(", ").orEmpty().ifEmpty { defaultVal })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_console_messages), value = if (config.uiConsoleMsg == true) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_x_frame_options), value = config.uiXFrameOptions ?: "SAMEORIGIN")
                        }

                    }

                    ExpressiveSection(title = stringResource(R.string.generalsettings_section_network_addresses), icon = Icons.Default.SettingsEthernet) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_ipv4_addresses), value = config.uiAddress?.joinToString(", ").orEmpty().ifEmpty { allIpv4 })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_ipv6_addresses), value = config.uiV6Address?.joinToString(", ").orEmpty().ifEmpty { allIpv6 })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_allow_list), value = config.uiAllowlist?.joinToString(", ").orEmpty().ifEmpty { noneVal })
                        }

                    }

                    uiState.localUrl?.let { url ->
                        ExpressiveSection(title = stringResource(R.string.generalsettings_section_local_url), icon = Icons.Default.Link) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    ExpressiveSection(title = stringResource(R.string.generalsettings_section_localization), icon = Icons.Default.Language) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_timezone), value = config.timezone.orEmpty().ifEmpty { notConfiguredCap })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_keyboard_map), value = config.kbdMap.orEmpty().ifEmpty { notConfiguredCap })
                        }

                    }

                    ExpressiveSection(title = stringResource(R.string.generalsettings_section_other), icon = Icons.Default.Security) {
                        ExpressiveInfoCard {
                            InfoRow(label = stringResource(R.string.attr_wizard_shown), value = if (config.wizardShown == true) yes else no)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_ds_auth), value = if (config.dsAuth == true) enabled else disabled)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(label = stringResource(R.string.attr_usage_collection), value = if (config.usageCollectionIsSet == true) configured else notConfigured)
                        }

                    }
                }
                }
            }
        }
    }
}
@Composable
private fun RollbackWaitingCard(seconds: Int, onCheckIn: () -> Unit) {
    ExpressiveSection(title = stringResource(R.string.generalsettings_section_rollback), icon = Icons.Default.Timer) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.generalsettings_rollback_msg, seconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onCheckIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.generalsettings_check_in), fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
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