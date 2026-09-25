package com.gegaremant.truenasmobile.ui.homepage.instancesettings.users

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveModerator
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: Int,
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: UserSettingsViewModel = viewModel(
        factory = UserSettingsViewModel.UserSettingsViewModelFactory(manager),
        key = userId.toString()
    )
    val uiState by viewModel.detailState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    var editUsername by remember { mutableStateOf("") }
    var editFullName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editHome by remember { mutableStateOf("") }
    var editShell by remember { mutableStateOf("") }
    var editLocked by remember { mutableStateOf(false) }
    var editPasswordDisabled by remember { mutableStateOf(false) }
    var showTwoFactorSheet by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }
    var changePasswordValue by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()


    fun enterEditMode(user: System.UserCreateUpdateResult) {
        editUsername = user.username
        editFullName = user.full_name
        editEmail = user.email ?: ""
        editHome = user.home
        editShell = user.shell
        editLocked = user.locked
        editPasswordDisabled = user.password_disabled
        isEditing = true
    }

    LaunchedEffect(Unit) { viewModel.loadUserDetail(userId) }
    LaunchedEffect(uiState.deleteResult) {
        if (uiState.deleteResult == true) onNavigateBack()
    }

    val current = uiState.user
    var showDeleteDialog by remember { mutableStateOf(false) }
    val yes = stringResource(R.string.common_yes)
    val no = stringResource(R.string.common_no)
    val enabled = stringResource(R.string.common_enabled)
    val disabled = stringResource(R.string.common_disabled)
    val configured = stringResource(R.string.generalsettings_configured)
    val notConfigured = stringResource(R.string.generalsettings_not_configured)
    val dash = stringResource(R.string.common_dash)
    val present = stringResource(R.string.user_present)

    val statusColor by animateColorAsState(
        targetValue = when {
            current?.locked == true -> Color(0xFFC62828)
            current?.builtin == true -> Color(0xFF1565C0)
            current?.roles?.contains("FULL_ADMIN") == true -> Color(0xFF6A1B9A)
            else -> Color(0xFF2E7D32)
        },
        label = "statusColor"
    )

    val statusLabel = when {
        current?.locked == true -> stringResource(R.string.user_status_locked)
        current?.builtin == true -> stringResource(R.string.user_status_system)
        current?.roles?.contains("FULL_ADMIN") == true -> stringResource(R.string.user_status_admin)
        else -> stringResource(R.string.common_active)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
    ) {
        UnifiedScreenHeader(
            title = current?.username ?: stringResource(R.string.user_detail_title_fallback),
            subtitle = current?.full_name ?: stringResource(R.string.user_detail_subtitle_fallback),
            isLoading = uiState.isLoading,
            isRefreshing = false,
            error = uiState.error,
            onDismissError = { viewModel.clearDetailError() },
            manager = manager,
            onBackPressed = onNavigateBack
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> LoadingScreen(stringResource(R.string.user_detail_loading))
                current != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status header card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person, null,
                                    tint = statusColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    current.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    current.full_name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (current.email != null) {
                                    Text(
                                        current.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (isEditing) {
                        EditUserForm(
                            username = editUsername, onUsernameChange = { editUsername = it },
                            fullName = editFullName, onFullNameChange = { editFullName = it },
                            email = editEmail, onEmailChange = { editEmail = it },
                            home = editHome, onHomeChange = { editHome = it },
                            shell = editShell, onShellChange = { editShell = it },
                            locked = editLocked, onLockedChange = { editLocked = it },
                            passwordDisabled = editPasswordDisabled,
                            onPasswordDisabledChange = { editPasswordDisabled = it }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.common_cancel)) }
                            Button(
                                onClick = {
                                    viewModel.updateUser(
                                        userId,
                                        System.UserUpdate(
                                            username = editUsername.takeIf { it != current.username },
                                            full_name = editFullName.takeIf { it != current.full_name },
                                            email = editEmail.takeIf { it != (current.email ?: "") },
                                            home = editHome.takeIf { it != current.home },
                                            shell = editShell.takeIf { it != current.shell },
                                            locked = editLocked.takeIf { it != current.locked },
                                            password_disabled = editPasswordDisabled
                                                .takeIf { it != current.password_disabled }
                                        )
                                    )
                                },
                                enabled = !uiState.isUpdating && editUsername.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isUpdating) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.common_save))
                            }
                        }
                    } else {
                        // Account Info section
                        SectionCard(
                            title = stringResource(R.string.user_section_account_info),
                            icon = Icons.Default.Person
                        ) {
                            DetailRow("UID", current.uid.toString())
                            DetailRow(stringResource(R.string.common_username), current.username)
                            DetailRow(stringResource(R.string.user_full_name), current.full_name)
                            DetailRow(stringResource(R.string.attr_email), current.email ?: dash)
                            DetailRow(stringResource(R.string.user_home_dir), current.home)
                            DetailRow(stringResource(R.string.user_shell), current.shell)
                        }

                        // Status & Permissions section
                        SectionCard(
                            title = stringResource(R.string.user_section_status_permissions),
                            icon = Icons.Default.Security
                        ) {
                            DetailRow(stringResource(R.string.user_builtin), if (current.builtin) yes else no)
                            DetailRow(stringResource(R.string.user_local), if (current.local) yes else no)
                            DetailRow(stringResource(R.string.user_locked), if (current.locked) yes else no)
                            DetailRow(stringResource(R.string.user_smb_access), if (current.smb) enabled else disabled)
                            DetailRow(stringResource(R.string.common_password), if (current.password_disabled) disabled else enabled)
                            DetailRow(stringResource(R.string.user_ssh_password), if (current.ssh_password_enabled) enabled else disabled)
                            DetailRow(stringResource(R.string.user_2fa), if (current.twofactor_auth_configured) configured else notConfigured)
                            DetailRow(stringResource(R.string.user_change_required), if (current.password_change_required) yes else no)
                        }

                        // Roles section
                        SectionCard(
                            title = stringResource(R.string.user_section_roles_groups),
                            icon = Icons.Default.AdminPanelSettings
                        ) {
                            DetailRow(stringResource(R.string.user_roles), current.roles.joinToString(", ").ifEmpty { dash })
                            DetailRow(
                                stringResource(R.string.user_primary_group),
                                current.group?.bsdgrp_group ?: dash
                            )
                            DetailRow(
                                "GID",
                                current.group?.bsdgrp_gid?.toString() ?: dash
                            )
                            DetailRow(stringResource(R.string.user_groups), current.groups.joinToString(", ").ifEmpty { dash })
                        }

                        // Password section
                        SectionCard(
                            title = stringResource(R.string.user_section_password),
                            icon = Icons.Default.Lock
                        ) {
                            DetailRow(stringResource(R.string.user_password_age), current.password_age?.toString() ?: dash)
                            DetailRow(stringResource(R.string.user_smb_hash), if (current.smbhash != null && current.smbhash != "") present else dash)
                            DetailRow("SID", current.sid ?: dash)
                        }
                        if (!current.builtin && !current.password_disabled) {
                            OutlinedButton(
                                onClick = { showPasswordSheet = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Key, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.user_change_password))
                            }
                        }
                        if (current.twofactor_auth_configured) {
                            SectionCard(
                                title = stringResource(R.string.user_section_2fa),
                                icon = Icons.Default.Security
                            ) {
                                Text(
                                    stringResource(R.string.user_2fa_configured),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showTwoFactorSheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.user_manage_2fa))
                                }
                            }
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { enterEditMode(current) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.common_edit))
                            }
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !uiState.isDeleting && !current.builtin,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isDeleting) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.common_delete))
                            }
                        }
                    }
                }
            }
        }
        if (showTwoFactorSheet && current != null) {
            ModalBottomSheet(
                onDismissRequest = { showTwoFactorSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        stringResource(R.string.user_2fa_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.user_2fa_dialog_user, current.username),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()

                    Button(
                        onClick = {
                            viewModel.renew2faSecret(current.username)
                        },
                        enabled = !uiState.isRenewing2fa,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isRenewing2fa) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.user_2fa_renew_secret))
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.unset2faSecret(current.username)
                        },
                        enabled = !uiState.isUnsetting2fa,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isUnsetting2fa) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.RemoveModerator, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.user_2fa_unset_secret))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showPasswordSheet && current != null) {
            ModalBottomSheet(
                onDismissRequest = { showPasswordSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        stringResource(R.string.user_password_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.user_password_dialog_message, current.username),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()

                    OutlinedTextField(
                        value = changePasswordValue,
                        onValueChange = { changePasswordValue = it },
                        label = { Text(stringResource(R.string.user_new_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.changeUserPassword(current.username, changePasswordValue)
                        },
                        enabled = !uiState.isChangingPassword && changePasswordValue.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isChangingPassword) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Key, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.user_set_password))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Handle results
        LaunchedEffect(uiState.passwordChangeResult) {
            if (uiState.passwordChangeResult == true) {
                showPasswordSheet = false
                changePasswordValue = ""
                viewModel.clearPasswordChangeResult()
            }
        }
        LaunchedEffect(uiState.unset2faResult) {
            if (uiState.unset2faResult == true) {
                showTwoFactorSheet = false
                viewModel.clearUnset2faResult()
                viewModel.loadUserDetail(userId)
            }
        }
        LaunchedEffect(uiState.renew2faResult) {
            if (uiState.renew2faResult != null) {
                showTwoFactorSheet = false
                viewModel.clearRenew2faResult()
            }
        }

    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.user_delete_title)) },
            text = {
                Text(stringResource(R.string.user_delete_message, current?.username ?: ""))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteUser(userId)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            content()
        }
    }
}

@Composable
private fun EditUserForm(
    username: String, onUsernameChange: (String) -> Unit,
    fullName: String, onFullNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    home: String, onHomeChange: (String) -> Unit,
    shell: String, onShellChange: (String) -> Unit,
    locked: Boolean, onLockedChange: (Boolean) -> Unit,
    passwordDisabled: Boolean, onPasswordDisabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Edit, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.user_edit_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            OutlinedTextField(
                value = username, onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.common_username)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = fullName, onValueChange = onFullNameChange,
                label = { Text(stringResource(R.string.user_full_name)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = email, onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.attr_email)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = home, onValueChange = onHomeChange,
                label = { Text(stringResource(R.string.user_home_dir)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = shell, onValueChange = onShellChange,
                label = { Text(stringResource(R.string.user_shell)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.user_locked), style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f))
                Switch(checked = locked, onCheckedChange = onLockedChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.user_password_disabled), style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f))
                Switch(
                    checked = passwordDisabled,
                    onCheckedChange = onPasswordDisabledChange
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
