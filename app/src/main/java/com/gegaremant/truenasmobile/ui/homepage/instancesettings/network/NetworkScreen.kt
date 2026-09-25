package com.gegaremant.truenasmobile.ui.homepage.instancesettings.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.ExpressiveFAB
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.PullToRefreshContent
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced.ExpressiveInfoCard
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced.InfoRow
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.general.ExpressiveSection

@Composable
fun NetworkScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: () -> Unit = {}
) {
    val vm: NetworkConfigViewModel = viewModel(
        factory = NetworkConfigViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val isFabVisible = !scrollState.isScrollInProgress
    val notConfigured = stringResource(R.string.network_not_configured)
    val notSet = stringResource(R.string.network_not_set)
    val enabled = stringResource(R.string.common_enabled)
    val disabled = stringResource(R.string.common_disabled)

    LaunchedEffect(Unit) { vm.loadAll() }

    Scaffold(
        topBar = {
            UnifiedScreenHeader(
                title = stringResource(R.string.instconfig_network),
                subtitle = stringResource(R.string.network_subtitle),
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
                modifier = Modifier.offset(y=10.dp),
                initiallyExpanded = true,
                expandedDurationMillis = 2500
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen(stringResource(R.string.network_loading))
            uiState.summary != null && uiState.config != null -> {
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

                    if (uiState.summary!!.ips.isNotEmpty()) {
                        ExpressiveSection(stringResource(R.string.network_interfaces), Icons.Default.SettingsEthernet) {
                            uiState.summary!!.ips.forEach { (iface, ipInfo) ->
                                InterfaceCard(iface, ipInfo)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }


                    if (uiState.summary!!.defaultRoutes!!.isNotEmpty()) {
                        ExpressiveSection(stringResource(R.string.network_default_routes), Icons.Default.Router) {
                            ExpressiveInfoCard {
                                uiState.summary!!.defaultRoutes?.forEachIndexed { idx, route ->
                                    InfoRow(stringResource(R.string.network_route, idx), route)
                                    if (idx < uiState.summary!!.defaultRoutes!!.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }


                    if (uiState.summary!!.nameservers.isNotEmpty()) {
                        ExpressiveSection(stringResource(R.string.network_nameservers), Icons.Default.Dns) {
                            ExpressiveInfoCard {
                                uiState.summary!!.nameservers.forEachIndexed { idx, ns ->
                                    InfoRow(stringResource(R.string.network_server_n, idx + 1), ns)
                                    if (idx < uiState.summary!!.nameservers.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }


                    ExpressiveSection(stringResource(R.string.network_identity), Icons.Default.Public) {
                        ExpressiveInfoCard {
                            InfoRow(stringResource(R.string.network_hostname), uiState.config!!.hostname)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_hostname_local), uiState.config!!.hostnameLocal?.ifEmpty { notConfigured } ?: notConfigured)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_domain), uiState.config!!.domain)
                        }
                    }


                    ExpressiveSection(stringResource(R.string.network_gateways), Icons.Default.Router) {
                        ExpressiveInfoCard {
                            InfoRow(stringResource(R.string.network_ipv4_gateway), uiState.config!!.ipv4Gateway ?: notSet)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_ipv6_gateway), uiState.config!!.ipv6Gateway ?: notSet)
                        }
                    }


                    ExpressiveSection(stringResource(R.string.network_dns_servers), Icons.Default.Language) {
                        ExpressiveInfoCard {
                            InfoRow(stringResource(R.string.network_nameserver_n, 1), uiState.config!!.nameserver1 ?: notSet)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_nameserver_n, 2), uiState.config!!.nameserver2 ?: notSet)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_nameserver_n, 3), uiState.config!!.nameserver3 ?: notSet)
                        }
                    }


                    if (!uiState.config!!.httpProxy.isNullOrBlank()) {
                        ExpressiveSection(stringResource(R.string.network_http_proxy), Icons.Default.Visibility) {
                            ExpressiveInfoCard {
                                InfoRow(stringResource(R.string.network_proxy_url), uiState.config!!.httpProxy ?: "")
                            }
                        }
                    }


                    if (uiState.config!!.domains.isNotEmpty()) {
                        ExpressiveSection(stringResource(R.string.network_search_domains), Icons.Default.Language) {
                            ExpressiveInfoCard {
                                uiState.config!!.domains.forEachIndexed { idx, domain ->
                                    InfoRow(stringResource(R.string.network_domain), domain)
                                    if (idx < uiState.config!!.domains.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }


                    if (uiState.config!!.hosts.isNotEmpty()) {
                        ExpressiveSection(stringResource(R.string.network_static_hosts), Icons.Default.Shield) {
                            ExpressiveInfoCard {
                                uiState.config!!.hosts.forEachIndexed { idx, host ->
                                    InfoRow(stringResource(R.string.network_entry), host)
                                    if (idx < uiState.config!!.hosts.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    ExpressiveSection(stringResource(R.string.network_service_announcement), Icons.Default.Visibility) {
                        ExpressiveInfoCard {
                            val sa = uiState.config!!.serviceAnnouncement
                            if (sa != null) {
                                InfoRow(stringResource(R.string.network_netbios), if (sa.netbios == true) enabled else disabled)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InfoRow(stringResource(R.string.network_mdns), if (sa.mdns == true) enabled else disabled)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InfoRow(stringResource(R.string.network_wsd), if (sa.wsd == true) enabled else disabled)
                            } else {
                                InfoRow(stringResource(R.string.network_status), stringResource(R.string.network_not_available))
                            }
                        }
                    }

                    ExpressiveSection(stringResource(R.string.network_activity_filtering), Icons.Default.Shield) {
                        ExpressiveInfoCard {
                            InfoRow(stringResource(R.string.attr_type), uiState.config!!.activity.type)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_activities), if (uiState.config!!.activity.activities.isEmpty()) stringResource(R.string.common_none) else uiState.config!!.activity.activities.joinToString(", "))
                        }
                    }

                    if (uiState.config!!.hostnameB != null || uiState.config!!.hostnameVirtual != null) {
                        ExpressiveSection(stringResource(R.string.network_ha_hostnames), Icons.Default.Public) {
                            ExpressiveInfoCard {
                                uiState.config!!.hostnameB?.let { InfoRow(stringResource(R.string.network_hostname_b), it) }
                                if (uiState.config!!.hostnameB != null && uiState.config!!.hostnameVirtual != null) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                                uiState.config!!.hostnameVirtual?.let { InfoRow(stringResource(R.string.network_virtual_hostname), it) }
                            }
                        }
                    }

                    ExpressiveSection(stringResource(R.string.network_current_state), Icons.Default.CheckCircle) {
                        ExpressiveInfoCard {
                            val st = uiState.config!!.state
                            InfoRow(stringResource(R.string.network_ipv4_gateway), st.ipv4Gateway ?: notSet)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_ipv6_gateway), st.ipv6Gateway ?: notSet)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_nameserver_n, 1), st.nameserver1 ?: notSet)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_nameserver_n, 2), st.nameserver2 ?: notSet)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(stringResource(R.string.network_nameserver_n, 3), st.nameserver3 ?: notSet)
                            if (st.hosts.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InfoRow(stringResource(R.string.network_hosts), stringResource(R.string.network_hosts_entries, st.hosts.size))
                            }
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
private fun InterfaceCard(iface: String, ipInfo: System.NetworkGeneralSummaryIP) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SettingsEthernet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    iface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))
            ipInfo.ipv4?.forEach { addr ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.protocol_ipv4),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        addr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            ipInfo.ipv6?.forEach { addr ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.protocol_ipv6),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        addr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}