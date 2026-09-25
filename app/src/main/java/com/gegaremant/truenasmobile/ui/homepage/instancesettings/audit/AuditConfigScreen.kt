package com.gegaremant.truenasmobile.ui.homepage.instancesettings.audit

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced.ExpressiveInfoCard
import com.gegaremant.truenasmobile.ui.homepage.instancesettings.advanced.ExpressiveSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditConfigScreen(
    manager: TrueNASApiManager,
    viewModel: AuditConfigViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current


    var retention by remember { mutableStateOf("") }
    var reservation by remember { mutableStateOf("") }
    var quota by remember { mutableStateOf("") }
    var fillWarning by remember { mutableStateOf("") }
    var fillCritical by remember { mutableStateOf("") }


    LaunchedEffect(uiState.config) {
        uiState.config?.let { cfg ->
            retention = cfg.retention.toString()
            reservation = cfg.reservation.toString()
            quota = cfg.quota.toString()
            fillWarning = cfg.quotaFillWarning.toString()
            fillCritical = cfg.quotaFillCritical.toString()
        }
    }


    LaunchedEffect(uiState.saveResult) {
        when (val result = uiState.saveResult) {
            is AuditSaveResult.Success -> {
                snackbarHostState.showSnackbar(context.getString(R.string.toast_audit_saved))
            }
            is AuditSaveResult.Error -> {
                snackbarHostState.showSnackbar(context.getString(R.string.audit_error, result.message))
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            UnifiedScreenHeader(
                title = stringResource(R.string.audit_title),
                subtitle = stringResource(R.string.audit_subtitle),
                isLoading = uiState.isLoading && uiState.config == null,
                isRefreshing = false,
                error = null,
                onDismissError = {},
                manager = manager,
                onBackPressed = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {

            if (uiState.config != null && !uiState.isLoading) {
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
                                viewModel.saveAuditConfig(
                                    retention = retention.toIntOrNull(),
                                    reservation = reservation.toIntOrNull(),
                                    quota = quota.toIntOrNull(),
                                    quotaFillWarning = fillWarning.toIntOrNull(),
                                    quotaFillCritical = fillCritical.toIntOrNull()
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
                                Text(stringResource(R.string.common_save_changes), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.config == null -> LoadingScreen(stringResource(R.string.audit_loading))
            uiState.config != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    val space = uiState.config!!.space
                    if (space != null) {
                        item {
                            ExpressiveSection(title = stringResource(R.string.audit_section_zfs_space), icon = Icons.Default.SdStorage) {
                                ExpressiveInfoCard {
                                    InfoRow(stringResource(R.string.audit_used), formatBytes(space.used))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    InfoRow(stringResource(R.string.audit_used_by_dataset), formatBytes(space.usedByDataset))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    InfoRow(stringResource(R.string.audit_used_by_reservation), formatBytes(space.usedByReservation))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    InfoRow(stringResource(R.string.audit_used_by_snapshots), formatBytes(space.usedBySnapshots))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    InfoRow(stringResource(R.string.audit_available), formatBytes(space.available))
                                }
                            }
                        }
                    }


                    item {
                        ExpressiveSection(title = stringResource(R.string.audit_section_remote_logging), icon = Icons.Default.Cloud) {
                            ExpressiveInfoCard {
                                val cfg = uiState.config!!
                                InfoRow(stringResource(R.string.audit_remote_logging_enabled), if (cfg.remoteLoggingEnabled) stringResource(R.string.common_yes) else stringResource(R.string.common_no))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                val svcCount = cfg.enabledServices.middleware.size + cfg.enabledServices.smb.size + cfg.enabledServices.sudo.size
                                InfoRow(stringResource(R.string.audit_enabled_services), stringResource(R.string.audit_services_configured, svcCount))
                            }
                        }
                    }


                    item {
                        ExpressiveSection(title = stringResource(R.string.audit_section_settings), icon = Icons.Default.Tune) {
                            ExpressiveInfoCard {
                                EditableField(
                                    value = retention,
                                    onValueChange = { retention = it },
                                    label = stringResource(R.string.audit_retention_days),
                                    icon = Icons.Default.Schedule,
                                    supportingText = "1 – 30"
                                )
                                Spacer(Modifier.height(12.dp))
                                EditableField(
                                    value = reservation,
                                    onValueChange = { reservation = it },
                                    label = stringResource(R.string.audit_reservation_gib),
                                    icon = Icons.Default.Dataset,
                                    supportingText = "0 – 100"
                                )
                                Spacer(Modifier.height(12.dp))
                                EditableField(
                                    value = quota,
                                    onValueChange = { quota = it },
                                    label = stringResource(R.string.audit_quota_gib),
                                    icon = Icons.Default.SdStorage,
                                    supportingText = "0 – 100"
                                )
                                Spacer(Modifier.height(12.dp))
                                EditableField(
                                    value = fillWarning,
                                    onValueChange = { fillWarning = it },
                                    label = stringResource(R.string.audit_quota_warning),
                                    icon = Icons.Default.Warning,
                                    supportingText = "5 – 80"
                                )
                                Spacer(Modifier.height(12.dp))
                                EditableField(
                                    value = fillCritical,
                                    onValueChange = { fillCritical = it },
                                    label = stringResource(R.string.audit_quota_critical),
                                    icon = Icons.Default.Warning,
                                    supportingText = "50 – 95"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    supportingText: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        supportingText = { Text(supportingText) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1.shl(30) -> "%.1f GiB".format(bytes.toDouble() / (1.shl(30)))
        bytes >= 1.shl(20) -> "%.1f MiB".format(bytes.toDouble() / (1.shl(20)))
        bytes >= 1.shl(10) -> "%.1f KiB".format(bytes.toDouble() / (1.shl(10)))
        else -> "$bytes B"
    }
}