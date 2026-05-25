package com.merak.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.merak.core.os.shizuku.PrivilegedManager
import com.merak.data.settings.model.AppSettings
import com.merak.data.settings.repo.SettingsRepo
import com.merak.ui.activity.MainActivity
import com.merak.util.timber.LogFormatter
import com.merak.x.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File

class KeepAliveService : Service(), KoinComponent {

    private val settingsRepo: SettingsRepo by inject()
    private val privilegedManager: PrivilegedManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val CHANNEL_ID = "keep_alive_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.merak.service.KeepAliveService.START"
        private const val ACTION_STOP = "com.merak.service.KeepAliveService.STOP"
        private const val TAG = "KeepAliveService"

        // Expose state for the Accessibility Watchdog to monitor
        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow = _isRunningFlow.asStateFlow()

        fun start(context: Context) {
            try {
                val intent = Intent(context, KeepAliveService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Start exception | FGS start not allowed")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunningFlow.value = true
        Timber.tag(TAG).d("Lifecycle | onCreate")
        createNotificationChannelIfNeeded()

        // Sync UI notification with stats and connection events
        serviceScope.launch {
            combine(
                settingsRepo.appSettings,
                ThemeInstallAccessibilityService.isConnectedFlow,
                ThemeInstallAccessibilityService.interceptEventFlow,
                ThemeInstallAccessibilityService.refreshEventFlow
            ) { settings, isA11yConnected, _, _ ->
                Pair(settings, isA11yConnected)
            }.collectLatest { (settings, isA11yConnected) ->
                if (settings.isKeepAliveEnabled) {
                    updateNotification(settings, isA11yConnected)
                } else {
                    stopSelfSafe()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            handleStopAction()
            return START_NOT_STICKY
        }

        _isRunningFlow.value = true
        startForegroundCompat()

        return START_STICKY
    }

    private fun startForegroundCompat() {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.keep_alive_notification_title))
                .setContentText("Initializing...")
                .setSmallIcon(R.drawable.magic_wand_color)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

            // The core fix discovered from gkd
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }

            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
            Timber.tag(TAG).d("FGS Started | ServiceCompat.startForeground executed")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start foreground service")
        }
    }

    private suspend fun updateNotification(settings: AppSettings, isA11yConnected: Boolean) {
        if (!hasNotificationPermission()) return

        val stats = withContext(Dispatchers.IO) { calculateStatistics() }
        val notification = buildNotification(stats, isA11yConnected)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun handleStopAction() {
        _isRunningFlow.value = false
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    privilegedManager.setAccessibilityServiceState(enabled = false)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to disable accessibility service")
                }
            }
            stopSelfSafe()
        }
    }

    private fun stopSelfSafe() {
        _isRunningFlow.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("Lifecycle | onDestroy - Swipe to kill triggered")
        // Crucial: Set to false so the Watchdog knows we died
        _isRunningFlow.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(stats: Statistics, isA11yConnected: Boolean): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPendingIntent = PendingIntent.getService(
            this, 2, Intent(this, KeepAliveService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val statusText = if (isA11yConnected) {
            getString(R.string.keep_alive_notification_content, stats.themeInstallCount, stats.alarmInterceptCount)
        } else {
            "Accessibility service disconnected. Tap to fix."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.keep_alive_notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.magic_wand_color)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(if (isA11yConnected) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.action_stop), stopPendingIntent)
            .build()
    }

    private fun calculateStatistics(): Statistics {
        val logDir = File(filesDir, "logs")
        if (!logDir.exists()) return Statistics(0, 0)

        var themeInstallCount = 0
        var alarmInterceptCount = 0

        val allFiles = logDir.listFiles { _, name -> name.endsWith(".log") } ?: emptyArray()
        allFiles.forEach { file ->
            try {
                file.useLines { lines ->
                    lines.forEach { line ->
                        if (line.contains("/${LogFormatter.TAG_THEME_INSTALL}:")) themeInstallCount++
                        if (line.contains("/${LogFormatter.TAG_ALARM_INTERCEPT}:")) alarmInterceptCount++
                    }
                }
            } catch (e: Exception) {
                // Ignore file IO issues
            }
        }
        return Statistics(themeInstallCount, alarmInterceptCount)
    }

    data class Statistics(val themeInstallCount: Int, val alarmInterceptCount: Int)

    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannelIfNeeded() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.keep_alive_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.keep_alive_channel_desc)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }
}