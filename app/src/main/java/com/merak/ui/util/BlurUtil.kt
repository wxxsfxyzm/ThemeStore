package com.merak.ui.util

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun WindowBlurEffect(useBlur: Boolean, blurRadius: Int = 30) {
    val view = LocalView.current
    // Find the window belonging to this view (could be a Dialog window or Activity window)
    val window = (view.parent as? View)?.context?.let {
        var currentContext = it
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return@let currentContext.window
            currentContext = currentContext.baseContext
        }
        null
    } ?: return

    SideEffect {
        if (useBlur) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = blurRadius
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 0
        }
        // Force update window attributes
        window.attributes = window.attributes
    }
}