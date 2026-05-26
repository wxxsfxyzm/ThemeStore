package com.merak.service

import android.Manifest
import android.app.ActivityManager
import android.app.AppOpsManager
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
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
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
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val CHANNEL_ID = "0"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.merak.service.KeepAliveService.START"
        private const val ACTION_STOP = "com.merak.service.KeepAliveService.STOP"
        const val ACTION_STATE_CHANGED = "com.merak.service.KeepAliveService.STATE_CHANGED"
        const val EXTRA_IS_RUNNING = "com.merak.service.KeepAliveService.EXTRA_IS_RUNNING"
        private const val TAG = "KeepAliveService"
        private const val OPSTR_FOREGROUND_SERVICE_SPECIAL_USE = "android:foreground_service_special_use"

        // Expose state for the Accessibility Watchdog to monitor
        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow = _isRunningFlow.asStateFlow()

        fun start(context: Context) {
            if (!hasNotificationPermission(context)) return
            if (!hasForegroundServiceSpecialUseAllowed(context)) return

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

        @Suppress("DEPRECATION")
        fun isServiceRunning(context: Context): Boolean {
            val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
            val serviceName = KeepAliveService::class.java.name
            return activityManager.getRunningServices(Int.MAX_VALUE).any {
                it.service.packageName == context.packageName && it.service.className == serviceName
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }

        fun hasForegroundServiceSpecialUseAllowed(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = appOps.checkOpNoThrow(
                OPSTR_FOREGROUND_SERVICE_SPECIAL_USE,
                android.os.Process.myUid(),
                context.packageName
            )
            return mode != AppOpsManager.MODE_IGNORED && mode != AppOpsManager.MODE_ERRORED
        }
    }

    override fun onCreate() {
        super.onCreate()
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

        return if (startForegroundCompat()) {
            START_STICKY
        } else {
            stopSelfSafe()
            START_NOT_STICKY
        }
    }

    private fun startForegroundCompat(): Boolean {
        if (!hasNotificationPermission(this)) return false
        if (!hasForegroundServiceSpecialUseAllowed(this)) return false

        return try {
            val isA11yEnabled = ThemeInstallAccessibilityService.isAccessibilityServiceEnabled(
                this,
                SelectToSpeakService::class.java
            )
            startForegroundCompat(
                buildNotification(
                    stats = calculateStatistics(),
                    isA11yConnected = ThemeInstallAccessibilityService.isConnected(),
                    isA11yEnabled = isA11yEnabled
                )
            )
            Timber.tag(TAG).d("FGS Started | ServiceCompat.startForeground executed")
            true
        } catch (e: Exception) {
            setRunningState(false)
            Timber.tag(TAG).e(e, "Failed to start foreground service")
            false
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )
        setRunningState(true)
    }

    private suspend fun updateNotification(settings: AppSettings, isA11yConnected: Boolean) {
        if (!hasNotificationPermission(this)) return
        if (!hasForegroundServiceSpecialUseAllowed(this)) return

        val stats = withContext(Dispatchers.IO) { calculateStatistics() }
        val isA11yEnabled = ThemeInstallAccessibilityService.isAccessibilityServiceEnabled(
            this,
            SelectToSpeakService::class.java
        )
        startForegroundCompat(buildNotification(stats, isA11yConnected, isA11yEnabled))
    }

    private fun handleStopAction() {
        setRunningState(false)
        stopSelfSafe()
    }

    private fun stopSelfSafe() {
        setRunningState(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("Lifecycle | onDestroy - Swipe to kill triggered")
        // Crucial: Set to false so the Watchdog knows we died
        setRunningState(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(
        stats: Statistics,
        isA11yConnected: Boolean,
        isA11yEnabled: Boolean
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val statusText = when {
            isA11yConnected || isA11yEnabled -> {
                getString(R.string.keep_alive_notification_content, stats.themeInstallCount, stats.alarmInterceptCount)
            }
            else -> getString(R.string.keep_alive_notification_accessibility_disabled)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.keep_alive_notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.magic_wand_color)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
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
            } catch (_: Exception) {
                // Ignore file IO issues
            }
        }
        return Statistics(themeInstallCount, alarmInterceptCount)
    }

    data class Statistics(val themeInstallCount: Int, val alarmInterceptCount: Int)

    private fun createNotificationChannelIfNeeded() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notificationChannels
            .filter { it.id != CHANNEL_ID }
            .forEach { manager.deleteNotificationChannel(it.id) }
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun setRunningState(isRunning: Boolean) {
        _isRunningFlow.value = isRunning
        sendBroadcast(
            Intent(ACTION_STATE_CHANGED).apply {
                setPackage(packageName)
                putExtra(EXTRA_IS_RUNNING, isRunning)
            }
        )
    }
}
