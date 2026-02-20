package com.merak.core.os.shizuku.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.merak.core.os.reflect.ReflectManager
import com.merak.core.os.reflect.invoke
import com.merak.core.os.reflect.invokeStatic
import com.merak.core.os.shizuku.util.ShizukuHook
import com.merak.x.IUserService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber
import java.io.IOException

@SuppressLint("LogNotTimber")
class PrivilegedService() : IUserService.Stub(), KoinComponent {
    companion object {
        private const val TAG = "PrivilegedService"
        private const val SHELL_PATH = "/system/bin/sh"
    }

    private val context by inject<Context>()
    private val reflect by inject<ReflectManager>()
    private val shizukuHook by inject<ShizukuHook>()

    @Throws(RemoteException::class)
    override fun execArr(command: Array<String>): String {
        val shellCmd = arrayOf(SHELL_PATH, "-c", command.toShellCommand())

        return try {
            val process = Runtime.getRuntime().exec(shellCmd)
            readResult(process)
        } catch (e: IOException) {
            throw RemoteException(e.message)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RemoteException(e.message)
        }
    }

    override fun destroy() {
        // 如果需要，可以在这里执行清理操作
        Log.d(TAG, "PrivilegedService destroy called")
    }

    private fun Array<String>.toShellCommand(): String {
        return this.joinToString(" ") { arg ->
            // 如果参数包含空格或特殊字符，加上单引号保护（简单处理）
            if (arg.contains(" ") || arg.contains("'") || arg.contains("\"")) {
                "'" + arg.replace("'", "'\\''") + "'"
            } else {
                arg
            }
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun readResult(process: Process): String {
        // Read standard output and standard error respectively
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val error = process.errorStream.bufferedReader().use { it.readText() }

        // Wait for command execution to complete and get the exit code
        val exitCode = process.waitFor()

        // Check exit code. 0 typically represents success, any non-zero value represents failure.
        if (exitCode != 0) {
            throw IOException(error.ifBlank { "Exit code: $exitCode" })
        }

        // If successful, return the content of standard output
        return output
    }

    override fun setAccessibilityServiceState(componentName: ComponentName, enabled: Boolean) {
        val serviceString = componentName.flattenToString()

        try {
            // 1. Get current accessibility services
            val currentServicesStr = execArr(arrayOf("settings", "get", "secure", "enabled_accessibility_services")).trim()
            val currentServicesList = if (currentServicesStr == "null" || currentServicesStr.isEmpty()) {
                emptyList<String>()
            } else {
                currentServicesStr.split(":")
            }

            val newServicesList = currentServicesList.toMutableList()
            var isChanged = false

            // 2. Add or remove the target service
            if (enabled) {
                if (!newServicesList.contains(serviceString)) {
                    newServicesList.add(serviceString)
                    isChanged = true
                }
            } else {
                if (newServicesList.contains(serviceString)) {
                    newServicesList.remove(serviceString)
                    isChanged = true
                }
            }

            // 3. Write back changes if necessary
            if (isChanged) {
                val newServicesStr = newServicesList.joinToString(":")

                // Use 'delete' instead of 'put' when the list is empty
                if (newServicesStr.isEmpty()) {
                    execArr(arrayOf("settings", "delete", "secure", "enabled_accessibility_services"))
                } else {
                    execArr(arrayOf("settings", "put", "secure", "enabled_accessibility_services", newServicesStr))
                }

                // If enabling, ensure the master accessibility toggle is ON
                if (enabled) {
                    execArr(arrayOf("settings", "put", "secure", "accessibility_enabled", "1"))
                } else if (newServicesStr.isEmpty()) {
                    // Turn off the master toggle if no services are left
                    execArr(arrayOf("settings", "put", "secure", "accessibility_enabled", "0"))
                }

                val action = if (enabled) "enabled" else "disabled"
                Timber.i("Successfully $action accessibility service via Shell: $serviceString")
            } else {
                Timber.d("Accessibility service state is already correct, no shell command needed.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set accessibility service state via shell commands.")
            throw e
        }
    }

    @SuppressLint("PrivateApi")
    override fun grantRuntimePermission(packageName: String, permission: String, userId: Int) {
        try {
            // Wrap the original PackageManager binder with ShizukuBinderWrapper
            val pmBinder = SystemServiceHelper.getSystemService("package")
            val wrappedBinder = ShizukuBinderWrapper(pmBinder)

            // Resolve the Stub class and cast the wrapped binder
            val stubClass = Class.forName("android.content.pm.IPackageManager\$Stub")
            val iPackageManager = reflect.invokeStatic<Any>(
                name = "asInterface",
                clazz = stubClass,
                parameterTypes = arrayOf(IBinder::class.java),
                args = arrayOf(wrappedBinder)
            )

            // Invoke the grantRuntimePermission method
            if (iPackageManager != null) {
                reflect.invoke<Unit>(
                    obj = iPackageManager,
                    name = "grantRuntimePermission",
                    clazz = iPackageManager.javaClass,
                    parameterTypes = arrayOf(String::class.java, String::class.java, Int::class.javaPrimitiveType!!),
                    packageName, permission, userId
                )
                Timber.i("Granted %s to %s via ShizukuBinderWrapper", permission, packageName)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to grant %s", permission)
        }
    }

    @SuppressLint("PrivateApi")
    override fun setAppOpsMode(code: Int, uid: Int, packageName: String, mode: Int) {
        try {
            // Wrap the original AppOpsService binder with ShizukuBinderWrapper
            val appOpsBinder = SystemServiceHelper.getSystemService(Context.APP_OPS_SERVICE)
            val wrappedBinder = ShizukuBinderWrapper(appOpsBinder)

            // Resolve the Stub class and cast the wrapped binder
            val stubClass = Class.forName("com.android.internal.app.IAppOpsService\$Stub")
            val iAppOpsService = reflect.invokeStatic<Any>(
                name = "asInterface",
                clazz = stubClass,
                parameterTypes = arrayOf(IBinder::class.java),
                args = arrayOf(wrappedBinder)
            )

            // Invoke the setMode method
            if (iAppOpsService != null) {
                reflect.invoke<Unit>(
                    obj = iAppOpsService,
                    name = "setMode",
                    clazz = iAppOpsService.javaClass,
                    parameterTypes = arrayOf(
                        Int::class.javaPrimitiveType!!,
                        Int::class.javaPrimitiveType!!,
                        String::class.java,
                        Int::class.javaPrimitiveType!!
                    ),
                    code, uid, packageName, mode
                )
                Timber.i("Set AppOps mode %d to %d for %s via ShizukuBinderWrapper", code, mode, packageName)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set AppOps mode")
        }
    }
}