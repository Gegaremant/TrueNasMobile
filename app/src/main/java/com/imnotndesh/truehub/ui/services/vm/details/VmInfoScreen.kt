package com.imnotndesh.truehub.ui.services.vm.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.imnotndesh.truehub.data.api.TrueNASApiManager
import com.imnotndesh.truehub.data.models.Vm
import com.imnotndesh.truehub.ui.components.UnifiedScreenHeader

@Composable
fun VmInfoScreen(
    vm: Vm.VmQueryResponse,
    manager: TrueNASApiManager,
    onNavigateBack: () -> Unit
) {
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    Scaffold(
        topBar = {
            UnifiedScreenHeader(
                title = vm.name,
                subtitle = "VM Details",
                isLoading = false,
                isRefreshing = false,
                error = null,
                onDismissError = {},
                manager = manager,
                onBackPressed = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                ) {
                    VmInfoPane(
                        vm = vm,
                        onClose = onNavigateBack,
                        showInlineHeader = false
                    )
                }
            }
        }
    }
}