package com.gegaremant.truenasmobile.data.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.TrueNASClient
import com.gegaremant.truenasmobile.data.api.AuthService
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.MultiAccountPrefs
import com.gegaremant.truenasmobile.data.helpers.TrueNasMobileLogger
import com.gegaremant.truenasmobile.data.models.Config
import com.gegaremant.truenasmobile.data.models.LoginMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CancelJobWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORK_NAME_PREFIX = "TrueNAS_Cancel_Job_"
        private const val KEY_JOB_ID = "job_id"

        fun enqueue(context: Context, jobId: Int) {
            val request = OneTimeWorkRequestBuilder<CancelJobWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(workDataOf(KEY_JOB_ID to jobId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_PREFIX + jobId,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val jobId = inputData.getInt(KEY_JOB_ID, -1)
        if (jobId == -1) return@withContext Result.failure()

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

            val result = manager.system.cancelJob(jobId)
            client.disconnect()

            when (result) {
                is ApiResult.Success -> Result.success()
                is ApiResult.Error -> {
                    TrueNasMobileLogger.e("CancelJobWorker", "Failed to cancel job $jobId: ${result.message}")
                    Result.failure()
                }
                else -> Result.retry()
            }
        } catch (e: Exception) {
            TrueNasMobileLogger.e("CancelJobWorker", "Error cancelling job $jobId", e)
            client?.disconnect()
            Result.retry()
        }
    }
}