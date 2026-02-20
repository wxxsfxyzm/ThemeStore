package com.merak.core.os.shizuku.util

import com.merak.core.os.shizuku.exception.ShizukuNotWorkException
import com.merak.core.os.shizuku.pool.Recyclable
import com.merak.core.os.shizuku.pool.ShizukuHookRecycler
import com.merak.core.os.shizuku.pool.ShizukuUserServiceRecycler
import com.merak.core.os.shizuku.service.UserService
import timber.log.Timber

private const val TAG = "PrivilegedService"

/**
 * Executes an action using Shizuku's UserService.
 *
 * @param useHookMode If true, runs in the current process using Shizuku hooking/spawning (lighter).
 * If false, runs in a separate privileged process (more stable for heavy tasks).
 */
fun useShizukuUserService(
    useHookMode: Boolean = false, // 默认为 false，保持原有行为（远程进程）
    action: (UserService) -> Unit
) {
    // 根据参数选择 Recycler
    val recycler: Recyclable<out UserService> = if (useHookMode) {
        Timber.tag(TAG).d("Processing Shizuku with recycler: ShizukuHookRecycler")
        ShizukuHookRecycler.make()
    } else {
        Timber.tag(TAG).d("Processing Shizuku with recycler: ShizukuUserServiceRecycler")
        ShizukuUserServiceRecycler.make()
    }

    try {
        recycler.use { action.invoke(it.entity) }
    } catch (e: IllegalStateException) {
        if (e.message?.contains("binder haven't been received") == true) {
            throw ShizukuNotWorkException("Shizuku service connection lost during privileged action.", e)
        }
        throw e
    }
}