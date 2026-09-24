package com.gegaremant.truenasmobile.ui.settings.push

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader

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
                title = stringResource(R.string.push_title),
                subtitle = stringResource(R.string.push_subtitle),
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
                            text = stringResource(R.string.push_notifications),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.enabled) {
                                stringResource(
                                    R.string.push_enabled_topic,
                                    state.ntfyTopic.ifBlank { stringResource(R.string.push_none_topic) }
                                )
                            } else {
                                stringResource(R.string.push_disabled_desc)
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
                label = { Text(stringResource(R.string.push_relay_url)) },
                placeholder = { Text("https://relay.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.relayToken,
                onValueChange = viewModel::updateRelayToken,
                label = { Text(stringResource(R.string.push_relay_token)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.ntfyBaseUrl,
                onValueChange = viewModel::updateNtfyBaseUrl,
                label = { Text(stringResource(R.string.push_ntfy_server)) },
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
                    Text(stringResource(R.string.push_test))
                }
                Button(
                    onClick = viewModel::registerAndEnable,
                    enabled = !state.isBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Notifications, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.push_register_enable))
                }
            }

            Button(
                onClick = viewModel::sendTestAlert,
                enabled = !state.isBusy && state.enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.push_send_test_alert))
            }

            OutlinedButton(
                onClick = viewModel::createWebhook,
                enabled = !state.isBusy && state.relayUrl.isNotBlank() && state.relayToken.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.push_create_webhook))
            }

            Text(
                text = state.status ?: stringResource(R.string.push_status_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}