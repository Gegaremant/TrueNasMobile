package com.gegaremant.truenasmobile.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.helpers.AccountSessionRegistry
import com.gegaremant.truenasmobile.data.helpers.MultiAccountPrefs
import com.gegaremant.truenasmobile.data.helpers.PersonalizationManager
import com.gegaremant.truenasmobile.data.models.AccountProfile
import com.gegaremant.truenasmobile.data.models.LoginMethod
import com.gegaremant.truenasmobile.data.models.SavedAccount
import com.gegaremant.truenasmobile.data.models.SavedServer
import com.gegaremant.truenasmobile.ui.background.AnimatedWavyGradientBackground
import com.gegaremant.truenasmobile.ui.setup.ServerConfigBottomSheet
import kotlinx.coroutines.launch

@Composable
fun AccountSwitcherScreen(
    onAccountSelected: (SavedServer, SavedAccount) -> Unit,
    onAddNewAccount: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var profiles by remember { mutableStateOf<List<AccountProfile>>(emptyList()) }
    var savedServers by remember { mutableStateOf<List<SavedServer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showDeleteDialog by remember { mutableStateOf<AccountProfile?>(null) }
    var editingProfile by remember { mutableStateOf<AccountProfile?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showSetupSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val servers = MultiAccountPrefs.getServers(context)
        savedServers = servers
        loadProfiles(context) { loaded ->
            profiles = loaded
            isLoading = false
        }
    }

    fun reload() {
        scope.launch {
            val servers = MultiAccountPrefs.getServers(context)
            savedServers = servers
            loadProfiles(context) { loaded ->
                profiles = loaded
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedWavyGradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Use systemBars to automatically add padding below the status bar
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(horizontal = 24.dp)
            ) {
                // Header
                Text(
                    text = stringResource(R.string.account_select_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Text(
                    text = if (profiles.isEmpty()) {
                        stringResource(R.string.account_none_saved)
                    } else {
                        pluralStringResource(R.plurals.account_saved_count, profiles.size, profiles.size)
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(profiles) { profile ->
                            AccountProfileCard(
                                profile = profile,
                                onClick = { onAccountSelected(profile.server, profile.account) },
                                onEdit = { editingProfile = profile },
                                onDelete = { showDeleteDialog = profile }
                            )
                        }
                    }
                }

                // One connection, one entity: the NAS with the login that
                // belongs to it. The owner asked not to split "a user" from
                // "a machine", so there is one button, not a pair.
                Button(
                    onClick = { showSetupSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.account_add_server),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // «Удалить все» больше не спрятано в выпадающем меню кнопки.
                TextButton(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.account_delete_all_credentials),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Delete a single account ─────────────────────────────
        showDeleteDialog?.let { profile ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text(stringResource(R.string.account_delete_dialog_title)) },
                text = {
                    Text(stringResource(R.string.account_delete_dialog_message, profile.displayName))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                MultiAccountPrefs.deleteAccount(context, profile.account.id)
                                PersonalizationManager.deleteForUser(context, profile.account.id)
                                // The registry may hold a live socket to a server
                                // the user just removed the credentials for.
                                AccountSessionRegistry.forget(profile.account.id)
                                reload()
                                showDeleteDialog = null
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }

        // ── Delete all saved credentials ────────────────────────
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text(stringResource(R.string.account_delete_all_title)) },
                text = {
                    Text(stringResource(R.string.account_delete_all_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAllDialog = false
                            scope.launch {
                                val accounts = MultiAccountPrefs.getAccounts(context)
                                accounts.forEach { account ->
                                    MultiAccountPrefs.deleteAccount(context, account.id)
                                    PersonalizationManager.deleteForUser(context, account.id)
                                    AccountSessionRegistry.forget(account.id)
                                }
                                AccountSessionRegistry.forgetAll()
                                reload()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.account_delete_all), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }

        // ── Edit a saved account ────────────────────────────────
        editingProfile?.let { profile ->
            AccountEditDialog(
                profile = profile,
                onDismiss = { editingProfile = null },
                onSaved = {
                    editingProfile = null
                    reload()
                }
            )
        }

        // ── Server picker for adding an account ────────────────
        // ── Edit a saved account ────────────────────────────────
        editingProfile?.let { profile ->
            AccountEditDialog(
                profile = profile,
                onDismiss = { editingProfile = null },
                onSaved = {
                    editingProfile = null
                    reload()
                }
            )
        }

        // ── Add a new NAS (setup screen) ───────────────────────
        if (showSetupSheet) {
            ServerConfigBottomSheet(
                onDismiss = { showSetupSheet = false },
                onConfigured = { url, insecure ->
                    scope.launch {
                        val server = SavedServer(
                            serverUrl = url,
                            insecure = insecure
                        )
                        MultiAccountPrefs.saveServer(context, server)
                        reload()
                    }
                    showSetupSheet = false
                }
            )
        }
    }
}

@Composable
private fun AccountProfileCard(
    profile: AccountProfile,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Тап по карточке открывает редактор - логин, пароль, никнейм.
            // Переключение осталось отдельной кнопкой справа.
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
            // Аккаунт - это подключение к железке, а не человек: NAS, а не аватар.
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Account Info: the NAS name is the title, the login is the caption.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.server.nickname ?: profile.server.serverUrl,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = profile.account.username,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (profile.account.autoLoginEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.account_auto_login_enabled),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Action Icons (switch, delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Переключиться на этот аккаунт - отдельной кнопкой, потому
                // что тап по карточке теперь редактирует.
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = stringResource(R.string.account_select_account_cd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.account_delete_account_cd),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private suspend fun loadProfiles(
    context: android.content.Context,
    onLoaded: (List<AccountProfile>) -> Unit
) {
    val servers = MultiAccountPrefs.getServers(context)
    val accounts = MultiAccountPrefs.getAccounts(context)

    val profiles = accounts.mapNotNull { account ->
        val server = servers.find { it.id == account.serverId }
        if (server != null) {
            AccountProfile(server, account)
        } else null
    }.sortedByDescending { it.account.lastUsed }

    onLoaded(profiles)
}
