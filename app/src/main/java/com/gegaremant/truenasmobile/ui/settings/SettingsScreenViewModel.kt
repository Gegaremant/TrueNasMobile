package com.gegaremant.truenasmobile.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.EncryptedPrefs
import com.gegaremant.truenasmobile.data.helpers.MultiAccountPrefs
import com.gegaremant.truenasmobile.data.helpers.dataStore
import com.gegaremant.truenasmobile.data.security.BiometricLockPrefs
import com.gegaremant.truenasmobile.ui.components.ToastManager
import androidx.biometric.BiometricManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val isChangingPassword: Boolean = false,
    val isLoading: Boolean = false,
    val showAutoLoginDialog: Boolean = false,
    val autoLoginDialogType: AutoLoginDialogType = AutoLoginDialogType.OFF_WARNING,
    val isAutoLoginSaving: Boolean = false,
    val error: String? = null,
    val isChangePassSuccess: Boolean = false,
    val isBiometricLockEnabled: Boolean = false
)
enum class AutoLoginDialogType {
    OFF_WARNING,
    PROMPT_API_KEY,
    PROMPT_PASSWORD
}

sealed class SettingsEvent {
    object Logout : SettingsEvent()
    object SignOut : SettingsEvent()
    data class ChangePassword(val oldPassword : String, val newPassword: String) : SettingsEvent()
    object ClearLogoutSuccess : SettingsEvent()
    object ClearError : SettingsEvent()
    data class ToggleAutoLogin(val newValue: Boolean) : SettingsEvent()
    object DismissAutoLoginDialog : SettingsEvent()
    data class SaveAutoLoginCredentials(val apiKey: String?, val username: String?, val userPass: String?) : SettingsEvent()
}

