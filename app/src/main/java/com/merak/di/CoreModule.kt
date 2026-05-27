package com.merak.di

import com.merak.core.installer.ThemeInstallerManager
import com.merak.core.accessibility.AccessibilityServiceManager
import com.merak.core.os.shizuku.AutoAccessibilityManager
import com.merak.core.os.shizuku.PrivilegedManager
import com.merak.core.os.shizuku.util.ShizukuHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreModule = module {
    single(named("AppScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    singleOf(::ThemeInstallerManager)
    singleOf(::AccessibilityServiceManager)

    singleOf(::PrivilegedManager)

    single(createdAtStart = true) {
        AutoAccessibilityManager(get(), get(), get()).apply { init() }
    }

    singleOf(::ShizukuHook)
}
