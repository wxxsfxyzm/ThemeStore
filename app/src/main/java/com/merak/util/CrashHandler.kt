package com.merak.util

import android.os.Process
import com.merak.util.timber.LogFormatter
import kotlin.system.exitProcess

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        LogFormatter.logCrash(e)

        // 给一点时间让日志写入文件 (因为是异步 Channel)
        try {
            Thread.sleep(500)
        } catch (ignored: InterruptedException) {
        }

        // 交给系统默认处理（通常是杀进程）
        defaultHandler?.uncaughtException(t, e) ?: run {
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    companion object {
        val instance = CrashHandler()
    }
}