package com.gegaremant.truenasmobile.ui.homepage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.LoadingScreen
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.homepage.details.ShareType

/**
 * «Хранилище»: пулы и общие папки (shares), данные берёт из [HomeViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    manager: TrueNASApiManager,
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit = {},
    onPoolClick: (System.Pool) -> Unit,
    onNavigateToShareInfo: (ShareType) -> Unit = {},
    onDisksClick: () -> Unit,
    onSearchClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = (uiState as? HomeUiState.Success)?.isRefreshing ?: false

    Column(modifier = Modifier.fillMaxSize()) {
        UnifiedScreenHeader(
            title = stringResource(R.string.storage_title),
            subtitle = when (val state = uiState) {
                is HomeUiState.Success -> {
                    val pools = state.poolDetails.size
                    val poolsText = if (pools == 0) {
                        stringResource(R.string.storage_no_pools)
                    } else {
                        pluralStringResource(R.plurals.storage_pool_count, pools, pools)
                    }
                    val shares = state.smbShares.size + state.nfsShares.size
                    stringResource(
                        R.string.storage_subtitle,
                        poolsText,
                        pluralStringResource(R.plurals.storage_share_count, shares, shares)
                    )
                }
                is HomeUiState.Loading -> stringResource(R.string.storage_loading_data)
                is HomeUiState.Error -> stringResource(R.string.storage_connection_error)
            },
            isLoading = uiState is HomeUiState.Loading,
            isRefreshing = false,
            error = null,
            onDismissError = { viewModel.refresh() },
            manager = manager,
            onNavigateToSettings = onNavigateToSettings,
            onSearchClick = onSearchClick
        )
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() }
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingScreen(stringResource(R.string.storage_loading))
                }
                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.storage_load_failed, state.message),
                            modifier = Modifier.padding(16.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )
                    }
                }
                is HomeUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (state.poolDetails.isNotEmpty()) {
                            state.poolDetails.forEach { pool ->
                                StorageCard(
                                    pool = pool,
                                    modifier = Modifier.padding(bottom = 16.dp),
                                    onClick = { onPoolClick(pool) }
                                )
                            }
                        } else {
                            NoStorageCard(modifier = Modifier.padding(bottom = 16.dp))
                        }

                        SharesCard(
                            smbShares = state.smbShares,
                            nfsShares = state.nfsShares,
                            onSmbShareClick = { share -> onNavigateToShareInfo(ShareType.Smb(share)) },
                            onNfsShareClick = { share -> onNavigateToShareInfo(ShareType.Nfs(share)) }
                        )
                    }
                }
            }
        }
    }
}