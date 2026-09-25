package com.gegaremant.truenasmobile.ui.homepage.instancesettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader

/**
 * Lists TrueNAS-instance-specific settings and configuration, separate from
 * the app-level [SettingsScreen]. Mirrors its visual language (same header,
 * same SettingsSection/SettingCard pattern) but only covers server-side
 * configuration areas.
 *
 * All onClick callbacks are currently empty placeholders — wire each one to
 * its real destination screen as it's built.
 */
@Composable
fun InstanceConfigScreen(
    manager: TrueNASApiManager?,
    onNavigateBack: () -> Unit = {},
    onNavigateToGeneralSettings: () -> Unit = {},
    onNavigateToAdvancedSettings: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToBoot: () -> Unit = {},
    onNavigateToServices: () -> Unit = {},
    onNavigateToAlertSettings: () -> Unit = {},
    onNavigateToApiKeys: () -> Unit = {},
    onNavigateToGeneralSystemSettings: () -> Unit = {},
    onNavigateToAuditConfig: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToSystemInformation: () -> Unit = {},
    onNavigateToTrueNasConnect: () -> Unit = {},
    onNavigateToTrueCommand: () -> Unit = {},
    onNavigateToAppImageManagement: () -> Unit = {},

) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // True OLED black in black mode — solid canvas instead of a grey-ish gradient.
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedScreenHeader(
            title = stringResource(R.string.instconfig_title),
            subtitle = stringResource(R.string.instconfig_subtitle),
            isLoading = false,
            isRefreshing = false,
            error = null,
            onDismissError = {},
            manager = manager!!,
            onBackPressed = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            InstanceConfigSection(
                title = stringResource(R.string.instconfig_section_core),
                items = listOf(
                    InstanceConfigItem(
                        icon = Icons.Default.Tune,
                        nameRes = R.string.instconfig_general,
                        descRes = R.string.instconfig_general_desc,
                        onClick = onNavigateToGeneralSystemSettings
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.Bolt,
                        nameRes = R.string.instconfig_advanced,
                        descRes = R.string.instconfig_advanced_desc,
                        onClick = onNavigateToAdvancedSettings
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.Wifi,
                        nameRes = R.string.instconfig_network,
                        descRes = R.string.instconfig_network_desc,
                        onClick = onNavigateToNetwork
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.PowerSettingsNew,
                        nameRes = R.string.instconfig_boot,
                        descRes = R.string.instconfig_boot_desc,
                        onClick = onNavigateToBoot
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.Dns,
                        nameRes = R.string.instconfig_services,
                        descRes = R.string.instconfig_services_desc,
                        onClick = onNavigateToServices
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.People,
                        nameRes = R.string.instconfig_users,
                        descRes = R.string.instconfig_users_desc,
                        onClick = onNavigateToUsers
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.Key,
                        nameRes = R.string.instconfig_api_keys,
                        descRes = R.string.instconfig_api_keys_desc,
                        onClick = onNavigateToApiKeys
                    ),

                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            InstanceConfigSection(
                title = stringResource(R.string.instconfig_section_apps),
                items = listOf(
                    InstanceConfigItem(
                        icon = Icons.Default.Image,
                        nameRes = R.string.instconfig_app_images,
                        descRes = R.string.instconfig_app_images_desc,
                        onClick = onNavigateToAppImageManagement
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            InstanceConfigSection(
                title = stringResource(R.string.instconfig_section_monitoring),
                items = listOf(
                    InstanceConfigItem(
                        icon = Icons.Default.NotificationsActive,
                        nameRes = R.string.instconfig_alert_settings,
                        descRes = R.string.instconfig_alert_settings_desc,
                        onClick = onNavigateToAlertSettings
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.FactCheck,
                        nameRes = R.string.instconfig_audit_config,
                        descRes = R.string.instconfig_audit_config_desc,
                        onClick = onNavigateToAuditConfig
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.FactCheck,
                        nameRes = R.string.instconfig_audit_logs,
                        descRes = R.string.instconfig_audit_logs_desc,
                        onClick = onNavigateToAuditLogs
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            InstanceConfigSection(
                title = stringResource(R.string.instconfig_section_sysinfo),
                items = listOf(
                    InstanceConfigItem(
                        icon = Icons.Default.Cloud,
                        nameRes = R.string.instconfig_truenas_connect,
                        descRes = R.string.instconfig_truenas_connect_desc,
                        onClick = onNavigateToTrueNasConnect
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.Insights,
                        nameRes = R.string.instconfig_truecommand,
                        descRes = R.string.instconfig_truecommand_desc,
                        onClick = onNavigateToTrueCommand
                    ),
                    InstanceConfigItem(
                        icon = Icons.Default.Info,
                        nameRes = R.string.instconfig_system_information,
                        descRes = R.string.instconfig_system_information_desc,
                        onClick = onNavigateToSystemInformation
                    )
                )
            )
        }
    }
}

@Composable
private fun InstanceConfigSection(
    title: String,
    items: List<InstanceConfigItem>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items.forEach { item ->
            InstanceConfigCard(item)
        }
    }
}

@Composable
private fun InstanceConfigCard(item: InstanceConfigItem) {
    val showSwitch = item.onToggle != null && item.isChecked != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !item.isLoading && !showSwitch) { item.onClick() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (item.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.nameRes),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(item.nameRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.isLoading) stringResource(R.string.common_processing) else stringResource(item.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!item.isLoading) {
                if (showSwitch) {
                    Switch(
                        checked = item.isChecked,
                        onCheckedChange = item.onToggle,
                        modifier = Modifier.clickable(enabled = false) { }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class InstanceConfigItem(
    val icon: ImageVector,
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    val onClick: () -> Unit,
    val isLoading: Boolean = false,
    val onToggle: ((Boolean) -> Unit)? = null,
    val isChecked: Boolean? = null
)