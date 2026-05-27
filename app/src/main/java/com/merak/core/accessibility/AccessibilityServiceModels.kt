package com.merak.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.graphics.drawable.Drawable

data class ManagedAccessibilityService(
    val id: String,
    val componentName: ComponentName,
    val packageLabel: String,
    val serviceLabel: String,
    val description: String,
    val icon: Drawable?,
    val isEnabled: Boolean,
    val isPinned: Boolean,
    val isDaemonEnabled: Boolean,
    val info: AccessibilityServiceInfo
)

data class AccessibilityServiceDetail(
    val serviceClass: String,
    val capabilities: String,
    val packageScope: String,
    val feedbackTypes: String,
    val eventTypes: String,
    val flags: String,
    val settingsComponent: ComponentName?
)
