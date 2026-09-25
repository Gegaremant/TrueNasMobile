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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.PullToRefreshContent
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.general.ExpressiveSection

@Composable
fun TrueCommandScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {}
) {
    val vm: TrueCommandViewModel = viewModel(
        factory = TrueCommandViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var enabled by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }

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
                title = stringResource(R.string.push_title),
                subtitle = stringResource(R.string.truecmd_subtitle),
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
                LoadingScreen(stringResource(R.string.truecmd_loading))
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
                                ExpressiveSection(title = stringResource(R.string.truecmd_section_connection), icon = Icons.Default.Dns) {
                                    InfoCard {
                                        InfoRow(stringResource(R.string.attr_status), config.status)
                                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                        InfoRow(stringResource(R.string.truecmd_status_reason), config.statusReason ?: stringResource(R.string.common_dash))
                                        if (config.remoteUrl != null) {
                                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                            InfoRow(stringResource(R.string.truecmd_remote_url), config.remoteUrl)
                                        }
                                        if (config.remoteIpAddress != null) {
                                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                            InfoRow(stringResource(R.string.truecmd_remote_ip), config.remoteIpAddress)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            ExpressiveSection(title = stringResource(R.string.truecmd_section_config), icon = Icons.Default.Key) {
                                InfoCard {
                                    ToggleRow(
                                        label = stringResource(R.string.truecmd_enable),
                                        description = stringResource(R.string.truecmd_enable_desc),
                                        checked = enabled,
                                        onCheckedChange = { enabled = it }
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = apiKey,
                                        onValueChange = { if (it.length <= 16) apiKey = it },
                                        label = { Text(stringResource(R.string.truecmd_api_key_label)) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { vm.updateConfig(enabled, apiKey.ifBlank { null }) },
                                        enabled = !uiState.isActing,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.truecmd_save_config), fontWeight = FontWeight.SemiBold)
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
private fun ToggleRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
