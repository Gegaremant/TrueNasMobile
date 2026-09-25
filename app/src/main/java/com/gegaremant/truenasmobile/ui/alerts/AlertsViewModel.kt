package com.gegaremant.truenasmobile.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.components.ToastManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlertsUiState(
    val alerts: List<System.AlertResponse> = emptyList(),
    val categories: List<System.AlertCategoriesResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0
)

class AlertsViewModel(private val manager: TrueNASApiManager) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                error = null
            )

            when (val result = manager.system.listAlertsWithResult()) {
                is ApiResult.Success -> {
                    loadCategories()

                    val unreadCount = result.data.count { !it.dismissed }
                    _uiState.value = _uiState.value.copy(
                        alerts = result.data,
                        isLoading = false,
                        isRefreshing = false,
                        unreadCount = unreadCount,
                        error = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.message
                    )
                }
                ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        error = null
                    )
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = manager.system.listCategoriesWithResult()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        categories = result.data
                    )
                }
                is ApiResult.Error -> {
                    // Categories are optional, don't update error state
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun dismissAlert(uuid: String) {
        viewModelScope.launch {
            val result = try {
                // Use the real API result: a failed dismiss must not be
                // reported to the user as a success.
                when (val apiResult = manager.system.dismissAlertWithResult(uuid)) {
                    is ApiResult.Success -> ApiResult.Success(Unit)
                    is ApiResult.Error -> ApiResult.Error(
                        apiResult.message ?: ToastManager.resolveString(R.string.toast_alert_dismiss_failed)
                    )
                    is ApiResult.Loading -> ApiResult.Error(
                        ToastManager.resolveString(R.string.common_processing)
                    )
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: ToastManager.resolveString(R.string.toast_alert_dismiss_failed))
            }

            when (result) {
                is ApiResult.Success -> {
                    loadAlerts(isRefresh = true)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message
                    )
                    ToastManager.showError(result.message)
                }
                is ApiResult.Loading ->{}
            }
        }
    }

    fun restoreAlert(uuid: String) {
        viewModelScope.launch {
            val result = try {
                // Same as dismissAlert: never fabricate success.
                when (val apiResult = manager.system.restoreAlertWithResult(uuid)) {
                    is ApiResult.Success -> ApiResult.Success(Unit)
                    is ApiResult.Error -> ApiResult.Error(
                        apiResult.message ?: ToastManager.resolveString(R.string.toast_alert_restore_failed)
                    )
                    is ApiResult.Loading -> ApiResult.Error(
                        ToastManager.resolveString(R.string.common_processing)
                    )
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: ToastManager.resolveString(R.string.toast_alert_restore_failed))
            }

            when (result) {
                is ApiResult.Success -> {
                    ToastManager.showSuccessRes(R.string.toast_alert_restored)
                    loadAlerts(isRefresh = true)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message
                    )
                    ToastManager.showError(result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadAlerts(isRefresh = true)
    }

    class AlertsViewModelFactory(
        private val manager: TrueNASApiManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
                return AlertsViewModel(manager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}