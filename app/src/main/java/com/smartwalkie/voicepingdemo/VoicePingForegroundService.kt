package com.smartwalkie.voicepingdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smartwalkie.voicepingsdk.ConnectionState
import com.smartwalkie.voicepingsdk.VoicePing
import com.smartwalkie.voicepingsdk.callback.ConnectCallback
import com.smartwalkie.voicepingsdk.callback.DisconnectCallback
import com.smartwalkie.voicepingsdk.exception.VoicePingException
import com.smartwalkie.voicepingsdk.listener.ConnectionStateListener

/**
 * Foreground service that keeps VoicePing connection alive while app is in background.
 */
class VoicePingForegroundService : Service(), ConnectionStateListener {

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        VoicePing.setConnectionStateListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                handleStop()
                return START_NOT_STICKY
            }
            else -> {
                // Always move to foreground ASAP.
                startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
                connectIfNeeded()
                updateNotificationForState(VoicePing.getConnectionState())
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        VoicePing.setConnectionStateListener(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ConnectionStateListener
    override fun onConnectionStateChanged(connectionState: ConnectionState) {
        updateNotificationForState(connectionState)
    }

    override fun onConnectionError(e: VoicePingException) {
        updateNotification("Error: ${e.message ?: "unknown"}")
    }

    private fun connectIfNeeded() {
        val userId = MyPrefs.userId.orEmpty().trim()
        val company = MyPrefs.company.orEmpty().trim()
        val serverUrl = MyPrefs.serverUrl.orEmpty().trim()

        if (userId.isBlank() || company.isBlank() || serverUrl.isBlank()) {
            // Nothing to do.
            stopSelf()
            return
        }

        if (VoicePing.getConnectionState() == ConnectionState.DISCONNECTED) {
            updateNotification("Connecting…")
            VoicePing.connect(serverUrl, userId, company, object : ConnectCallback {
                override fun onConnected() {
                    updateNotification("Connected")
                }

                override fun onFailed(exception: VoicePingException) {
                    updateNotification("Connection failed")
                }
            })
        }
    }

    private fun handleStop() {
        updateNotification("Disconnecting…")
        try {
            VoicePing.disconnect(object : DisconnectCallback {
                override fun onDisconnected() {
                    stopForeground(true)
                    stopSelf()
                }
            })
        } catch (_: Throwable) {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun updateNotificationForState(state: ConnectionState) {
        val text = when (state) {
            ConnectionState.DISCONNECTED -> "Disconnected"
            ConnectionState.CONNECTING -> "Connecting…"
            ConnectionState.CONNECTED -> "Connected"
        }
        updateNotification(text)
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun buildNotification(statusText: String): Notification {
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, piFlags)

        val stopIntent = Intent(this, VoicePingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, piFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "VoicePing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps walkie-talkie connection running in background"
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "voiceping_foreground"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.smartwalkie.voicepingdemo.action.START"
        const val ACTION_STOP = "com.smartwalkie.voicepingdemo.action.STOP"

        fun start(context: Context) {
            val i = Intent(context, VoicePingForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            val i = Intent(context, VoicePingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
    }
}
