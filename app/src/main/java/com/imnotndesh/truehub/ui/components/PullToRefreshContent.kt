package com.imnotndesh.truehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp

/**
 * A pull-to-refresh wrapper around Material3's [PullToRefreshBox] that swaps in the
 * Material3 expressive [LoadingIndicator] (the shape-morphing indicator) as the refresh
 * indicator.
 *
 * The [LoadingIndicator] is only composed while a refresh is actually in flight
 * ([isRefreshing] == true), so it does not animate on top of idle content.
 *
 * @param isRefreshing Whether a refresh operation is being triggered.
 * @param onRefresh Callback invoked when the user pulls down far enough.
 * @param content The scrollable content to wrap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = refreshState,
        modifier = modifier,
        indicator = {
            ShapesRefreshIndicator(
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShapesRefreshIndicator(isRefreshing: Boolean, modifier: Modifier = Modifier) {
    // Only show the loading indicator while a refresh is actually happening, so it
    // does not animate on top of idle content.
    if (isRefreshing) {
        Box(
            modifier = modifier
                .offset(y = 16.dp)
                .zIndex(1f)
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LoadingIndicator(
                modifier = Modifier.size(36.dp).align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
