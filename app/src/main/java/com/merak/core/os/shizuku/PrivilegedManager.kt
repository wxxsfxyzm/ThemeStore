package com.merak.core.os.shizuku

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import com.merak.core.os.shizuku.util.useShizukuUserService
import com.merak.service.ThemeInstallAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuProvider
import timber.log.Timber
import android.os.Process as AndroidProcess

/**
 * A centralized manager for handling all privileged actions.
 * * Dependencies like Context are injected via constructor to maintain
 * decouple from the DI framework and improve testability.
 */
class PrivilegedManager(private val context: Context) {
    companion object {
        private const val OP_MANAGE_EXTERNAL_STORAGE = 92
        private const val MODE_ALLOWED = 0
    }

    /**
     * Executes a shell command array (safer).
     */
    fun execArr(command: Array<String>): String {

        var result = ""
        useShizukuUserService {
            try {
                result = it.privileged.execArr(command)
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute array command via IPC: ${command.joinToString(" ")}")
                result = e.message ?: "Execution failed"
            }
        }
        return result
    }

    fun setAccessibilityServiceState(enabled: Boolean) {
        ShizukuProvider.requestBinderForNonProviderProcess(context)
        val componentName = ComponentName(context, ThemeInstallAccessibilityService::class.java)

        useShizukuUserService { userService ->
            try {
                // Now passing the 'enabled' parameter to the IPC call
                userService.privileged.setAccessibilityServiceState(componentName, enabled)
                val action = if (enabled) "开启" else "关闭"
                Timber.i("已请求${action}无障碍服务: ${componentName.flattenToShortString()}")
            } catch (e: Exception) {
                Timber.e(e, "设置无障碍服务状态失败")
            }
        }
    }

    fun grantStorageAndNotificationPermissions() {
        val packageName = context.packageName

        try {
            val uid = AndroidProcess.myUid()
            val userId = uid / 100000

            useShizukuUserService(true) { userService ->
                try {
                    // 1. Grant Notification Permission via PackageManager
                    userService.privileged.grantRuntimePermission(
                        packageName,
                        Manifest.permission.POST_NOTIFICATIONS,
                        userId
                    )

                    // 2. Grant Storage Permission via AppOps
                    userService.privileged.setAppOpsMode(OP_MANAGE_EXTERNAL_STORAGE, uid, packageName, MODE_ALLOWED)

                    Timber.i("Storage and Notification permissions granted successfully")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to execute permission grants via IPC")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get package uid")
        }
    }

    /**
     * Request a writable file descriptor from the Shizuku process
     */
    suspend fun openRestrictedTargetFile(targetPath: String): ParcelFileDescriptor? {
        return withContext(Dispatchers.IO) {
            var pfd: ParcelFileDescriptor? = null
            // We use the default UserService (remote process) for file operations
            useShizukuUserService { userService ->
                pfd = userService.privileged.openRestrictedFile(targetPath)
            }
            pfd
        }
    }

    /**
     * Start an activity privileged via IPC
     */
    fun startActivityPrivileged(intent: Intent): Boolean {
        var success = false
        useShizukuUserService(true) { userService ->
            try {
                success = userService.privileged.startActivityPrivileged(intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start activity privileged via IPC")
            }
        }
        return success
    }
}