package com.imnotndesh.truehub.ui.homepage.instancesettings.alertservice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imnotndesh.truehub.data.ApiResult
import com.imnotndesh.truehub.data.api.TrueNASApiManager
import com.imnotndesh.truehub.data.models.Alerts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertServicesListUiState(
    val services: List<Alerts.AlertServiceEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class AlertServicesListViewModel(private val manager: TrueNASApiManager) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertServicesListUiState())
    val uiState: StateFlow<AlertServicesListUiState> = _uiState.asStateFlow()

    init {
        loadServices(isInitial = true)
    }

    fun refresh() = loadServices(isInitial = false)

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadServices(isInitial: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (isInitial) it.copy(isLoading = true, error = null)
                else it.copy(isRefreshing = true, error = null)
            }
            when (val result = manager.alertsService.listAlertServicesWithResult()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            services = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.message
                        )
                    }
                }
                is ApiResult.Loading -> { /* no-op */ }
            }
        }
    }

    class AlertServicesListViewModelFactory(private val manager: TrueNASApiManager) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AlertServicesListViewModel(manager) as T
    }
}
