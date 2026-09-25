package com.gegaremant.truenasmobile.ui.homepage.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.JobState
import com.gegaremant.truenasmobile.data.helpers.JobTracker
import com.gegaremant.truenasmobile.data.models.Storage
import com.gegaremant.truenasmobile.data.models.System.Pool
import com.gegaremant.truenasmobile.ui.components.ToastManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PoolDetailsUiState {
    object Loading : PoolDetailsUiState()
    data class Success(
        val pool: Pool,
        val scrubTasks: List<Storage.PoolScrubQueryResponse> = emptyList(),
        val isRefreshing: Boolean = false,
        val jobStates : Map<String, JobState> = emptyMap()
    ) : PoolDetailsUiState()
    data class Error(val message: String) : PoolDetailsUiState()
}

class PoolDetailsViewModel(
    private val apiManager: TrueNASApiManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PoolDetailsUiState>(PoolDetailsUiState.Loading)
    val uiState: StateFlow<PoolDetailsUiState> = _uiState.asStateFlow()
    private val _jobStates = MutableStateFlow<Map<String, JobState>>(emptyMap())
    private var currentPoolId: Int? = null

    init {
        val pool = PoolDataHolder.currentPool
        if (pool != null) {
            _uiState.value = PoolDetailsUiState.Success(pool)
            currentPoolId = pool.id
            PoolDataHolder.currentPool = null
        } else {
            _uiState.value = PoolDetailsUiState.Error(ToastManager.resolveString(R.string.pooldetails_load_failed))
        }
        viewModelScope.launch {
            _jobStates.collect { jobs ->
                _uiState.update {
                    if (it is PoolDetailsUiState.Success) {
                        it.copy(jobStates = jobs)
                    } else {
                        it
                    }
                }
            }
        }

        getScrubTasks()
    }
    fun refresh(){
        getPoolDetails()
        getScrubTasks()
    }
    fun getPoolDetails() {
        val id = currentPoolId ?: run {
            _uiState.value = PoolDetailsUiState.Error(ToastManager.resolveString(R.string.pooldetails_id_not_found))
            return
        }

        val currentScrubTasks = (_uiState.value as? PoolDetailsUiState.Success)?.scrubTasks ?: emptyList()

        viewModelScope.launch {
            _uiState.update {
                if (it is PoolDetailsUiState.Success) it.copy(isRefreshing = true) else it
            }

            try {
                val result = apiManager.system.getPoolsWithResult()
                when (result) {
                    is ApiResult.Success -> {
                        val newPool = result.data.find { it.id == id }
                        if (newPool != null) {
                            _uiState.value = PoolDetailsUiState.Success(
                                pool = newPool,
                                scrubTasks = currentScrubTasks,
                                isRefreshing = false
                            )
                            currentPoolId = newPool.id
                        } else {
                            _uiState.value = PoolDetailsUiState.Error(ToastManager.resolveString(R.string.pooldetails_refresh_failed))
                        }
                    }
                    is ApiResult.Error -> {
                        _uiState.value = PoolDetailsUiState.Error(result.message)
                    }
                    ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.value = PoolDetailsUiState.Error(e.message ?: ToastManager.resolveString(R.string.pooldetails_error_occurred))
            }
        }
    }
    fun getScrubTasks(){
        viewModelScope.launch {
            _uiState.update {
                if (it is PoolDetailsUiState.Success) it.copy(isRefreshing = true) else it
            }
            try {
                val res = apiManager.storage.getScrubTasks()
                when (res){
                    is ApiResult.Loading -> {}
                    is ApiResult.Error -> {
                        _uiState.value = PoolDetailsUiState.Error(res.message)
                    }
                    is ApiResult.Success -> {
                        _uiState.update {
                            if (it is PoolDetailsUiState.Success) {
                                it.copy(scrubTasks = res.data, isRefreshing = false)
                            } else {
                                it
                            }
                        }
                    }
                }
            }catch (e: Exception){
                _uiState.value = PoolDetailsUiState.Error(e.message ?: ToastManager.resolveString(R.string.pooldetails_error_occurred))
            }
        }
    }
    fun createScrubTask(args: Storage.UpdatePoolScrubDetails) {
        viewModelScope.launch {
            _uiState.update {
                if (it is PoolDetailsUiState.Success) it.copy(isRefreshing = true) else it
            }
            try {
                val result = apiManager.storage.createScrubTask(args)
                when (result) {
                    is ApiResult.Success -> {
                        getScrubTasks()
                    }
                    is ApiResult.Error -> {
                        _uiState.value = PoolDetailsUiState.Error(result.message)
                    }
                    ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.value = PoolDetailsUiState.Error(e.message ?: ToastManager.resolveString(R.string.pooldetails_error_occurred))
            }
        }
    }
    fun updateScrubTask(id: Int, data: Storage.UpdatePoolScrubDetails) {
        val args = Storage.UpdatePoolScrubArgs(id, data)
        viewModelScope.launch {
            _uiState.update {if (it is PoolDetailsUiState.Success) it.copy(isRefreshing = true) else it
            }
            try {
                val result = apiManager.storage.updateScrubTask(args)
                when (result) {
                    is ApiResult.Success -> {
                        getScrubTasks()
                    }
                    is ApiResult.Error -> {
                        _uiState.value = PoolDetailsUiState.Error(result.message)
                    }
                    ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.value = PoolDetailsUiState.Error(e.message ?: ToastManager.resolveString(R.string.pooldetails_error_occurred))
            }
        }
    }
    fun runScrubTask(args :Storage.RunPoolScrubArgs){
        viewModelScope.launch {
            _uiState.update {
                if (it is PoolDetailsUiState.Success) it.copy(isRefreshing = true) else it
            }
            try {
                val result = apiManager.storage.runScrubTask(args)
                when (result) {
                    is ApiResult.Success -> {
                        ToastManager.showToast(ToastManager.resolveString(R.string.pooldetails_scrub_started))
                        getScrubTasks()
                    }
                    is ApiResult.Error -> {
                        _uiState.update {
                            if (it is PoolDetailsUiState.Success) it.copy(isRefreshing = false) else it
                        }
                        ToastManager.showToast(ToastManager.resolveString(R.string.pooldetails_scrub_started))
                    }
                    ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.value = PoolDetailsUiState.Error(e.message ?: ToastManager.resolveString(R.string.pooldetails_error_occurred))
            }
        }
    }
    fun switchScrubTaskState(poolName: String, scrubId: Long, action: Storage.PoolScrubAction) {
        val args = Storage.TakeActionOnPoolScrubArgs(name = poolName, action = action)
        viewModelScope.launch {
            try {
                val result = apiManager.storage.setScrubState(args)
                when (result) {
                    is ApiResult.Success -> {
                        val jobId = result.data
                        ToastManager.showToast(ToastManager.resolveString(R.string.pooldetails_action_initiated))
                        JobTracker.pollJobStatus(
                            jobId = jobId,
                            manager = apiManager,
                            jobsStateFlow = _jobStates,
                            trackingKey = "scrub_$scrubId",
                            onComplete = { finalState ->
                                if (finalState == "SUCCESS") {
                                    getScrubTasks()
                                }else if (finalState == "FAILED"){
                                    _jobStates.update {
                                        it - "scrub_$scrubId"
                                    }
                                    ToastManager.showToast(ToastManager.resolveString(R.string.pool_scrub_switch_failed))
                                }
                            }
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = PoolDetailsUiState.Error(result.message)
                    }
                    ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.value = PoolDetailsUiState.Error(e.message ?: ToastManager.resolveString(R.string.pooldetails_error_occurred))
            }
        }
    }


    fun deleteScrubTask(id: Int) {
        viewModelScope.launch {
            _uiState.update {
                if (it is PoolDetailsUiState.Success) it.copy(isRefreshing = true) else it
            }
            try {
                val result = apiManager.storage.deleteScrubTask(id)
                when (result) {
                    is ApiResult.Success -> {
                        if (result.data){
                            getScrubTasks()
                        }else{
                            ToastManager.showToast(ToastManager.resolveString(R.string.pool_scrub_delete_failed))
                        }
                    }
                    is ApiResult.Error -> {
                        _uiState.value = PoolDetailsUiState.Error(result.message)
                    }
                    ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.value = PoolDetailsUiState.Error(e.message ?: ToastManager.resolveString(R.string.pooldetails_error_occurred))
            }
        }
    }



    class PoolDetailsViewModelFactory(
        private val apiManager: TrueNASApiManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PoolDetailsViewModel::class.java)) {
                return PoolDetailsViewModel(apiManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}