class SettingsScreenViewModel(
    private val manager: TrueNASApiManager?,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBiometricLockEnabled = BiometricLockPrefs.isEnabled(application.dataStore)
            )
        }
    }

    suspend fun isBiometricLockEnabled(): Boolean {
        return BiometricLockPrefs.isEnabled(application.dataStore)
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val canAuthenticate = BiometricManager.from(application).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                    ToastManager.showErrorRes(R.string.toast_biometric_need_lock)
                    return@launch
                }
            }
            BiometricLockPrefs.setEnabled(application.dataStore, enabled)
            _uiState.value = _uiState.value.copy(isBiometricLockEnabled = enabled)
            ToastManager.showSuccessRes(
                if (enabled) R.string.toast_biometric_enabled else R.string.toast_biometric_disabled
            )
        }
    }

    fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.Logout -> performLogout()
            is SettingsEvent.SignOut -> performSignOut()
            is SettingsEvent.ClearLogoutSuccess -> {
                _uiState.value = _uiState.value.copy(logoutSuccess = false)
            }
            is SettingsEvent.ChangePassword -> changePassword(event.newPassword,event.oldPassword)
            is SettingsEvent.ClearError -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
            is SettingsEvent.ToggleAutoLogin -> handleAutoLoginToggle(event.newValue)
            is SettingsEvent.DismissAutoLoginDialog -> {
                _uiState.value = _uiState.value.copy(showAutoLoginDialog = false)
            }
            is SettingsEvent.SaveAutoLoginCredentials -> saveAutoLoginCredentials(
                event.apiKey, event.username, event.userPass
            )
        }
    }
    private fun handleAutoLoginToggle(newValue: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            if (newValue) {
                val (serverId, accountId) = MultiAccountPrefs.getLastUsedProfile(application)?: Pair(null,null)

                if (serverId != null && accountId != null) {
                    val account = MultiAccountPrefs.getAccount(application, accountId)

                    if (account != null) {
                        val (cred1, cred2) = MultiAccountPrefs.getAccountCredentials(
                            application,
                            accountId,
                            account.loginMethod
                        )

                        val hasCredentials = when (account.loginMethod) {
                            com.gegaremant.truenasmobile.data.models.LoginMethod.API_KEY -> cred1 != null
                            com.gegaremant.truenasmobile.data.models.LoginMethod.PASSWORD,
                            com.gegaremant.truenasmobile.data.models.LoginMethod.TOTP -> cred1 != null && cred2 != null
                        }

                        if (hasCredentials) {
                            val updatedAccount = account.copy(autoLoginEnabled = true)
                            MultiAccountPrefs.saveAccount(application, updatedAccount)
                            ToastManager.showSuccessRes(R.string.toast_autologin_enabled)
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        } else {
                            // Prompt for credentials
                            val dialogType = when (account.loginMethod) {
                                com.gegaremant.truenasmobile.data.models.LoginMethod.API_KEY -> AutoLoginDialogType.PROMPT_API_KEY
                                com.gegaremant.truenasmobile.data.models.LoginMethod.PASSWORD,
                                com.gegaremant.truenasmobile.data.models.LoginMethod.TOTP -> AutoLoginDialogType.PROMPT_PASSWORD
                            }
                            _uiState.value = _uiState.value.copy(
                                showAutoLoginDialog = true,
                                autoLoginDialogType = dialogType,
                                isLoading = false
                            )
                        }
                    } else {
                        ToastManager.showErrorRes(R.string.toast_current_account_not_found)
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                } else {
                    ToastManager.showErrorRes(R.string.toast_no_active_session)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    showAutoLoginDialog = true,
                    autoLoginDialogType = AutoLoginDialogType.OFF_WARNING,
                    isLoading = false
                )
            }
        }
    }

    private fun saveAutoLoginCredentials(apiKey: String?, username: String?, userPass: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAutoLoginSaving = true)

            try {
                val (serverId, accountId) = MultiAccountPrefs.getLastUsedProfile(application)?: Pair(null,null)


                if (serverId != null && accountId != null) {
                    val account = MultiAccountPrefs.getAccount(application, accountId)

                    if (account != null) {
                        MultiAccountPrefs.saveAccountCredentials(
                            context = application,
                            accountId = accountId,
                            loginMethod = account.loginMethod,
                            apiKey = apiKey,
                            username = username,
                            password = userPass
                        )

                        // Enable auto-login
                        val updatedAccount = account.copy(autoLoginEnabled = true)
                        MultiAccountPrefs.saveAccount(application, updatedAccount)

                        _uiState.value = _uiState.value.copy(
                            showAutoLoginDialog = false,
                            isAutoLoginSaving = false
                        )
                        ToastManager.showSuccessRes(R.string.toast_credentials_saved_autologin)
                    } else {
                        throw Exception("Account not found")
                    }
                } else {
                    throw Exception("No active session")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isAutoLoginSaving = false)
                ToastManager.showErrorRes(R.string.toast_save_credentials_failed, e.message.orEmpty())
            }
        }
    }

    private fun performSignOut() {
        _uiState.value = _uiState.value.copy(isLoggingOut = true, error = null)

        viewModelScope.launch {
            try {
                val (_, accountId) = MultiAccountPrefs.getLastUsedProfile(application)?: Pair(null,null)
                MultiAccountPrefs.clearCurrentSession(application)
                if (accountId != null) {
                    MultiAccountPrefs.deleteAccount(application, accountId)
                }

                // Disconnect manager
                manager?.disconnect()

                _uiState.value = _uiState.value.copy(
                    isLoggingOut = false,
                    logoutSuccess = true,
                    error = null
                )

                ToastManager.showSuccessRes(R.string.toast_signed_out)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoggingOut = false,
                    logoutSuccess = false,
                    error = e.message ?: application.getString(R.string.toast_sign_out_failed)
                )
                ToastManager.showErrorRes(R.string.toast_sign_out_failed_pattern, e.message.orEmpty())
            }
        }
    }

    private fun performLogout() {
        _uiState.value = _uiState.value.copy(isLoggingOut = true, error = null)

        viewModelScope.launch {
            try {
                manager?.disconnect()

                _uiState.value = _uiState.value.copy(
                    isLoggingOut = false,
                    logoutSuccess = true,
                    error = null
                )

                ToastManager.showSuccessRes(R.string.toast_logged_out)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoggingOut = false,
                    logoutSuccess = false,
                    error = e.message ?: application.getString(R.string.toast_logout_failed)
                )
                ToastManager.showErrorRes(R.string.toast_logout_failed_pattern, e.message.orEmpty())
            }
        }
    }
    private fun changePassword(newPassword :String,oldPassword : String){
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val username = EncryptedPrefs.getUsername(application)
            if (username == null) {
                ToastManager.showErrorRes(R.string.toast_username_not_found)
                return@launch
            }
            try {
                val result = manager?.user?.changeUserPasswordWithResult(username,oldPassword,newPassword)
                when (result){
                    is ApiResult.Error -> {
                        ToastManager.showErrorRes(R.string.toast_password_change_failed_pattern, result.message.orEmpty())
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isChangePassSuccess = false,
                            error = result.message
                        )
                    }
                    is ApiResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, isChangePassSuccess = true, error = null)
                        ToastManager.showSuccessRes(R.string.toast_password_changed)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        ToastManager.showErrorRes(R.string.toast_password_change_failed)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isChangePassSuccess = false,
                    error = e.message ?: application.getString(R.string.toast_password_change_failed)
                )
                ToastManager.showErrorRes(R.string.toast_password_change_failed_pattern, e.message.orEmpty())
            }
        }
    }

    suspend fun clearUseAutoLogin() {
        val (_, accountId) = MultiAccountPrefs.getLastUsedProfile(application) ?: Pair(null, null)

        if (accountId != null) {
            val account = MultiAccountPrefs.getAccount(application, accountId)
            if (account != null) {
                val updatedAccount = account.copy(autoLoginEnabled = false)
                MultiAccountPrefs.saveAccount(application, updatedAccount)

                MultiAccountPrefs.clearAccountCredentials(application, accountId)

                ToastManager.showSuccessRes(R.string.toast_autologin_disabled)
            }
        }
    }

    class SettingsViewModelFactory(
        private val manager: TrueNASApiManager?,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsScreenViewModel::class.java)) {
                return SettingsScreenViewModel(manager, application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    suspend fun isAutoLoginEnabled(): Boolean {
        val (_, accountId) = MultiAccountPrefs.getLastUsedProfile(application)?: Pair(null,null)
        return if (accountId != null) {
            MultiAccountPrefs.getAccount(application, accountId)?.autoLoginEnabled ?: false
        } else {
            false
        }
    }
}
