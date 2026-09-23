package com.imnotndesh.truehub.data.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.imnotndesh.truehub.data.ApiResult
import com.imnotndesh.truehub.data.TrueNASClient
import com.imnotndesh.truehub.data.api.AuthService
import com.imnotndesh.truehub.data.api.TrueNASApiManager
import com.imnotndesh.truehub.data.helpers.MultiAccountPrefs
import com.imnotndesh.truehub.data.helpers.QuickLaunchSync
import com.imnotndesh.truehub.data.helpers.TrueHubLogger
import com.imnotndesh.truehub.data.helpers.WidgetDataStore
import com.imnotndesh.truehub.data.models.Config
import com.imnotndesh.truehub.data.models.LoginMethod
import com.imnotndesh.truehub.data.workers.AppsRefreshWorker.Companion.scheduleImmediate
import com.imnotndesh.truehub.data.workers.AppsRefreshWorker.Companion.scheduleRecurring
import com.imnotndesh.truehub.ui.utils.AppCache
import com.imnotndesh.truehub.ui.widgets.AppsUpdateWidgetUpdater
import com.imnotndesh.truehub.ui.widgets.pools.PoolsWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background worker that refreshes both the Apps widget cache and the Pools
 * widget cache in a single authenticated session.
 *
 * Call sites are unchanged — [scheduleRecurring] and [scheduleImmediate] have
 * the same signatures and work names as before. The only internal change is
 * that pools are fetched in parallel with apps, stored via
 * [WidgetDataStore.saveAppsAndPools], and [PoolsWidgetUpdater.update] is
 * called so the pools widget refreshes alongside the apps widget.
 */
class AppsRefreshWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME          = "TrueHub_Apps_Refresh"
        const val ONE_TIME_WORK_NAME = "TrueHub_Apps_Refresh_Once"

        fun scheduleRecurring(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppsRefreshWorker>(3, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<AppsRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var client: TrueNASClient? = null
        try {
            val servers = MultiAccountPrefs.getServers(context)
            if (servers.isEmpty()) return@withContext Result.success()

            val (serverId, accountId) = MultiAccountPrefs.getLastUsedProfile(context)
                ?: return@withContext Result.success()

            val server  = MultiAccountPrefs.getServer(context, serverId)
                ?: return@withContext Result.failure()
            val account = MultiAccountPrefs.getAccount(context, accountId)
                ?: return@withContext Result.failure()

            client = TrueNASClient(Config.ClientConfig(
                serverUrl = server.serverUrl,
                insecure  = server.insecure
            ))
            if (!client.connect()) return@withContext Result.retry()

            val manager = TrueNASApiManager(client, context)

            val token  = MultiAccountPrefs.getTokenForLastUsed(context)
            val authed = if (token != null) {
                (manager.auth.loginWithTokenAndResult(token) is ApiResult.Success)
            } else false

            if (!authed) {
                val (cred1, cred2) = MultiAccountPrefs.getAccountCredentials(
                    context, accountId, account.loginMethod
                )
                val loginResult = when (account.loginMethod) {
                    LoginMethod.API_KEY -> cred1?.let {
                        manager.auth.loginWithApiKeyWithResult(it)
                    } ?: return@withContext Result.failure()
                    LoginMethod.PASSWORD, LoginMethod.TOTP -> if (cred1 != null && cred2 != null) {
                        manager.auth.loginUserWithResult(AuthService.DefaultAuth(cred1, cred2))
                    } else return@withContext Result.failure()
                }
                if (loginResult is ApiResult.Error) return@withContext Result.retry()
            }

            // ── Fetch apps + pools concurrently ──────────────────────────────
            val appsDeferred  = async { manager.apps.getInstalledAppsWithResult() }
            val poolsDeferred = async { manager.system.getPoolsWithResult() }

            val appsResult  = appsDeferred.await()
            val poolsResult = poolsDeferred.await()

            val apps  = if (appsResult  is ApiResult.Success) appsResult.data  else null
            val pools = if (poolsResult is ApiResult.Success) poolsResult.data else null

            when {
                apps != null && pools != null -> {
                    WidgetDataStore.saveAppsAndPools(context, apps, pools)
                    AppCache.updateApps(apps)
                    AppCache.updatePools(pools)
                }
                apps != null -> {
                    WidgetDataStore.saveUpgradableApps(context, apps)
                    AppCache.updateApps(apps)
                    QuickLaunchSync.refresh(context, apps)
                }
                pools != null -> {
                    WidgetDataStore.savePools(context, pools)
                    AppCache.updatePools(pools)
                }
                else -> {
                    client.disconnect()
                    return@withContext if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            }

            // ── Trigger widget redraws ────────────────────────────────────────
            if (apps  != null) AppsUpdateWidgetUpdater.update(context)
            if (pools != null) PoolsWidgetUpdater.update(context)

            client.disconnect()
            Result.success()

        } catch (e: Exception) {
            client?.disconnect()
            TrueHubLogger.e("AppsRefreshWorker", "Failed: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}