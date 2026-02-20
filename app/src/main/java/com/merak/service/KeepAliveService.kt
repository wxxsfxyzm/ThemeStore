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
import android.os.IBinder
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KeepAliveService : Service(), KoinComponent {

    private val settingsRepo: SettingsRepo by inject()
    private val privilegedManager: PrivilegedManager by inject()

    companion object {
        private const val CHANNEL_ID = "keep_alive_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.merak.service.KeepAliveService.START"
        private const val ACTION_STOP = "com.merak.service.KeepAliveService.STOP"
        private const val ACTION_REFRESH = "com.merak.service.KeepAliveService.REFRESH"
        private const val TAG = "KeepAliveService"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        // 核心修复1：直接向 ActivityManager 查询真实运行状态，解决冷启动静态变量丢失问题
        @Suppress("DEPRECATION")
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            // getRunningServices 在 Android 8.0 以后只能获取本应用的服务，完美符合我们的需求
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (KeepAliveService::class.java.name == service.service.className) {
                    _isRunning.value = true // 同步修正内存状态
                    return true
                }
            }
            _isRunning.value = false
            return false
        }

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun requestRefresh(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_REFRESH
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        Timber.tag(TAG).d("Service lifecycle | onCreate - Service created")

        createNotificationChannelIfNeeded()

        serviceScope.launch {
            settingsRepo.appSettings.collectLatest { settings ->
                if (settings.isKeepAliveEnabled && isForeground) {
                    try {
                        updateRealNotification(settings)
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Error in collectLatest update")
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val commandName = when (action) {
            ACTION_START -> "START"
            ACTION_STOP -> "STOP"
            ACTION_REFRESH -> "REFRESH"
            else -> "UNKNOWN"
        }
        Timber.tag(TAG).d("Received command | $commandName")

        if (action == ACTION_STOP) {
            _isRunning.value = false
            serviceScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        privilegedManager.setAccessibilityServiceState(enabled = false)
                        Timber.tag(TAG).i("Action STOP | Requested to disable accessibility service")
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Action STOP | Failed to disable accessibility service")
                    }
                }
                stopServiceSafe()
            }
            return START_NOT_STICKY
        }

        _isRunning.value = true
        startForegroundPlaceholder()

        serviceScope.launch {
            try {
                val settings = settingsRepo.appSettings.first()

                // 核心修复2：防穿透。不管是启动、刷新，还是被定时器拉起，只要开关没开，格杀勿论！
                if (!settings.isKeepAliveEnabled) {
                    Timber.tag(TAG).i("Action $commandName | Config is disabled. Committing suicide.")
                    stopServiceSafe()
                    return@launch
                }

                updateRealNotification(settings)

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error processing $commandName command")
            }
        }

        return START_STICKY
    }

    // 核心修复3：利用 AlarmManager 实现划卡不死（防杀拉起）
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.tag(TAG).w("Service lifecycle | onTaskRemoved - App swiped from recents")

        // 定时闹钟：1秒后唤醒自己
        val restartIntent = Intent(applicationContext, KeepAliveService::class.java).apply {
            action = ACTION_START
        }
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000,
            pendingIntent
        )
    }

    private fun stopServiceSafe() {
        _isRunning.value = false
        if (!isForeground) {
            startForegroundPlaceholder()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("Service lifecycle | onDestroy - Service destroyed")
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundPlaceholder() {
        if (isForeground) return

        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.keep_alive_notification_title))
                .setContentText("Starting...")
                .setSmallIcon(R.drawable.magic_wand_color)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
            isForeground = true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start foreground service | Unable to show placeholder notification")
        }
    }

    private suspend fun updateRealNotification(settings: AppSettings) {
        if (!hasNotificationPermission()) {
            Timber.tag(TAG).w("Permission missing | Cannot send notification: POST_NOTIFICATIONS required")
            return
        }

        val stats = withContext(Dispatchers.IO) {
            calculateStatistics()
        }

        Timber.tag(TAG).i("Refresh notification | Theme installs: ${stats.themeInstallCount}, Alarm intercepts: ${stats.alarmInterceptCount}")

        val notification = buildNotification(stats, settings.isOptimizationModeEnabled)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun calculateStatistics(): Statistics {
        val logDir = File(filesDir, "logs")
        if (!logDir.exists()) return Statistics(0, 0)

        val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayLogFile = File(logDir, "${fileNameFormat.format(Date())}.log")
        val allFiles = logDir.listFiles { _, name -> name.endsWith(".log") } ?: emptyArray()

        var themeInstallCount = 0
        var alarmInterceptCount = 0

        allFiles.forEach { file ->
            try {
                file.useLines { lines ->
                    lines.forEach { line ->
                        if (line.contains("/${LogFormatter.TAG_THEME_INSTALL}:")) {
                            themeInstallCount++
                        }
                        if (line.contains("/${LogFormatter.TAG_ALARM_INTERCEPT}:")) {
                            alarmInterceptCount++
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore file read errors
            }
        }
        return Statistics(themeInstallCount, alarmInterceptCount)
    }

    data class Statistics(val themeInstallCount: Int, val alarmInterceptCount: Int)

    private fun buildNotification(stats: Statistics, isOptimizationEnabled: Boolean): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, KeepAliveService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = getString(
            R.string.keep_alive_notification_content,
            stats.themeInstallCount,
            stats.alarmInterceptCount
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.keep_alive_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.magic_wand_color)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)

        builder.addAction(
            0,
            getString(R.string.action_stop),
            stopPendingIntent
        )

        return builder.build()
    }

    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
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