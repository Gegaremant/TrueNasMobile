package com.imnotndesh.truehub.ui.settings.push

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imnotndesh.truehub.data.api.TrueNASApiManager
import com.imnotndesh.truehub.ui.components.UnifiedScreenHeader

@Composable
fun PushSettingsScreen(
    manager: TrueNASApiManager?,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: PushSettingsViewModel = viewModel(
        factory = PushSettingsViewModel.PushSettingsViewModelFactory(
            application = context.applicationContext as Application,
            manager = manager
        )
    )
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            UnifiedScreenHeader(
                title = "Push Notifications",
                subtitle = "TrueNAS alerts straight to this device",
                isLoading = false,
                isRefreshing = false,
                error = null,
                onDismissError = {},
                manager = manager!!,
                onBackPressed = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(snackbarData = it) } },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Push notifications",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.enabled) {
                                "Enabled — topic ${state.ntfyTopic.ifBlank { "(none)" }}"
                            } else {
                                "Relay alerts to your phone. No Google services required (ntfy)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { viewModel.setEnabled(it) },
                        enabled = !state.isBusy
                    )
                }
            }

            OutlinedTextField(
                value = state.relayUrl,
                onValueChange = viewModel::updateRelayUrl,
                label = { Text("Relay URL") },
                placeholder = { Text("https://relay.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.relayToken,
                onValueChange = viewModel::updateRelayToken,
                label = { Text("Relay token") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.ntfyBaseUrl,
                onValueChange = viewModel::updateNtfyBaseUrl,
                label = { Text("ntfy server") },
                placeholder = { Text("https://ntfy.sh") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::testRelay,
                    enabled = !state.isBusy && state.relayUrl.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Notifications, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test")
                }
                Button(
                    onClick = viewModel::registerAndEnable,
                    enabled = !state.isBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Notifications, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Register & Enable")
                }
            }

            Button(
                onClick = viewModel::sendTestAlert,
                enabled = !state.isBusy && state.enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.width(8.dp))
                Text("Send test alert")
            }

            OutlinedButton(
                onClick = viewModel::createWebhook,
                enabled = !state.isBusy && state.relayUrl.isNotBlank() && state.relayToken.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create webhook on TrueNAS")
            }

            Text(
                text = state.status ?: "Step 1: point TrueNAS at your relay webhook. Step 2: register this device. Then press “Send test alert”.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}