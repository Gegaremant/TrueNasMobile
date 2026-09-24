package com.gegaremant.truenasmobile.ui.settings.push

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.Alerts
import com.gegaremant.truenasmobile.data.push.PushConfig
import com.gegaremant.truenasmobile.data.push.PushE2E
import com.gegaremant.truenasmobile.data.push.PushPrefs
import com.gegaremant.truenasmobile.data.push.PushService
import com.gegaremant.truenasmobile.data.push.RelayClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class PushUiState(
    val enabled: Boolean = false,
    val relayUrl: String = "",
    val relayToken: String = "",
    val ntfyBaseUrl: String = "https://ntfy.sh",
    val ntfyTopic: String = "",
    val deviceId: String = "",
    val isBusy: Boolean = false,
    val status: String? = null
)

/**
 * Wizard behind Settings → Push Notifications.
 *
 * It registers this device's E2E key with the relay, keeps the config in
 * [PushPrefs], starts/stops [PushService], opens a test path and can create
 * the matching "Webhook" alert service on TrueNAS in one tap instead of
 * forcing users through the web UI.
 */
class PushSettingsViewModel(
    application: Application,
    private val manager: TrueNASApiManager?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PushUiState())
    val uiState: StateFlow<PushUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val config = PushPrefs.getConfig(application)
            _uiState.value = PushUiState(
                enabled = config.enabled,
                relayUrl = config.relayUrl,
                relayToken = config.relayToken,
                ntfyBaseUrl = config.ntfyBaseUrl,
                ntfyTopic = config.ntfyTopic,
                deviceId = config.deviceId
            )
        }
    }

    fun updateRelayUrl(value: String) = _uiState.update { it.copy(relayUrl = value) }
    fun updateRelayToken(value: String) = _uiState.update { it.copy(relayToken = value) }
    fun updateNtfyBaseUrl(value: String) = _uiState.update { it.copy(ntfyBaseUrl = value) }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(enabled = enabled, isBusy = true, status = null) }
            if (enabled) {
                val ok = registerDevice()
                if (!ok) {
                    _uiState.update { it.copy(enabled = false, isBusy = false) }
                    PushPrefs.setEnabled(getApplication(), false)
                    return@launch
                }
                PushService.start(getApplication())
            } else {
                PushService.stop(getApplication())
                PushPrefs.setEnabled(getApplication(), false)
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun testRelay() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, status = getApplication<Application>().getString(R.string.push_testing)) }
            val state = _uiState.value
            val result = RelayClient().healthz(state.relayUrl)
            _uiState.update {
                when (result) {
                    is RelayClient.RelayResult.Success ->
                        it.copy(isBusy = false, status = getApplication<Application>().getString(R.string.push_relay_reachable))
                    is RelayClient.RelayResult.Error ->
                        it.copy(isBusy = false, status = getApplication<Application>().getString(R.string.push_relay_unreachable, result.message))
                }
            }
        }
    }

    fun registerAndEnable() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, status = getApplication<Application>().getString(R.string.push_registering)) }
            val ok = registerDevice()
            if (ok) {
                PushPrefs.setEnabled(getApplication(), true)
                PushService.start(getApplication())
                _uiState.update { it.copy(isBusy = false, status = getApplication<Application>().getString(R.string.push_registered)) }
            } else {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    /** Registers (or refreshes) this device against the relay. Returns success. */
    private suspend fun registerDevice(): Boolean {
        val state = _uiState.value
        val relayUrl = state.relayUrl.trim().trimEnd('/')
        val token = state.relayToken.trim()
        if (relayUrl.isBlank() || token.isBlank()) {
            _uiState.update { it.copy(status = getApplication<Application>().getString(R.string.push_required)) }
            return false
        }

        val topic = state.ntfyTopic.ifBlank {
            "th-" + UUID.randomUUID().toString().replace("-", "").take(20)
        }
        val pubkey = try {
            PushE2E.publicKeyPointB64(getApplication())
        } catch (e: Exception) {
            _uiState.update { it.copy(status = getApplication<Application>().getString(R.string.push_e2e_failed, e.message)) }
            return false
        }

        val registration = JSONObject()
            .put("name", deviceName())
            .put("pubkey", pubkey)
            .put("transport", "ntfy")
            .put("pushToken", topic)

        return when (val result = RelayClient().register(relayUrl, token, registration)) {
            is RelayClient.RelayResult.Success -> {
                val deviceId = result.body.optString("deviceId")
                val ntfyBaseUrl = state.ntfyBaseUrl.trim().ifBlank { "https://ntfy.sh" }
                PushPrefs.saveConfig(
                    getApplication(),
                    PushConfig(
                        enabled = true,
                        relayUrl = relayUrl,
                        relayToken = token,
                        transport = "ntfy",
                        ntfyBaseUrl = ntfyBaseUrl,
                        ntfyTopic = topic,
                        deviceId = deviceId
                    )
                )
                _uiState.update {
                    it.copy(
                        enabled = true,
                        ntfyTopic = topic,
                        deviceId = deviceId,
                        status = getApplication<Application>().getString(R.string.push_registered_id, deviceId)
                    )
                }
                true
            }
            is RelayClient.RelayResult.Error -> {
                _uiState.update { it.copy(status = getApplication<Application>().getString(R.string.push_registration_failed, result.message)) }
                false
            }
        }
    }

    fun sendTestAlert() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, status = getApplication<Application>().getString(R.string.push_sending_test)) }
            val state = _uiState.value
            val payload = JSONObject()
                .put("level", "WARNING")
                .put("title", getApplication<Application>().getString(R.string.push_test_alert_title))
                .put("text", getApplication<Application>().getString(R.string.push_test_alert_text))
                .put("uuid", UUID.randomUUID().toString())
            val result = RelayClient().sendTest(state.relayUrl, state.relayToken, payload)
            _uiState.update {
                when (result) {
                    is RelayClient.RelayResult.Success ->
                        it.copy(
                            isBusy = false,
                            status = getApplication<Application>().getString(
                                R.string.push_delivered,
                                result.body.optInt("delivered", 0)
                            )
                        )
                    is RelayClient.RelayResult.Error ->
                        it.copy(isBusy = false, status = getApplication<Application>().getString(R.string.push_test_failed, result.message))
                }
            }
        }
    }

    fun createWebhook() {
        val apiManager = manager
        if (apiManager == null) {
            _uiState.update { it.copy(status = getApplication<Application>().getString(R.string.push_not_connected)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, status = getApplication<Application>().getString(R.string.push_creating_webhook)) }
            val state = _uiState.value
            val relayUrl = state.relayUrl.trim().trimEnd('/')
            val token = state.relayToken.trim()
            if (relayUrl.isBlank() || token.isBlank()) {
                _uiState.update { it.copy(isBusy = false, status = getApplication<Application>().getString(R.string.push_required)) }
                return@launch
            }

            val create = Alerts.AlertServiceCreate(
                name = getApplication<Application>().getString(R.string.push_service_name),
                attributes = Alerts.AlertServiceAttributes(
                    type = "Webhook",
                    webhook_url = "$relayUrl/api/send",
                    webhook_http_method = "POST",
                    webhook_http_auth = "NONE",
                    webhook_headers = listOf(
                        Alerts.AlertServiceHeader("Authorization", "Bearer $token")
                    )
                ),
                level = Alerts.AlertLevels.WARNING,
                enabled = true
            )

            when (val result = apiManager.alertsService.createAlertServiceWithResult(create)) {
                is ApiResult.Success ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            status = getApplication<Application>().getString(R.string.push_webhook_created, result.data.name)
                        )
                    }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isBusy = false, status = getApplication<Application>().getString(R.string.push_webhook_failed, result.message)) }
                else ->
                    _uiState.update { it.copy(isBusy = false, status = getApplication<Application>().getString(R.string.push_webhook_unexpected)) }
            }
        }
    }

    private fun deviceName(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    class PushSettingsViewModelFactory(
        private val application: Application,
        private val manager: TrueNASApiManager?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PushSettingsViewModel(application, manager) as T
    }
}