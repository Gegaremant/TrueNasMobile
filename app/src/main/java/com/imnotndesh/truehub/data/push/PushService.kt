package com.imnotndesh.truehub.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.imnotndesh.truehub.MainActivity
import com.imnotndesh.truehub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Foreground service that keeps the push pipeline alive:
 *
 *  1. (re)registers this device's E2E public key with the relay,
 *  2. opens the ntfy WebSocket for the device topic,
 *  3. decrypts incoming envelopes and renders them as notifications.
 *
 * FCM transport is a later milestone (needs a Firebase project); the feature
 * works GMS-free over ntfy today.
 */
class PushService : LifecycleService() {

    private var connectionJob: Job? = null
    private var ntfyClient: NtfyPushClient? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> stopPush()
            else -> startPush()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ntfyClient?.stop()
        ntfyClient = null
        connectionJob?.cancel()
        super.onDestroy()
    }

    private fun startPush() {
        // Bring the FGS up immediately (5s rule), then settle based on config.
        createNotificationChannels()
        startForeground(FOREGROUND_ID, serviceNotification("Connecting…"))

        connectionJob?.cancel()
        connectionJob = lifecycleScope.launch {
            val config = PushPrefs.getConfig(this@PushService)
            if (!config.enabled) {
                Log.i(TAG, "Push is disabled, stopping service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            if (config.relayUrl.isBlank() || config.ntfyTopic.isBlank()) {
                Log.w(TAG, "Push enabled but relay/topic missing, stopping service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            registerDevice(config)

            startForeground(FOREGROUND_ID, serviceNotification("Watching TrueNAS alerts"))
            startTransport(config)
        }
    }

    private fun stopPush() {
        ntfyClient?.stop()
        ntfyClient = null
        connectionJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Re-registers the device key so the relay always holds the current public
     * key. The relay upserts on (relayToken, pushToken), so this never creates
     * duplicates.
     */
    private suspend fun registerDevice(config: PushConfig) {
        val pubkey = try {
            PushE2E.publicKeyPointB64(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read E2E keypair: ${e.message}")
            return
        }
        val registration = JSONObject()
            .put("name", deviceName())
            .put("pubkey", pubkey)
            .put("transport", config.transport)
            .put("pushToken", config.ntfyTopic)

        val result = RelayClient().register(config.relayUrl, config.relayToken, registration)
        when (result) {
            is RelayClient.RelayResult.Success -> {
                val deviceId = result.body.optString("deviceId")
                if (deviceId.isNotBlank() && deviceId != config.deviceId) {
                    PushPrefs.saveConfig(this, config.copy(deviceId = deviceId))
                    Log.i(TAG, "Registered with relay (deviceId=$deviceId)")
                }
            }
            is RelayClient.RelayResult.Error -> {
                Log.w(TAG, "Relay registration failed: ${result.message}")
            }
        }
    }

    private fun startTransport(config: PushConfig) {
        ntfyClient?.stop()
        ntfyClient = NtfyPushClient(
            ntfyBaseUrl = config.ntfyBaseUrl,
            topic = config.ntfyTopic,
            scope = lifecycleScope,
            onEnvelope = ::handleEnvelope
        )
        ntfyClient?.start()
    }

    private fun handleEnvelope(envelope: PushEnvelope) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val plaintext = PushE2E.decryptEnvelope(this@PushService, envelope)
                PushNotificationRenderer(this@PushService).render(plaintext)
                Log.i(TAG, "Delivered push notification (${plaintext.size} bytes)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt inbound push envelope", e)
            }
        }
    }

    private fun deviceName(): String =
        "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

    private fun serviceNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("TrueNasMobile Push")
            .setContentText(text)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun createNotificationChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Push connection",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Keeps the TrueNAS push websocket alive"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannels(listOf(serviceChannel))

        // Alert channels are also created by AlertsWorker/renderer; creating
        // them here too is idempotent and ensures they exist before the first push.
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    com.imnotndesh.truehub.data.workers.AlertsWorker.CHANNEL_SYSTEM,
                    "System",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    com.imnotndesh.truehub.data.workers.AlertsWorker.CHANNEL_INFORMATIONAL,
                    "Informational",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    com.imnotndesh.truehub.data.workers.AlertsWorker.CHANNEL_WARNING,
                    "Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep pushing after the user swipes the app away.
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val ACTION_START = "com.imnotndesh.truehub.action.PUSH_START"
        const val ACTION_STOP = "com.imnotndesh.truehub.action.PUSH_STOP"
        const val CHANNEL_SERVICE = "truehub_push_service"

        private const val TAG = "PushService"
        private const val FOREGROUND_ID = 90210

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PushService::class.java).setAction(ACTION_START)
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, PushService::class.java).setAction(ACTION_STOP))
        }
    }
}