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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DecimalFormat
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.PullToRefreshContent
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.general.ExpressiveSection
import com.gegaremant.truenasmobile.ui.services.apps.details.appdetails.AppDataHolder

@Composable
fun HardwareInformationScreen(
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {},
    onNavigateToDisks: () -> Unit = {}
) {
    val vm: HardwareInformationViewModel = viewModel(
        factory = HardwareInformationViewModel.ViewModelFactory(manager)
    )
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            UnifiedScreenHeader(
                title = stringResource(R.string.sysinfo_title_hardware),
                subtitle = stringResource(R.string.sysinfo_subtitle_hardware),
                isLoading = uiState.isLoading,
                isRefreshing = false,
                error = uiState.error,
                onDismissError = { vm.clearError() },
                manager = manager,
                onBackPressed = onNavigateBack
            )
        }
    ) { innerPadding ->
        PullToRefreshContent(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val info = uiState.systemInfo

                if (info != null) {
                    item {
                        ExpressiveSection(title = stringResource(R.string.sysinfo_section_cpu), icon = Icons.Default.Build) {
                            InfoCard {
                                InfoRow(stringResource(R.string.sysinfo_model), info.model.ifEmpty { stringResource(R.string.common_dash) })
                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                InfoRow(stringResource(R.string.sysinfo_physical_cores), info.physical_cores?.toString() ?: stringResource(R.string.common_dash))
                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                InfoRow(stringResource(R.string.sysinfo_total_cores), info.cores.toInt().toString())
                            }
                        }
                    }

                    item {
                        ExpressiveSection(title = stringResource(R.string.sysinfo_section_memory), icon = Icons.Default.Memory) {
                            InfoCard {
                                InfoRow(
                                    stringResource(R.string.sysinfo_total_memory),
                                    "${DecimalFormat("#.#").format(info.physmem / (1024.0 * 1024.0 * 1024.0))} GB"
                                )
                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                InfoRow(stringResource(R.string.sysinfo_ecc), if (info.ecc_memory) stringResource(R.string.common_yes) else stringResource(R.string.common_no))
                            }
                        }
                    }
                }

                item {
                    ExpressiveSection(title = stringResource(R.string.sysinfo_section_storage), icon = Icons.Default.Storage) {
                        InfoCard {
                            InfoRow(stringResource(R.string.sysinfo_disks), uiState.diskCount.toString())
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            InfoRow(stringResource(R.string.sysinfo_pools), if (uiState.poolsHealthy == null) stringResource(R.string.common_dash) else if (uiState.poolsHealthy == true) stringResource(R.string.sysinfo_healthy) else stringResource(R.string.sysinfo_issues))
                            Spacer(Modifier.height(16.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    AppDataHolder.disks = uiState.disks
                                    onNavigateToDisks()
                                },
                                enabled = uiState.disks.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Storage, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.padding(start = 6.dp))
                                Text(stringResource(R.string.sysinfo_view_disks), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                item {
                    ExpressiveSection(title = stringResource(R.string.sysinfo_section_platform), icon = Icons.Default.Hardware) {
                        InfoCard {
                            InfoRow(stringResource(R.string.sysinfo_chassis), uiState.chassisHardware ?: stringResource(R.string.common_dash))
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            InfoRow(stringResource(R.string.sysinfo_ix_hardware), booleanLabel(uiState.isIxHardware))
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            InfoRow(stringResource(R.string.sysinfo_host_id), uiState.hostId ?: stringResource(R.string.common_dash))
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            InfoRow(stringResource(R.string.sysinfo_boot_id), uiState.bootId ?: stringResource(R.string.common_dash))
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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

@Composable
private fun booleanLabel(value: Boolean?): String = when (value) {
    true -> stringResource(R.string.common_yes)
    false -> stringResource(R.string.common_no)
    null -> stringResource(R.string.common_dash)
}
