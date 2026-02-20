package com.merak.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Base64
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.merak.util.timber.LogFormatter
import com.merak.x.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Broadcast interceptor service.
 * Dynamically registers receiver to intercept MIUI scheduled broadcasts and prevent theme from reverting to default.
 */
@SuppressLint("AccessibilityPolicy")
class ThemeInstallAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_SERVICE_UP = "com.merak.action_Service_UP"
        const val ACTION_SERVICE_DOWN = "com.merak.action_Service_DOWN"

        // Log Tag, used for UI parsing
        private const val TAG = "ThemeAccessibility"

        // "miui.intent.action.CHECK_TIME_UP"
        private val ALARM_ACTION = String(
            Base64.decode("bWl1aS5pbnRlbnQuYWN0aW9uLkNIRUNLX1RJTUVfVVA=", Base64.DEFAULT)
        )

        @Volatile
        private var isConnected = false

        @Volatile
        private var st_connectedTime = ""

        @Volatile
        private var st_receiveTime = ""

        // Static method: Check if service is enabled
        fun isAccessibilityServiceEnabled(
            context: Context,
            clazz: Class<out AccessibilityService>
        ): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false

            val enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

            val myComponentName = "${context.packageName}/${clazz.name}"

            return enabledServices.any {
                "${it.resolveInfo.serviceInfo.packageName}/${it.resolveInfo.serviceInfo.name}" == myComponentName
            }
        }

        fun isConnected(): Boolean = isConnected
        fun getConnectedTime(): String = st_connectedTime
        fun getReceiveTime(): String = st_receiveTime
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Dynamic broadcast receiver for external system intents only
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || context == null) return

            if (intent.action == ALARM_ACTION) {
                // Intercept broadcast to prevent system from reverting to default theme
                // Wrapped in try-catch in case it's not an ordered broadcast in newer MIUI versions
                try {
                    abortBroadcast()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Intercept failed | Broadcast might not be ordered")
                }

                val now = Date()
                st_receiveTime = dateFormat.format(now)

                // Use LogFormatter to record interception log
                LogFormatter.logAlarmIntercept(
                    title = context.getString(R.string.alarm_intercept_title),
                    content = context.getString(R.string.alarm_intercept_content, ALARM_ACTION, st_receiveTime)
                )

                // Refresh KeepAliveService notification to show latest intercept count
                KeepAliveService.requestRefresh(context.applicationContext)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle specific accessibility events, just utilize the service lifecycle
    }

    override fun onInterrupt() {
        Timber.tag(TAG).w("Service status | Accessibility service interrupted by system")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.tag(TAG).i("Service status | Accessibility service connected")

        // Configure service info programmatically to ensure flags are active
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info

        KeepAliveService.requestRefresh(applicationContext)
        sendServiceUpBroadcast()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("Service lifecycle | onCreate - Service starting")

        if (isConnected) {
            Timber.tag(TAG).w("Service status | Service already connected, skipping initialization")
            return
        }

        st_connectedTime = dateFormat.format(Date())

        // Register high priority broadcast receiver for system intents
        val intentFilter = IntentFilter().apply {
            addAction(ALARM_ACTION)
            // Optional: Listen to screen off to prevent being killed
            addAction(Intent.ACTION_SCREEN_OFF)
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }

        try {
            // Must use RECEIVER_EXPORTED to receive broadcasts from MIUI system packages
            registerReceiver(mBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Registration failed | Broadcast receiver registration exception")
        }

        isConnected = true
        sendServiceUpBroadcast()
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("Service lifecycle | onDestroy - Service destroyed")

        if (isConnected) {
            try {
                unregisterReceiver(mBroadcastReceiver)
            } catch (e: Exception) {
                // Ignore receiver not registered exception
            }
            isConnected = false
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.tag(TAG).d("Service lifecycle | onUnbind - Service unbound")
        sendServiceDownBroadcast()
        return super.onUnbind(intent)
    }

    private fun sendServiceUpBroadcast() {
        try {
            val intent = Intent(ACTION_SERVICE_UP).apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Broadcast failed | ServiceUp broadcast exception")
        }
    }

    private fun sendServiceDownBroadcast() {
        try {
            val intent = Intent(ACTION_SERVICE_DOWN).apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Broadcast failed | ServiceDown broadcast exception")
        }
    }
}
