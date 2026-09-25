package com.gegaremant.truenasmobile.ui.homepage.instancesettings.systeminformation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.PullToRefreshContent
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.general.ExpressiveSection

@Composable
fun TrueNasConnectScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {}
) {
    val vm: TrueNasConnectViewModel = viewModel(
        factory = TrueNasConnectViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var enabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refresh() }

    LaunchedEffect(uiState.config?.enabled) {
        enabled = uiState.config?.enabled ?: false
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            UnifiedScreenHeader(
                title = stringResource(R.string.tnc_title),
                subtitle = stringResource(R.string.tnc_subtitle),
                isLoading = uiState.isLoading,
                isRefreshing = false,
                error = uiState.error,
                onDismissError = { vm.clearError() },
                manager = manager,
                onBackPressed = onNavigateBack
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.config == null ->
                LoadingScreen(stringResource(R.string.tnc_loading))
            else ->
                PullToRefreshContent(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { vm.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        val config = uiState.config
                        if (config != null) {
                            item {
                                StatusCard(config = config, enabled = enabled, onToggleEnabled = {
                                    vm.updateConfig(System.TNConnectUpdateArgs(enabled = it))
                                })
                            }

                            item {
                                ExpressiveSection(title = stringResource(R.string.tnc_section_connection), icon = Icons.Default.Cloud) {
                                    InfoCard {
                                        InfoRow(stringResource(R.string.attr_status), config.status)
                                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                        InfoRow(stringResource(R.string.tnc_account_url), config.accountServiceBaseUrl!!)
                                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                        InfoRow(stringResource(R.string.tnc_tnc_url), config.tncBaseUrl!!)
                                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                        InfoRow(stringResource(R.string.tnc_heartbeat_url), config.heartbeatUrl!!)
                                    }
                                }
                            }

                            if (config.interfaces.isNotEmpty()) {
                                item {
                                    ExpressiveSection(title = stringResource(R.string.tnc_section_interfaces), icon = Icons.Default.Share) {
                                        InfoCard {
                                            InfoRow(stringResource(R.string.tnc_use_all_interfaces), if (config.useAllInterfaces!!) stringResource(R.string.common_yes) else stringResource(R.string.common_no))
                                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                            InfoRow(stringResource(R.string.tnc_interfaces), config.interfaces.joinToString(", "))
                                            if (config.interfacesIps.isNotEmpty()) {
                                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                                InfoRow(stringResource(R.string.tnc_interface_ips), config.interfacesIps.joinToString(", "))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.tnc_section_registration), icon = Icons.Default.Key) {
                                InfoCard {
                                    Text(
                                        stringResource(R.string.tnc_registration_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = { vm.generateClaimToken() },
                                            enabled = !uiState.isActing,
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.Key, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(stringResource(R.string.tnc_generate_claim), fontWeight = FontWeight.SemiBold)
                                        }
                                        OutlinedButton(
                                            onClick = { vm.loadRegistrationUri() },
                                            enabled = !uiState.isActing,
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(stringResource(R.string.tnc_registration), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    uiState.claimToken?.let {
                                        Spacer(Modifier.height(16.dp))
                                        InfoRow(stringResource(R.string.tnc_claim_token), it)
                                    }
                                    uiState.registrationUri?.let {
                                        Spacer(Modifier.height(8.dp))
                                        InfoRow(stringResource(R.string.tnc_registration_uri), it)
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun StatusCard(config: System.TNCEntry, enabled: Boolean, onToggleEnabled: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (config.enabled) stringResource(R.string.common_enabled) else stringResource(R.string.common_disabled),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    config.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggleEnabled)
        }
    }
}

@Composable
private fun InfoCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
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
            modifier = Modifier.weight(0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
