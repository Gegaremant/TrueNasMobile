package com.gegaremant.truenasmobile.data.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.TrueNASClient
import com.gegaremant.truenasmobile.data.api.AuthService
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.MultiAccountPrefs
import com.gegaremant.truenasmobile.data.helpers.TrueNasMobileLogger
import com.gegaremant.truenasmobile.data.helpers.dataStore
import com.gegaremant.truenasmobile.data.models.Config
import com.gegaremant.truenasmobile.data.models.LoginMethod
import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.alerts.AlertDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AlertsWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "TrueNAS_Alerts_Sync"

        // Channels we expose to users for independent toggling.
        const val CHANNEL_SYSTEM = "truenasmobile_system_channel"
        const val CHANNEL_INFORMATIONAL = "truenasmobile_informational_channel"
        const val CHANNEL_WARNING = "truenasmobile_warning_channel"

        private val SEEN_ALERTS_KEY = stringSetPreferencesKey("seen_alerts_ids")

        /**
         * Routes a TrueNAS alert to the correct channel based on its level and text.
         *
         * - Warning/critical levels → dedicated high-priority warning channel.
         * - System updates: text mentions "truenas version" or "system update".
         * - Application updates: text mentions "applications" or "updates are available".
         * - Everything else falls back to informational.
         */
        fun channelForAlert(alert: System.AlertResponse): String {
            val level = alert.level.lowercase()
            if (level == "warning" || level == "critical") {
                return CHANNEL_WARNING
            }

            val haystack = buildString {
                alert.formatted?.let { append(it.lowercase()) }
                append(' ')
                append(alert.text.lowercase())
            }

            return when {
                haystack.contains("truenas version") || haystack.contains("system update") -> CHANNEL_SYSTEM
                haystack.contains("applications") || haystack.contains("updates are available") -> CHANNEL_INFORMATIONAL
                else -> CHANNEL_INFORMATIONAL
            }
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AlertsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var client: TrueNASClient? = null
        try {
            val (serverId, accountId) = MultiAccountPrefs.getLastUsedProfile(context)
                ?: return@withContext Result.success()
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
                    context,
                    accountId,
                    account.loginMethod
                )

                val loginResult = when (account.loginMethod) {
                    LoginMethod.API_KEY -> {
                        if (credentialPrimary.isNullOrBlank()) {
                            client.disconnect()
                            return@withContext Result.failure()
                        }
                        manager.auth.loginWithApiKeyWithResult(credentialPrimary)
                    }
                    LoginMethod.PASSWORD, LoginMethod.TOTP -> {
                        if (credentialPrimary.isNullOrBlank() || credentialSecondary.isNullOrBlank()) {
                            client.disconnect()
                            return@withContext Result.failure()
                        }
                        manager.auth.loginUserWithResult(
                            AuthService.DefaultAuth(credentialPrimary, credentialSecondary)
                        )
                    }
                }

                authed = loginResult is ApiResult.Success && loginResult.data == true
            }

            if (!authed) {
                TrueNasMobileLogger.e("AlertsWorker", "Authentication failed for account $accountId")
                client.disconnect()
                return@withContext Result.retry()
            }

            val alertsResult = manager.system.listAlertsWithResult()

            when (alertsResult) {
                is ApiResult.Success -> {
                    val unDismissedAlerts = alertsResult.data.filter { !it.dismissed }
                    val currentActiveUuids = unDismissedAlerts.map { it.uuid }.toSet()

                    val prefs = context.dataStore.data.first()
                    val seenAlertUuids = prefs[SEEN_ALERTS_KEY] ?: emptySet()

                    val newAlerts = unDismissedAlerts.filter { !seenAlertUuids.contains(it.uuid) }

                    if (newAlerts.isNotEmpty()) {
                        notifyAlerts(newAlerts)
                    }

                    context.dataStore.edit { it[SEEN_ALERTS_KEY] = currentActiveUuids }
                }
                is ApiResult.Error -> {
                    Log.e("AlertsWorker", "Failed to list alerts: ${alertsResult.message}")
                }
                else -> {}
            }

            client.disconnect()
            Result.success()

        } catch (e: Exception) {
            Log.e("AlertsWorker", "Error fetching system alerts in background", e)
            client?.disconnect()
            Result.retry()
        }
    }

    private fun notifyAlerts(alerts: List<System.AlertResponse>) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("AlertsWorker", "Skipping alert notification: POST_NOTIFICATIONS not granted")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Independently-toggleable channels, matching the user-facing grouping we want.
        val systemChannel = NotificationChannel(
            CHANNEL_SYSTEM, "System", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "System-level update and maintenance notifications" }
        val informationalChannel = NotificationChannel(
            CHANNEL_INFORMATIONAL, "Informational", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Informational and application update notifications" }
        val warningChannel = NotificationChannel(
            CHANNEL_WARNING, "Warnings", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "High-priority warnings and critical alerts" }

        notificationManager.createNotificationChannels(
            listOf(systemChannel, informationalChannel, warningChannel)
        )

        alerts.forEach { alert ->
            val notificationId = alert.uuid.hashCode()
            val channelId = channelForAlert(alert)

            val dismissIntent = Intent(context, DismissAlertReceiver::class.java).apply {
                action = DismissAlertReceiver.ACTION_DISMISS_ALERT
                putExtra(DismissAlertReceiver.EXTRA_ALERT_UUID, alert.uuid)
                putExtra(DismissAlertReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // "Backup failed" is not an answer on its own: the arguments are
            // where the stand says which dataset, which host, what error. They
            // go into the body, so expanding the notification actually explains
            // it.
            val headline = alert.formatted?.takeIf { it.isNotBlank() }
                ?: alert.text.takeIf { it.isNotBlank() }
                ?: AlertDetails.readableClass(alert.klass)
            val detailLines = AlertDetails.lines(alert)
            val fullText = if (detailLines.isEmpty()) {
                headline
            } else {
                headline + "\n" + detailLines.joinToString("\n")
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.gegaremant.truenasmobile.R.drawable.ic_stat_notification)
                .setContentTitle("${AlertDetails.readableClass(alert.klass)} · ${alert.level.uppercase()}")
                .setContentText(headline)
                .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
                .setSubText(context.getString(com.gegaremant.truenasmobile.R.string.alert_notification_subtext))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .build()

            notificationManager.notify(notificationId, notification)
        }
    }
}