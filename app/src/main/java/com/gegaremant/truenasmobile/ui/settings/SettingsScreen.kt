package com.gegaremant.truenasmobile.ui.settings

import android.app.Application
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.BugReport
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.LoggingPrefs
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    manager: TrueNASApiManager?,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToTheme : () -> Unit = {},
    onNavigateToProfile : () -> Unit = {},
    onNavigateToChangePassword : () -> Unit = {},
    onNavigateToLogging: () -> Unit = {},
    onNavigateToPushSettings: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val viewModel : SettingsScreenViewModel = viewModel(
        factory = SettingsScreenViewModel.SettingsViewModelFactory(manager, LocalContext.current.applicationContext as Application)
    )
    val scope = rememberCoroutineScope()
    var isAutoLoginChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAutoLoginChecked = viewModel.isAutoLoginEnabled()
    }

    val uiState by viewModel.uiState.collectAsState()
    var showPassChangeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading, uiState.isAutoLoginSaving, uiState.showAutoLoginDialog) {
        if (!uiState.isLoading && !uiState.isAutoLoginSaving && !uiState.showAutoLoginDialog) {
            isAutoLoginChecked = viewModel.isAutoLoginEnabled()
        }
    }

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            onNavigateToLogin()
            viewModel.handleEvent(SettingsEvent.ClearLogoutSuccess)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // True OLED black in black mode — solid canvas instead of a grey-ish gradient.
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        UnifiedScreenHeader(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle),
            isLoading = uiState.isLoading,
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
            SettingsSection(
                title = stringResource(R.string.settings_section_account),
                items = listOf(
                    SettingItem(
                        icon = Icons.Default.AccountCircle,
                        name = stringResource(R.string.settings_profile),
                        description = stringResource(R.string.settings_profile_desc),
                        onClick = {
                            onNavigateToProfile()
                        }
                    ),
                    SettingItem(
                        icon = Icons.Default.Security,
                        name = stringResource(R.string.settings_password),
                        description = stringResource(R.string.settings_change_password),
                        onClick = {
                            onNavigateToChangePassword()
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(
                title = stringResource(R.string.settings_section_app),
                items = listOf(
                    SettingItem(
                        icon = Icons.Default.Apps,
                        name = stringResource(R.string.settings_theme),
                        description = stringResource(R.string.settings_theme_desc),
                        onClick = { onNavigateToTheme() }
                    ),
                    SettingItem(
                        icon = Icons.Default.Fingerprint,
                        name = stringResource(R.string.settings_biometric),
                        description = stringResource(R.string.settings_biometric_desc),
                        onClick = {},
                        onToggle = { newValue ->
                            viewModel.setBiometricLockEnabled(newValue)
                        },
                        isChecked = uiState.isBiometricLockEnabled,
                    ),
                    SettingItem(
                        icon = Icons.Default.Notifications,
                        name = stringResource(R.string.settings_push),
                        description = stringResource(R.string.settings_push_desc),
                        onClick = { onNavigateToPushSettings() }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(
                title = stringResource(R.string.settings_section_session),
                items = listOf(
                    SettingItem(
                        icon = Icons.Default.Timer,
                        name = stringResource(R.string.settings_auto_login),
                        description = stringResource(R.string.settings_auto_login_desc),
                        onClick = {},
                        isLoading = uiState.isLoading,
                        onToggle = { newValue ->
                            viewModel.handleEvent(SettingsEvent.ToggleAutoLogin(newValue))
                        },
                        isChecked = isAutoLoginChecked,
                    ),
                    SettingItem(
                        icon = Icons.Default.AccountCircle,
                        name = stringResource(R.string.settings_switch_account),
                        description = stringResource(R.string.settings_switch_account_desc),
                        onClick = {
                            onNavigateToLogin()
                        }
                    ),
                    SettingItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        name = stringResource(R.string.settings_sign_out),
                        description = stringResource(R.string.settings_sign_out_desc),
                        onClick = { viewModel.handleEvent(SettingsEvent.SignOut) },
                        isLoading = uiState.isLoggingOut
                    )
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            // "О приложении" and "Лицензии" are gone: there are no third-party
            // licenses to list and one developer does not need an about page.
            // App logging used to hide behind five taps on the about card, and
            // with the card gone it is a normal row - it is how a bug gets
            // diagnosed.
            SettingItem(
                icon = Icons.Default.BugReport,
                name = stringResource(R.string.settings_app_logging),
                description = stringResource(R.string.settings_app_logging_desc),
                onClick = { onNavigateToLogging() }
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.showAutoLoginDialog) {
                AutoLoginConfigDialog(
                    dialogType = uiState.autoLoginDialogType,
                    isSaving = uiState.isAutoLoginSaving,
                    onConfirmToggle = { apiKey, username, userPass ->
                        viewModel.handleEvent(
                            SettingsEvent.SaveAutoLoginCredentials(
                                apiKey = apiKey,
                                username = username,
                                userPass = userPass
                            )
                        )
                    },
                    onCancel = {
                        viewModel.handleEvent(SettingsEvent.DismissAutoLoginDialog)
                    },
                    onClearAutoLogin = {
                        scope.launch {
                            viewModel.clearUseAutoLogin()
                            viewModel.handleEvent(SettingsEvent.DismissAutoLoginDialog)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingItem>
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
            SettingCard(item)
        }
    }
}

@Composable
private fun SettingCard(item: SettingItem) {
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
                        contentDescription = item.name,
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
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.isLoading) stringResource(R.string.common_processing) else item.description,
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

@Composable
fun AutoLoginConfigDialog(
    dialogType: AutoLoginDialogType,
    isSaving: Boolean,
    onConfirmToggle: (apiKey: String?, username: String?, userPass: String?) -> Unit,
    onCancel: () -> Unit,
    onClearAutoLogin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = when (dialogType) {
                    AutoLoginDialogType.OFF_WARNING -> stringResource(R.string.settings_autologin_off_title)
                    AutoLoginDialogType.PROMPT_API_KEY -> stringResource(R.string.settings_autologin_api_title)
                    AutoLoginDialogType.PROMPT_PASSWORD -> stringResource(R.string.settings_autologin_credentials_title)
                }
            )
        },
        text = {
            AutoLoginDialogContent(
                dialogType = dialogType,
                isSaving = isSaving,
                onConfirmToggle = onConfirmToggle,
                onClearAutoLogin = onClearAutoLogin,
                onCancel = onCancel
            )
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun AutoLoginDialogContent(
    dialogType: AutoLoginDialogType,
    isSaving: Boolean,
    onConfirmToggle: (apiKey: String?, username: String?, userPass: String?) -> Unit,
    onClearAutoLogin: () -> Unit,
    onCancel : () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        when (dialogType) {
            AutoLoginDialogType.OFF_WARNING -> {
                Text(stringResource(R.string.settings_autologin_off_warning))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onClearAutoLogin) { Text(stringResource(R.string.settings_disable)) }
                }
            }
            AutoLoginDialogType.PROMPT_API_KEY -> {
                Text(stringResource(R.string.settings_autologin_api_prompt))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.settings_api_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmToggle(apiKey, null, null) },
                        enabled = !isSaving && apiKey.isNotBlank()
                    ) {
                        Text(if (isSaving) stringResource(R.string.settings_saving) else stringResource(R.string.settings_submit))
                    }
                }
            }
            AutoLoginDialogType.PROMPT_PASSWORD -> {
                Text(stringResource(R.string.settings_autologin_password_prompt))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.settings_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.settings_password_field)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmToggle(null, username, password) },
                        enabled = !isSaving && username.isNotBlank() && password.isNotBlank()
                    ) {
                        Text(if (isSaving) stringResource(R.string.settings_saving) else stringResource(R.string.settings_submit))
                    }
                }
            }
        }
    }
}

data class SettingItem(
    val icon: ImageVector,
    val name: String,
    val description: String,
    val onClick: () -> Unit,
    val isLoading: Boolean = false,
    val onToggle: ((Boolean) -> Unit)? = null,
    val isChecked: Boolean? = null
)