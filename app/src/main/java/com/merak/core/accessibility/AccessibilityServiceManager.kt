package com.merak.core.accessibility

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.merak.core.os.shizuku.PrivilegedManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AccessibilityServiceManager(
    private val context: Context,
    private val privilegedManager: PrivilegedManager
) {
    private val packageManager = context.packageManager

    suspend fun loadServices(
        pinnedIds: List<String>,
        daemonIds: Set<String>
    ): List<ManagedAccessibilityService> = withContext(Dispatchers.IO) {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledValue = currentEnabledServicesValue()
        val services = accessibilityManager.installedAccessibilityServiceList.mapNotNull { info ->
            val component = info.componentNameCompat() ?: return@mapNotNull null
            val packageLabel = runCatching {
                packageManager.getApplicationInfo(component.packageName, PackageManager.GET_META_DATA)
                    .loadLabel(packageManager)
                    .toString()
            }.getOrDefault(component.packageName)
            val serviceLabel = runCatching {
                packageManager.getServiceInfo(component, PackageManager.MATCH_DEFAULT_ONLY)
                    .loadLabel(packageManager)
                    .toString()
            }.getOrDefault(packageLabel)

            ManagedAccessibilityService(
                id = info.id,
                componentName = component,
                packageLabel = packageLabel,
                serviceLabel = serviceLabel,
                description = info.loadDescription(packageManager).orEmpty(),
                icon = runCatching { packageManager.getApplicationIcon(component.packageName) }.getOrNull(),
                isEnabled = isServiceEnabled(enabledValue, info.id),
                isPinned = pinnedIds.contains(info.id),
                isDaemonEnabled = daemonIds.contains(info.id),
                info = info
            )
        }

        services.sortedWith(
            compareByDescending<ManagedAccessibilityService> { it.isPinned }
                .thenBy { service ->
                    pinnedIds.indexOf(service.id).let { if (it == -1) Int.MAX_VALUE else it }
                }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.packageLabel }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.serviceLabel }
        )
    }

    fun hasSecureSettingsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun currentEnabledServicesValue(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
    }

    fun isServiceEnabled(enabledValue: String, serviceId: String): Boolean {
        if (enabledValue.isBlank()) return false
        if (enabledValue.split(":").any { it == serviceId }) return true
        val packageName = serviceId.substringBefore("/", missingDelimiterValue = "")
        val className = serviceId.substringAfter("/", missingDelimiterValue = "")
        if (packageName.isBlank() || className.isBlank()) return false
        val legacyId = if (className.startsWith(".")) {
            "$packageName/$packageName$className"
        } else {
            "$packageName/$className"
        }
        return enabledValue.split(":").any { it == legacyId }
    }

    suspend fun setServiceEnabled(componentName: ComponentName, enabled: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            privilegedManager.setAccessibilityServiceState(componentName, enabled)
        }

    suspend fun restoreDaemonServices(serviceIds: Set<String>, showToast: (String) -> Unit = {}): Boolean =
        withContext(Dispatchers.IO) {
            if (serviceIds.isEmpty() || !hasSecureSettingsPermission()) return@withContext false

            val installed = loadServices(emptyList(), serviceIds)
                .associateBy { it.id }
            val enabledValue = currentEnabledServicesValue()
            var restored = false
            serviceIds.forEach { id ->
                val service = installed[id] ?: return@forEach
                if (!isServiceEnabled(enabledValue, id)) {
                    if (privilegedManager.setAccessibilityServiceState(service.componentName, true)) {
                        restored = true
                        showToast(service.packageLabel)
                    }
                }
            }
            restored
        }

    fun buildDetail(info: AccessibilityServiceInfo): AccessibilityServiceDetail {
        val component = info.componentNameCompat()
        val settingsComponent = info.settingsActivityName
            ?.takeIf { it.isNotBlank() }
            ?.let { settingsName -> component?.let { ComponentName(it.packageName, settingsName) } }

        return AccessibilityServiceDetail(
            serviceClass = info.id,
            capabilities = describeCapabilities(info.capabilities),
            packageScope = info.packageNames?.joinToString("\n").takeUnless { it.isNullOrBlank() } ?: "全局生效",
            feedbackTypes = describeFeedbackTypes(info.feedbackType),
            eventTypes = describeEventTypes(info.eventTypes),
            flags = describeFlags(info.flags),
            settingsComponent = settingsComponent
        )
    }

    fun settingsIntent(componentName: ComponentName): Intent = Intent().setComponent(componentName)

    private fun AccessibilityServiceInfo.componentNameCompat(): ComponentName? {
        ComponentName.unflattenFromString(id)?.let { return it }
        val serviceInfo = resolveInfo?.serviceInfo ?: return null
        return ComponentName(serviceInfo.packageName, serviceInfo.name)
    }

    private fun describeCapabilities(capabilities: Int): String = buildString {
        appendIf(capabilities, AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES, "执行手势")
        appendIf(capabilities, AccessibilityServiceInfo.CAPABILITY_CAN_CONTROL_MAGNIFICATION, "控制显示器放大率")
        appendIf(capabilities, AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS, "监听和拦截按键事件")
        appendIf(capabilities, AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY, "请求增强的 Web 辅助功能")
        appendIf(capabilities, AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION, "请求触摸探索模式")
        appendIf(capabilities, AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT, "读取屏幕内容")
    }.ifBlank { "无" }

    private fun describeFeedbackTypes(feedbackType: Int): String = buildString {
        appendIf(feedbackType, AccessibilityServiceInfo.FEEDBACK_BRAILLE, "盲文反馈")
        appendIf(feedbackType, AccessibilityServiceInfo.FEEDBACK_GENERIC, "通用反馈")
        appendIf(feedbackType, AccessibilityServiceInfo.FEEDBACK_VISUAL, "视觉反馈")
        appendIf(feedbackType, AccessibilityServiceInfo.FEEDBACK_AUDIBLE, "可听反馈")
        appendIf(feedbackType, AccessibilityServiceInfo.FEEDBACK_HAPTIC, "触觉反馈")
        appendIf(feedbackType, AccessibilityServiceInfo.FEEDBACK_SPOKEN, "口头反馈")
    }.ifBlank { "无" }

    private fun describeEventTypes(eventTypes: Int): String = buildString {
        appendIf(eventTypes, AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT, "正在阅读屏幕上下文")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED, "控件上下文点击")
        appendIf(eventTypes, AccessibilityEvent.TYPE_WINDOWS_CHANGED, "窗口更改")
        appendIf(eventTypes, AccessibilityEvent.TYPE_TOUCH_INTERACTION_END, "触摸交互结束")
        appendIf(eventTypes, AccessibilityEvent.TYPE_TOUCH_INTERACTION_START, "触摸交互开始")
        appendIf(eventTypes, AccessibilityEvent.TYPE_GESTURE_DETECTION_END, "手势检测结束")
        appendIf(eventTypes, AccessibilityEvent.TYPE_GESTURE_DETECTION_START, "手势检测开始")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY, "遍历视图文本")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED, "清除无障碍焦点")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, "获得无障碍焦点")
        appendIf(eventTypes, AccessibilityEvent.TYPE_ANNOUNCEMENT, "应用发布公告")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED, "选中文本更改")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_SCROLLED, "视图滚动")
        appendIf(eventTypes, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, "窗口内容更改")
        appendIf(eventTypes, AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END, "触摸探索手势结束")
        appendIf(eventTypes, AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START, "触摸探索手势开始")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, "文本改变")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_FOCUSED, "控件获得焦点")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_SELECTED, "控件被选取")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_LONG_CLICKED, "长按控件")
        appendIf(eventTypes, AccessibilityEvent.TYPE_VIEW_CLICKED, "点击控件")
    }.ifBlank { "无" }

    private fun describeFlags(flags: Int): String = buildString {
        appendIf(flags, AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS, "访问所有交互式窗口内容")
        appendIf(flags, AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS, "监听和拦截按键事件")
        appendIf(flags, AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS, "获取控件 ID")
        appendIf(flags, AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY, "启用 Web 辅助功能增强")
        appendIf(flags, AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE, "请求触摸探索模式")
        appendIf(flags, AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS, "查询不重要内容")
        appendIf(flags, AccessibilityServiceInfo.DEFAULT, "默认")
    }.ifBlank { "无" }

    private fun StringBuilder.appendIf(value: Int, flag: Int, label: String) {
        if (value and flag != 0) {
            if (isNotEmpty()) append('\n')
            append(label)
        }
    }

    private companion object {
        const val TAG = "AccessibilityServiceManager"
    }
}
