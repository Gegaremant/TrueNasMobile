package com.gegaremant.truenasmobile.ui.homepage.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.MultiAccountPrefs
import com.gegaremant.truenasmobile.data.models.Shares
import com.gegaremant.truenasmobile.ui.components.ToastManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.DecimalFormat

/**
 * The file list at the bottom of a share screen: what actually lies inside the
 * shared path, via `filesystem.listdir`.
 *
 * The files live on the NAS, not on the phone, so "open it elsewhere" cannot
 * hand over a local file. What does work is an `smb://` URI: the platform and
 * every file manager that speaks SMB will open the very same folder in its own
 * window, which is what the owner asked for.
 */
@Composable
fun ShareFileBrowser(
    manager: TrueNASApiManager,
    initialPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var path by remember { mutableStateOf(initialPath) }
    var entries by remember { mutableStateOf<List<Shares.DirectoryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }

    fun openStoreLink(link: String) {
        showInstallDialog = false
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            ToastManager.showErrorRes(R.string.browser_store_link_failed)
        }
    }

    // The NAS address is needed to build smb://host/share/... links.
    var serverHost by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val host = runCatching {
            val (serverId, _) = MultiAccountPrefs.getLastUsedProfile(context)
                ?: return@runCatching null
            MultiAccountPrefs.getServer(context, serverId)?.serverUrl
                ?.replace("https://", "")
                ?.replace("wss://", "")
                ?.replace("http://", "")
                ?.replace("ws://", "")
                ?.substringBefore("/api")
        }.getOrNull()
        serverHost = host
    }

    LaunchedEffect(path) {
        isLoading = true
        error = null
        when (val listing = manager.sharing.listDirectoryWithResult(path)) {
            is ApiResult.Success -> {
                entries = listing.data.sortedWith(
                    compareByDescending<Shares.DirectoryEntry> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                )
                isLoading = false
            }
            is ApiResult.Error -> {
                entries = emptyList()
                error = listing.message
                isLoading = false
            }
            is ApiResult.Loading -> Unit
        }
    }

    fun openExternally(target: String, isDirectory: Boolean) {
        val host = serverHost
        if (host == null) {
            ToastManager.showErrorRes(R.string.browser_no_server_address)
            return
        }
        // smb://host/mnt/... - the share path as it is on the NAS, so a file
        // manager lands on the same folder instead of the share root.
        val smbPath = if (target.startsWith("/")) target else "/$target"
        val uri = Uri.parse("smb://$host$smbPath")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // The owner hit exactly this: a dead end with a toast. Offer the
            // app that can open smb:// instead of just reporting its absence.
            showInstallDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.browser_files_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    val parent = path.trimEnd('/').substringBeforeLast('/', "")
                    path = if (parent.isEmpty()) "/" else parent
                },
                enabled = path != "/"
            ) {
                Text(stringResource(R.string.browser_up))
            }
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = { openExternally(path, isDirectory = true) },
                enabled = serverHost != null
            ) {
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.browser_open_externally))
            }
        }

        when {
            isLoading -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
            }

            error != null -> Text(
                text = error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            entries.isEmpty() -> Text(
                text = stringResource(R.string.browser_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(entries, key = { it.path }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (entry.isDirectory) {
                                    path = entry.path
                                } else {
                                    openExternally(entry.path, isDirectory = false)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (entry.isDirectory) {
                                Icons.Default.Folder
                            } else {
                                Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = if (entry.isDirectory) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!entry.isDirectory) {
                            Text(
                                text = formatSize(entry.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.UnfoldMore,
                                contentDescription = stringResource(R.string.browser_open_externally),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Нет приложения, умеющего открывать smb://: предлагаем поставить вместо
    // того, чтобы молча сказать «не поддерживается».
    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.browser_install_title)) },
            text = { Text(stringResource(R.string.browser_install_message)) },
            confirmButton = {
                TextButton(onClick = { openStoreLink(FDROID_FILES_URL) }) {
                    Text(stringResource(R.string.browser_install_fdroid))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        openStoreLink(PLAY_FILES_URL)
                    }) {
                        Text(stringResource(R.string.browser_install_play))
                    }
                    TextButton(onClick = { showInstallDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
        )
    }
}

/** Files (Material Files) speaks smb://, and both stores carry it. */
private const val FDROID_FILES_URL = "https://f-droid.org/packages/me.zhanghai.android.files/"
private const val PLAY_FILES_URL =
    "https://play.google.com/store/apps/details?id=me.zhanghai.android.files"

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024.0
        unitIndex++
    }
    return DecimalFormat("#.#").format(size) + " " + units[unitIndex]
}