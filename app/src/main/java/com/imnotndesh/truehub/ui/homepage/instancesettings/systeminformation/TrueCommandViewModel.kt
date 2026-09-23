package com.imnotndesh.truehub.ui.homepage.instancesettings.systeminformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imnotndesh.truehub.data.ApiResult
import com.imnotndesh.truehub.data.api.TrueNASApiManager
import com.imnotndesh.truehub.data.models.System
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrueCommandUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isActing: Boolean = false,
    val config: System.TruecommandEntry? = null,
    val error: String? = null,
    val actionMessage: String? = null
)

class TrueCommandViewModel(private val manager: TrueNASApiManager) : ViewModel() {

    private val _uiState = MutableStateFlow(TrueCommandUiState())
    val uiState: StateFlow<TrueCommandUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                if (forceRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            when (val result = manager.system.getTruecommandConfig()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, config = result.data)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = result.message)
                    }
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun updateConfig(enabled: Boolean, apiKey: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isActing = true, error = null) }
            when (val result = manager.system.updateTruecommand(
                System.TruecommandUpdateArgs(enabled = enabled, apiKey = apiKey)
            )) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActing = false,
                            config = result.data,
                            actionMessage = "TrueCommand updated"
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isActing = false, error = result.message) }
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    class ViewModelFactory(
        private val manager: TrueNASApiManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrueCommandViewModel(manager) as T
        }
    }
}
