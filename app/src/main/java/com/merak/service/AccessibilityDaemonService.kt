package com.merak.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.merak.core.accessibility.AccessibilityServiceManager
import com.merak.data.settings.repo.SettingsRepo
import com.merak.ui.activity.MainActivity
import com.merak.x.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AccessibilityDaemonService : Service(), KoinComponent {
    private val settingsRepo: SettingsRepo by inject()
    private val accessibilityServiceManager: AccessibilityServiceManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastKnownSecureValue = ""

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            restoreIfNeeded("settings_changed")
        }
    }

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                restoreIfNeeded("screen_on")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
        lastKnownSecureValue = accessibilityServiceManager.currentEnabledServicesValue()
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            true,
            settingsObserver
        )
        ContextCompat.registerReceiver(
            this,
            screenOnReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafe()
            return START_NOT_STICKY
        }

        return if (startForegroundCompat()) {
            restoreIfNeeded("start")
            START_STICKY
        } else {
            stopSelfSafe()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOnReceiver) }
        runCatching { contentResolver.unregisterContentObserver(settingsObserver) }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(): Boolean {
        if (!hasNotificationPermission(this)) return false

        return try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start accessibility daemon foreground service")
            false
        }
    }

    private fun restoreIfNeeded(reason: String) {
        serviceScope.launch {
            val currentValue = accessibilityServiceManager.currentEnabledServicesValue()
            if (reason != "start" && currentValue == lastKnownSecureValue) return@launch
            lastKnownSecureValue = currentValue

            val settings = settingsRepo.appSettings.first()
            if (settings.accessibilityDaemonServices.isEmpty()) {
                stopSelfSafe()
                return@launch
            }
            withContext(Dispatchers.IO) {
                accessibilityServiceManager.restoreDaemonServices(settings.accessibilityDaemonServices) { label ->
                    if (settings.isAccessibilityDaemonToastEnabled) {
                        serviceScope.launch {
                            Toast.makeText(
                                this@AccessibilityDaemonService,
                                getString(R.string.accessibility_daemon_restored_toast, label),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            lastKnownSecureValue = accessibilityServiceManager.currentEnabledServicesValue()
        }
    }

    private fun stopSelfSafe() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.accessibility_daemon_notification_title))
            .setContentText(getString(R.string.accessibility_daemon_notification_content))
            .setSmallIcon(R.drawable.ic_accessibility)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun createNotificationChannelIfNeeded() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.accessibility_daemon_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "accessibility_daemon"
        private const val NOTIFICATION_ID = 1002
        private const val TAG = "AccessibilityDaemon"
        private const val ACTION_STOP = "com.merak.service.AccessibilityDaemonService.STOP"

        fun start(context: Context) {
            if (!hasNotificationPermission(context)) return
            ContextCompat.startForegroundService(context, Intent(context, AccessibilityDaemonService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AccessibilityDaemonService::class.java).apply {
                action = ACTION_STOP
            })
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
