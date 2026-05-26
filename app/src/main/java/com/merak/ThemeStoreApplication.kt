package com.merak

import android.app.Application
import com.merak.di.init.appModules
import com.merak.util.CrashHandler
import com.merak.util.timber.AppLogcatTree
import com.merak.x.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

class ThemeStoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        HiddenApiBypass.addHiddenApiExemptions("")
        if (BuildConfig.DEBUG) Timber.plant(AppLogcatTree())
        CrashHandler.instance.init()
        startKoin {
            // Koin Android Logger
            androidLogger()
            // Koin Android Context
            androidContext(this@ThemeStoreApplication)
            // use modules
            modules(appModules)
        }
    }
}

