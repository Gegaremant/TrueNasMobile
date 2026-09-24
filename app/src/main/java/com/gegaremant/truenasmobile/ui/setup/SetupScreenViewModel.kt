package com.gegaremant.truenasmobile.ui.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.TrueNASClient
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.Prefs
import com.gegaremant.truenasmobile.data.models.Config.ClientConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class SetupUiState(
    val serverUrl: String = "",
    val insecure: Boolean = false,
    val isConfiguring: Boolean = false,
    val urlValidation: UrlValidation = UrlValidation(false, emptyList(), emptyList()),
    val connectionError: String? = null,
    val setupComplete: Boolean = false
)

sealed class SetupEvent {
    data class UpdateServerUrl(val url: String) : SetupEvent()
    data class UpdateInsecure(val insecure: Boolean) : SetupEvent()
    data class Configure(val context: Context) : SetupEvent()
    object ResetError : SetupEvent()
    object ResetSetupComplete : SetupEvent()
}

class SetupScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private var tempApiManager: TrueNASApiManager? = null

    fun handleEvent(event: SetupEvent) {
        when (event) {
            is SetupEvent.UpdateServerUrl -> {
                val validation = validateUrl(event.url)
                _uiState.value = _uiState.value.copy(
                    serverUrl = event.url,
                    urlValidation = validation
                )
            }
            is SetupEvent.UpdateInsecure -> {
                _uiState.value = _uiState.value.copy(insecure = event.insecure)
            }
            is SetupEvent.Configure -> {
                configureServer(event.context)
            }
            is SetupEvent.ResetError -> {
                _uiState.value = _uiState.value.copy(connectionError = null)
            }
            is SetupEvent.ResetSetupComplete -> {
                _uiState.value = _uiState.value.copy(setupComplete = false)
            }
        }
    }

    private fun configureServer(context: Context) {
        val currentState = _uiState.value

        // Validate before proceeding
        if (!currentState.urlValidation.isValid || currentState.serverUrl.isEmpty()) {
            _uiState.value = currentState.copy(
                connectionError = context.getString(R.string.setup_enter_valid_url)
            )
            return
        }

        _uiState.value = currentState.copy(
            isConfiguring = true,
            connectionError = null
        )

        viewModelScope.launch {
            try {
                val formattedUrl = formatUrl(currentState.serverUrl)

                withTimeout(10000L) {
                    val config = ClientConfig(
                        serverUrl = formattedUrl,
                        insecure = currentState.insecure,
                        connectionTimeoutMs = 8000,
                        enablePing = true,
                        enableDebugLogging = false
                    )

                    val client = TrueNASClient(config)
                    tempApiManager = TrueNASApiManager(client,context.applicationContext)

                    // Test connection
                    tempApiManager?.connect()

                    if (tempApiManager?.isConnected() == true) {
                        // Connection successful - save configuration
                        Prefs.save(context, formattedUrl, currentState.insecure)

                        _uiState.value = currentState.copy(
                            isConfiguring = false,
                            setupComplete = true
                        )
                    } else {
                        throw Exception(context.getString(R.string.setup_unable_establish_connection))
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = _uiState.value.copy(
                    isConfiguring = false,
                    connectionError = context.getString(R.string.setup_connection_timeout)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConfiguring = false,
                    connectionError = context.getString(
                        R.string.setup_connection_failed,
                        e.message ?: context.getString(R.string.common_unknown_error)
                    )
                )
            } finally {
                // Clean up temporary resources
                tempApiManager?.disconnect()
                tempApiManager = null
            }
        }
    }

    fun formatUrl(url: String): String {
        var formattedUrl = url.trim()

        // Remove trailing slash if present
        if (formattedUrl.endsWith("/")) {
            formattedUrl = formattedUrl.dropLast(1)
        }

        // Add /api/current if not present
        if (!formattedUrl.contains("/api/current")) {
            formattedUrl += "/api/current"
        }

        return formattedUrl
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            tempApiManager?.disconnect()
        }
        tempApiManager = null
    }
}