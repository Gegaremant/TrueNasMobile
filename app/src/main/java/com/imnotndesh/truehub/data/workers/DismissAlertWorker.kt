package com.imnotndesh.truehub.data.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.imnotndesh.truehub.data.ApiResult
import com.imnotndesh.truehub.data.TrueNASClient
import com.imnotndesh.truehub.data.api.AuthService
import com.imnotndesh.truehub.data.api.TrueNASApiManager
import com.imnotndesh.truehub.data.helpers.MultiAccountPrefs
import com.imnotndesh.truehub.data.helpers.TrueHubLogger
import com.imnotndesh.truehub.data.models.Config
import com.imnotndesh.truehub.data.models.LoginMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DismissAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORK_NAME_PREFIX = "TrueNAS_Dismiss_Alert_"
        private const val KEY_ALERT_UUID = "alert_uuid"

        fun enqueue(context: Context, alertUuid: String) {
            val request = OneTimeWorkRequestBuilder<DismissAlertWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(workDataOf(KEY_ALERT_UUID to alertUuid))
                .build()

            // Keyed by uuid so a double-tap before the system removes the
            // action doesn't queue two identical dismiss calls.
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_PREFIX + alertUuid,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val alertUuid = inputData.getString(KEY_ALERT_UUID)
            ?: return@withContext Result.failure()

        var client: TrueNASClient? = null
        try {
            val (serverId, accountId) = MultiAccountPrefs.getLastUsedProfile(context)
                ?: return@withContext Result.failure()

            val server = MultiAccountPrefs.getServer(context, serverId)
                ?: return@withContext Result.failure()
            val account = MultiAccountPrefs.getAccount(context, accountId)
                ?: return@withContext Result.failure()

            client = TrueNASClient(
                Config.ClientConfig(
                    serverUrl = server.serverUrl,
                    insecure = server.insecure
                )
            )
            if (!client.connect()) return@withContext Result.retry()

            val manager = TrueNASApiManager(client, context)

            val token = MultiAccountPrefs.getTokenForLastUsed(context)
            var authed = token != null &&
                    (manager.auth.loginWithTokenAndResult(token) is ApiResult.Success)

            if (!authed) {
                val (credentialPrimary, credentialSecondary) = MultiAccountPrefs.getAccountCredentials(
                    context, accountId, account.loginMethod
                )
                val loginResult = when (account.loginMethod) {
                    LoginMethod.API_KEY -> {
                        if (credentialPrimary.isNullOrBlank()) {
                            client.disconnect(); return@withContext Result.failure()
                        }
                        manager.auth.loginWithApiKeyWithResult(credentialPrimary)
                    }
                    LoginMethod.PASSWORD, LoginMethod.TOTP -> {
                        if (credentialPrimary.isNullOrBlank() || credentialSecondary.isNullOrBlank()) {
                            client.disconnect(); return@withContext Result.failure()
                        }
                        manager.auth.loginUserWithResult(
                            AuthService.DefaultAuth(credentialPrimary, credentialSecondary)
                        )
                    }
                }
                authed = loginResult is ApiResult.Success && loginResult.data == true
            }

            if (!authed) {
                client.disconnect()
                return@withContext Result.retry()
            }

            val result = manager.system.dismissAlertWithResult(alertUuid)
            client.disconnect()

            when (result) {
                is ApiResult.Success -> Result.success()
                is ApiResult.Error -> {
                    TrueHubLogger.e("DismissAlertWorker", "Failed to dismiss $alertUuid: ${result.message}")
                    Result.retry()
                }
                else -> Result.retry()
            }
        } catch (e: Exception) {
            TrueHubLogger.e("DismissAlertWorker", "Error dismissing $alertUuid", e)
            client?.disconnect()
            Result.retry()
        }
    }
}