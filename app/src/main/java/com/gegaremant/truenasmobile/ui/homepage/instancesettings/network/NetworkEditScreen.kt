package com.gegaremant.truenasmobile.ui.homepage.instancesettings.network

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced.ExpressiveInfoCard
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced.ExpressiveSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkEditScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {}
) {
    val vm: NetworkConfigViewModel = viewModel(
        factory = NetworkConfigViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.loadAll() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.toast_network_saved))
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    val config = uiState.config

    var hostname by remember(config) { mutableStateOf(config?.hostname ?: "") }
    var domain by remember(config) { mutableStateOf(config?.domain ?: "") }
    var ipv4Gateway by remember(config) { mutableStateOf(config?.ipv4Gateway ?: "") }
    var ipv6Gateway by remember(config) { mutableStateOf(config?.ipv6Gateway ?: "") }
    var nameserver1 by remember(config) { mutableStateOf(config?.nameserver1 ?: "") }
    var nameserver2 by remember(config) { mutableStateOf(config?.nameserver2 ?: "") }
    var nameserver3 by remember(config) { mutableStateOf(config?.nameserver3 ?: "") }
    var httpProxy by remember(config) { mutableStateOf(config?.httpProxy ?: "") }

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
                            Text(stringResource(R.string.common_discard), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                vm.updateConfig(
                                    System.NetworkConfigurationUpdateArgs(
                                        hostname = hostname.ifBlank { null },
                                        domain = domain.ifBlank { null },
                                        ipv4Gateway = ipv4Gateway.ifBlank { null },
                                        ipv6Gateway = ipv6Gateway.ifBlank { null },
                                        nameserver1 = nameserver1.ifBlank { null },
                                        nameserver2 = nameserver2.ifBlank { null },
                                        nameserver3 = nameserver3.ifBlank { null },
                                        httpProxy = httpProxy.ifBlank { null }
                                    )
                                )
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
                                Text(stringResource(R.string.common_save_changes), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
            UnifiedScreenHeader(
                title = stringResource(R.string.network_edit_title),
                subtitle = stringResource(R.string.network_edit_subtitle),
                isLoading = uiState.isLoading,
                isRefreshing = false,
                error = null,
                onDismissError = {},
                manager = manager,
                onBackPressed = onNavigateBack
            )
            when {
                uiState.isLoading -> LoadingScreen(stringResource(R.string.network_loading))
                config != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            ExpressiveSection(stringResource(R.string.network_identity), Icons.Default.Public) {
                                ExpressiveInfoCard {
                                    OutlinedTextField(
                                        value = hostname,
                                        onValueChange = { hostname = it },
                                        label = { Text(stringResource(R.string.network_hostname)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = domain,
                                        onValueChange = { domain = it },
                                        label = { Text(stringResource(R.string.network_domain)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(stringResource(R.string.network_gateways), Icons.Default.Router) {
                                ExpressiveInfoCard {
                                    OutlinedTextField(
                                        value = ipv4Gateway,
                                        onValueChange = { ipv4Gateway = it },
                                        label = { Text(stringResource(R.string.network_ipv4_gateway)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = ipv6Gateway,
                                        onValueChange = { ipv6Gateway = it },
                                        label = { Text(stringResource(R.string.network_ipv6_gateway)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(stringResource(R.string.network_dns_servers), Icons.Default.Language) {
                                ExpressiveInfoCard {
                                    OutlinedTextField(
                                        value = nameserver1,
                                        onValueChange = { nameserver1 = it },
                                        label = { Text(stringResource(R.string.network_nameserver_n, 1)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = nameserver2,
                                        onValueChange = { nameserver2 = it },
                                        label = { Text(stringResource(R.string.network_nameserver_n, 2)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = nameserver3,
                                        onValueChange = { nameserver3 = it },
                                        label = { Text(stringResource(R.string.network_nameserver_n, 3)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        item {
                            ExpressiveSection(stringResource(R.string.network_http_proxy), Icons.Default.Visibility) {
                                ExpressiveInfoCard {
                                    OutlinedTextField(
                                        value = httpProxy,
                                        onValueChange = { httpProxy = it },
                                        label = { Text(stringResource(R.string.network_http_proxy_url)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        supportingText = { Text(stringResource(R.string.network_leave_blank)) }
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