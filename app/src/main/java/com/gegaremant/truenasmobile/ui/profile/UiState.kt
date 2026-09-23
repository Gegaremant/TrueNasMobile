package com.gegaremant.truenasmobile.ui.profile

import com.gegaremant.truenasmobile.data.models.Auth.AuthResponse
import com.gegaremant.truenasmobile.data.models.System


sealed class UiState {
    object Loading : UiState()
    data class Success(
        val user: AuthResponse,
        val system: System.SystemInfo
    ) : UiState()
    data class Error(val message: String) : UiState()
}