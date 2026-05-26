package com.merak.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.merak.data.settings.repo.SettingsRepo
import com.merak.util.timber.LogFormatter
import com.merak.x.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("AccessibilityPolicy")
open class ThemeInstallAccessibilityService : AccessibilityService(), KoinComponent {

    private val settingsRepo: SettingsRepo by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var aliveOverlayView: View? = null
    private var watchdogJob: Job? = null
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    companion object {
        const val ACTION_SERVICE_UP = "com.merak.action_Service_UP"
        const val ACTION_SERVICE_DOWN = "com.merak.action_Service_DOWN"
        private const val TAG = "ThemeAccessibility"

        private val ALARM_ACTION = String(
            Base64.decode("bWl1aS5pbnRlbnQuYWN0aW9uLkNIRUNLX1RJTUVfVVA=", Base64.DEFAULT)
        )

        private val _isConnectedFlow = MutableStateFlow(false)
        val isConnectedFlow = _isConnectedFlow.asStateFlow()

        private val _interceptEventFlow = MutableStateFlow(0L)
        val interceptEventFlow = _interceptEventFlow.asStateFlow()

        private val _refreshEventFlow = MutableStateFlow(0L)
        val refreshEventFlow = _refreshEventFlow.asStateFlow()

        private const val KEEP_ALIVE_RESTART_THROTTLE_MS = 1_000L

        @Volatile
        private var lastKeepAliveRestart = 0L

        @Volatile
        private var st_connectedTime = ""

        @Volatile
        private var st_receiveTime = ""

        @Volatile
        private var st_nextExpectedTime = ""

        fun isAccessibilityServiceEnabled(context: Context, clazz: Class<out AccessibilityService>): Boolean {
            val am = context.getSystemService(ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val myComponentName = "${context.packageName}/${clazz.name}"
            return enabledServices.any {
                "${it.resolveInfo.serviceInfo.packageName}/${it.resolveInfo.serviceInfo.name}" == myComponentName
            }
        }

        fun isConnected(): Boolean = _isConnectedFlow.value
        fun requestRefresh() {
            _refreshEventFlow.value = System.currentTimeMillis()
        }

        private fun canRestartKeepAlive(): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastKeepAliveRestart < KEEP_ALIVE_RESTART_THROTTLE_MS) return false
            lastKeepAliveRestart = now
            return true
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ALARM_ACTION && context != null) {
                handleAlarmIntercept(context)
            }
        }

        private fun handleAlarmIntercept(context: Context) {
            try {
                abortBroadcast()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Intercept failed; broadcast may not be ordered")
            }

            val nowMillis = System.currentTimeMillis()
            st_receiveTime = dateFormat.format(Date(nowMillis))
            st_nextExpectedTime = context.getString(R.string.estimated_next_time, timeFormat.format(Date(nowMillis + 7_200_000L)))

            LogFormatter.logAlarmIntercept(
                title = context.getString(R.string.alarm_intercept_title),
                content = context.getString(R.string.alarm_intercept_content, ALARM_ACTION, st_receiveTime, st_nextExpectedTime)
            )

            _interceptEventFlow.value = System.currentTimeMillis()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val intentFilter = IntentFilter().apply {
            addAction(ALARM_ACTION)
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        try {
            registerReceiver(mBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Receiver registration failed")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isConnectedFlow.value = true
        st_connectedTime = dateFormat.format(Date())

        addAliveOverlayView()
        sendServiceUpBroadcast()
        startWatchdog()
    }

    /**
     * The Watchdog Mechanism:
     * Observes KeepAliveService's running state. If it drops to false (e.g., swiped from recents)
     * but settings indicate it should be running, the Anchor brings it back to life.
     */
    private fun startWatchdog() {
        if (watchdogJob?.isActive == true) return
        watchdogJob = serviceScope.launch {
            settingsRepo.appSettings.collectLatest { settings ->
                if (settings.isKeepAliveEnabled && !KeepAliveService.isServiceRunning(applicationContext)) {
                    Timber.tag(TAG).w("Watchdog triggered | KeepAliveService is dead. Resurrecting...")
                    if (canRestartKeepAlive()) {
                        KeepAliveService.start(applicationContext)
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        _isConnectedFlow.value = false
        removeAliveOverlayView()
        sendServiceDownBroadcast()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _isConnectedFlow.value = false
        removeAliveOverlayView()
        try {
            unregisterReceiver(mBroadcastReceiver)
        } catch (_: Exception) {
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun sendServiceUpBroadcast() {
        try {
            sendBroadcast(Intent(ACTION_SERVICE_UP).apply { setPackage(packageName) })
        } catch (_: Exception) {
        }
    }

    private fun sendServiceDownBroadcast() {
        try {
            sendBroadcast(Intent(ACTION_SERVICE_DOWN).apply { setPackage(packageName) })
        } catch (_: Exception) {
        }
    }

    private fun addAliveOverlayView() {
        removeAliveOverlayView()
        val view = View(this)
        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            gravity = Gravity.START or Gravity.TOP
            width = 1
            height = 1
            packageName = this@ThemeInstallAccessibilityService.packageName
        }
        try {
            windowManager.addView(view, params)
            aliveOverlayView = view
            Timber.tag(TAG).d("Accessibility alive overlay attached")
        } catch (e: Throwable) {
            aliveOverlayView = null
            Timber.tag(TAG).e(e, "Failed to attach accessibility alive overlay")
        }
    }

    private fun removeAliveOverlayView() {
        val view = aliveOverlayView ?: return
        aliveOverlayView = null
        try {
            windowManager.removeView(view)
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "Failed to remove accessibility alive overlay")
        }
    }
}
