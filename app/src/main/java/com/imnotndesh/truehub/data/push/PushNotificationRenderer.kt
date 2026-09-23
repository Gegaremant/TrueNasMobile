package com.imnotndesh.truehub.data.push

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.imnotndesh.truehub.MainActivity
import com.imnotndesh.truehub.R
import com.imnotndesh.truehub.data.workers.AlertsWorker
import com.imnotndesh.truehub.data.workers.DismissAlertReceiver
import org.json.JSONObject

/**
 * Turns a decrypted relay payload into an Android notification, reusing the
 * exact channel split and Dismiss mechanics of [AlertsWorker] so push and
 * polling notifications feel like one system.
 */
class PushNotificationRenderer(private val context: Context) {

    fun render(payload: ByteArray) {
        val text = String(payload, Charsets.UTF_8)
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Skipping push notification: POST_NOTIFICATIONS not granted")
            return
        }

        // TrueNAS webhook payloads nest the alert under "alert" on newer
        // versions; older ones keep the fields at the top level.
        val alert = json.optJSONObject("alert") ?: json
        val level = alert.optString("level", "INFO")
        val title = alert.optString("title").ifBlank { "TrueNAS Alert: $level" }
        val bodyText = alert.optString("text")
        val uuid = alert.optString("uuid")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(alertChannels())

        val notificationId = if (uuid.isNotBlank()) uuid.hashCode() else text.hashCode()

        val builder = NotificationCompat.Builder(context, channelForPush(level, bodyText))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(bodyText.ifBlank { "A new TrueNAS alert has been triggered." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        builder.setContentIntent(
            PendingIntent.getActivity(
                context,
                notificationId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        if (uuid.isNotBlank()) {
            val dismissIntent = Intent(context, DismissAlertReceiver::class.java).apply {
                action = DismissAlertReceiver.ACTION_DISMISS_ALERT
                putExtra(DismissAlertReceiver.EXTRA_ALERT_UUID, uuid)
                putExtra(DismissAlertReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    /** Mirrors [AlertsWorker.channelForAlert] but works off the webhook shape. */
    private fun channelForPush(level: String, text: String): String {
        val lvl = level.lowercase()
        if (lvl == "warning" || lvl == "critical" || lvl == "alert" || lvl == "emergency") {
            return AlertsWorker.CHANNEL_WARNING
        }
        val haystack = text.lowercase()
        return when {
            haystack.contains("truenas version") || haystack.contains("system update") ->
                AlertsWorker.CHANNEL_SYSTEM
            else -> AlertsWorker.CHANNEL_INFORMATIONAL
        }
    }

    private fun alertChannels() = listOf(
        android.app.NotificationChannel(
            AlertsWorker.CHANNEL_SYSTEM,
            "System",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "System-level update and maintenance notifications" },
        android.app.NotificationChannel(
            AlertsWorker.CHANNEL_INFORMATIONAL,
            "Informational",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Informational and application update notifications" },
        android.app.NotificationChannel(
            AlertsWorker.CHANNEL_WARNING,
            "Warnings",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "High-priority warnings and critical alerts" }
    )

    private companion object {
        const val TAG = "PushNotification"
    }
}