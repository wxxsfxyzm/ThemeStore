package com.merak.di

import com.merak.core.installer.ThemeInstaller
import com.merak.core.os.reflect.ReflectManager
import com.merak.core.os.reflect.ReflectManagerImpl
import com.merak.core.os.shizuku.AutoAccessibilityManager
import com.merak.core.os.shizuku.PrivilegedManager
import com.merak.core.os.shizuku.util.ShizukuHook
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    // Theme Installer
    single { ThemeInstaller(androidContext()) }

    // Reflection Provider
    single<ReflectManager> { ReflectManagerImpl() }

    // Shizuku Privileged Manager
    single { PrivilegedManager(androidContext()) }

    // Register AutoAccessibilityManager to be created at Koin startup
    // Automatically calls init() when instantiated
    single {
        AutoAccessibilityManager(androidContext(), get(), get()).apply { init() }
    }

    // Shizuku Hook logic, Koin automatically resolves ReflectManager here using get()
    single { ShizukuHook(get()) }
}