package com.gegaremant.truenasmobile.ui.homepage

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.Shares
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import kotlinx.coroutines.launch

/**
 * The Storage tab: everything this server exposes to the outside world.
 *
 * It used to be a second copy of the Details screen - the same pool card and
 * the same shares card, reading the same ViewModel. That duplication is gone.
 * What stays here is the part that is genuinely about sharing rather than about
 * the box: SMB, NFS and TrueNAS's own web shares.
 *
 * Pools moved to Details, where the storage card already lives, and tapping
 * "Пул хранения" there leads here.
 */
class SharedResourcesViewModel(
    private val manager: TrueNASApiManager
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val smb: List<Shares.SmbShare>,
            val nfs: List<Shares.NfsShare>,
            val web: List<Shares.WebShare>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _state = mutableStateOf<UiState>(UiState.Loading)
    val state: State<UiState> = _state

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val smb = manager.sharing.getSmbSharesWithResult()
            val nfs = manager.sharing.getNfsSharesWithResult()
            val web = manager.sharing.getWebSharesWithResult()

            _state.value = when {
                smb is ApiResult.Error ->
                    UiState.Error(smb.message)
                nfs is ApiResult.Error ->
                    UiState.Error(nfs.message)
                web is ApiResult.Error ->
                    UiState.Error(web.message)
                else -> UiState.Success(
                    smb = (smb as ApiResult.Success).data,
                    nfs = (nfs as ApiResult.Success).data,
                    web = (web as ApiResult.Success).data
                )
            }
        }
    }
}

@Composable
fun SharedResourcesScreen(
    manager: TrueNASApiManager?,
    onNavigateBack: () -> Unit = {},
    onSmbShareClick: (Shares.SmbShare) -> Unit = {},
    onNfsShareClick: (Shares.NfsShare) -> Unit = {}
) {
    if (manager == null) return
    val viewModel: SharedResourcesViewModel = viewModel(
        factory = SharedResourcesViewModelFactory(manager)
    )

    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedScreenHeader(
            title = stringResource(R.string.storage_title),
            subtitle = stringResource(R.string.storage_subtitle),
            isLoading = state is SharedResourcesViewModel.UiState.Loading,
            isRefreshing = false,
            error = (state as? SharedResourcesViewModel.UiState.Error)?.message,
            onRefresh = { viewModel.load() },
            onDismissError = {},
            manager = manager,
            onBackPressed = onNavigateBack
        )

        when (val current = state) {
            is SharedResourcesViewModel.UiState.Loading ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is SharedResourcesViewModel.UiState.Error ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            is SharedResourcesViewModel.UiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShareGroup(
                    titleRes = R.string.shared_smb_title,
                    icon = Icons.Filled.Storage,
                    empty = current.smb.isEmpty()
                ) {
                    current.smb.forEach { share ->
                        ShareRow(
                            name = share.name,
                            path = share.path,
                            enabled = share.enabled,
                            onClick = { onSmbShareClick(share) }
                        )
                    }
                }

                ShareGroup(
                    titleRes = R.string.shared_nfs_title,
                    icon = Icons.Filled.FolderShared,
                    empty = current.nfs.isEmpty()
                ) {
                    current.nfs.forEach { share ->
                        ShareRow(
                            name = share.path,
                            path = share.path,
                            enabled = share.enabled,
                            readOnly = share.ro,
                            onClick = { onNfsShareClick(share) }
                        )
                    }
                }

                ShareGroup(
                    titleRes = R.string.shared_webshare_title,
                    icon = Icons.Filled.Language,
                    empty = current.web.isEmpty()
                ) {
                    current.web.forEach { share ->
                        ShareRow(
                            name = share.path,
                            path = share.path,
                            enabled = share.enabled,
                            onClick = {}
                        )
                    }
                }

                if (current.smb.isEmpty() && current.nfs.isEmpty() && current.web.isEmpty()) {
                    Text(
                        text = stringResource(R.string.shared_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareGroup(
    titleRes: Int,
    icon: ImageVector,
    empty: Boolean,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (empty) {
            Text(
                text = stringResource(R.string.shared_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            content()
        }
    }
}

@Composable
private fun ShareRow(
    name: String,
    path: String,
    enabled: Boolean,
    readOnly: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (path.isNotEmpty() && path != name) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (readOnly) {
            Text(
                text = stringResource(R.string.share_feat_read_only),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = stringResource(
                if (enabled) R.string.common_enabled else R.string.common_disabled
            ),
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

private class SharedResourcesViewModelFactory(
    private val manager: TrueNASApiManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SharedResourcesViewModel(manager) as T
}