package com.gegaremant.truenasmobile.ui.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ToastType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class ToastData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val duration: Long = 3000L,
    val actionText: String? = null,
    val onActionClick: (() -> Unit)? = null
)

object ToastManager {
    var currentToast by mutableStateOf<ToastData?>(null)
        private set

    private val scope = CoroutineScope(Dispatchers.Main)

    @Volatile
    private var appContext: Context? = null

    /** Must be called once from the Application class before showing toasts. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun resolveText(@StringRes resId: Int, vararg formatArgs: Any): String {
        val context = appContext
            ?: throw IllegalStateException("ToastManager.init(context) must be called before using string resources")
        return context.getString(resId, *formatArgs)
    }

    fun showToast(
        message: String,
        type: ToastType = ToastType.INFO,
        duration: Long = 3000L,
        actionText: String? = null,
        onActionClick: (() -> Unit)? = null
    ) {
        val toast = ToastData(
            message = message,
            type = type,
            duration = duration,
            actionText = actionText,
            onActionClick = onActionClick
        )

        currentToast = toast

        scope.launch {
            delay(duration)
            if (currentToast?.id == toast.id) {
                currentToast = null
            }
        }
    }

    fun dismissToast() {
        currentToast = null
    }

    // Convenience methods
    fun showSuccess(message: String, duration: Long = 3000L) {
        showToast(message, ToastType.SUCCESS, duration)
    }

    fun showError(message: String, duration: Long = 4000L) {
        showToast(message, ToastType.ERROR, duration)
    }

    fun showWarning(message: String, duration: Long = 3500L) {
        showToast(message, ToastType.WARNING, duration)
    }

    fun showInfo(message: String, duration: Long = 3000L) {
        showToast(message, ToastType.INFO, duration)
    }

    // Resource-based convenience methods (resolved via init(context))
    fun showSuccessRes(@StringRes resId: Int, vararg formatArgs: Any, duration: Long = 3000L) {
        showToast(resolveText(resId, *formatArgs), ToastType.SUCCESS, duration)
    }

    fun showErrorRes(@StringRes resId: Int, vararg formatArgs: Any, duration: Long = 4000L) {
        showToast(resolveText(resId, *formatArgs), ToastType.ERROR, duration)
    }

    fun showWarningRes(@StringRes resId: Int, vararg formatArgs: Any, duration: Long = 3500L) {
        showToast(resolveText(resId, *formatArgs), ToastType.WARNING, duration)
    }

    fun showInfoRes(@StringRes resId: Int, vararg formatArgs: Any, duration: Long = 3000L) {
        showToast(resolveText(resId, *formatArgs), ToastType.INFO, duration)
    }
}