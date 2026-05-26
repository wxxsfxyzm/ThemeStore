package com.merak.core.os.shizuku.util

// Update imports to the new ReflectManager location
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.IActivityManager
import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import com.android.internal.app.IAppOpsService
import com.merak.core.os.reflect.ReflectManager
import com.merak.core.os.reflect.getStaticValue
import com.merak.core.os.reflect.getValue
import com.merak.core.os.shizuku.exception.ShizukuNotWorkException
import com.merak.x.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import rikka.sui.Sui
import timber.log.Timber
import java.lang.reflect.Field

suspend fun <T> requireShizukuPermissionGranted(action: suspend () -> T): T {
    callbackFlow {
        Sui.init(BuildConfig.APPLICATION_ID)
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            send(Unit)
            awaitClose()
        } else {
            val requestCode = (Int.MIN_VALUE..Int.MAX_VALUE).random()
            val listener =
                Shizuku.OnRequestPermissionResultListener { _requestCode, grantResult ->
                    if (_requestCode != requestCode) return@OnRequestPermissionResultListener
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                        trySend(Unit)
                    else close(Exception("sui/shizuku permission denied"))
                }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(requestCode)
            awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
        }
    }.catch {
        throw ShizukuNotWorkException(it)
    }.first()

    return action()
}

class ShizukuContext(base: Context) : ContextWrapper(base) {
    override fun getOpPackageName(): String {
        return "com.android.shell"
    }

    override fun getAttributionSource(): AttributionSource {
        val shellUid = Shizuku.getUid()
        val builder = AttributionSource.Builder(shellUid)
            .setPackageName("com.android.shell")

        builder.setPid(Process.INVALID_PID)

        return builder.build()
    }
}

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
class ShizukuHook(private val reflectManager: ReflectManager) {
    companion object {
        private const val TAG = "ShizukuHook"
    }

    // 1. Hook ActivityManager
    val hookedActivityManager: IActivityManager by lazy {
        Timber.tag(TAG).d("Creating on-demand hooked IActivityManager...")
        val amSingleton = reflectManager.getStaticValue<Any>("IActivityManagerSingleton", ActivityManager::class.java)
            ?: throw NullPointerException("Failed to retrieve IActivityManagerSingleton")
        val singletonClass = Class.forName("android.util.Singleton")

        val originalAM = reflectManager.getValue<IActivityManager>(amSingleton, "mInstance", singletonClass)
            ?: throw NullPointerException("Failed to retrieve mInstance from Singleton")

        val wrapper = ShizukuBinderWrapper(originalAM.asBinder())
        IActivityManager.Stub.asInterface(wrapper).also {
            Timber.tag(TAG).i("On-demand hooked IActivityManager created.")
        }
    }

    // 2. Hook PackageManager
    val hookedPackageManager: IPackageManager by lazy {
        Timber.tag(TAG).d("Creating on-demand hooked IPackageManager...")
        val originalBinder = SystemServiceHelper.getSystemService("package")
        val originalPM = IPackageManager.Stub.asInterface(originalBinder)

        val wrapper = ShizukuBinderWrapper(originalPM.asBinder())
        IPackageManager.Stub.asInterface(wrapper).also {
            Timber.tag(TAG).i("On-demand hooked IPackageManager created.")
        }
    }

    // 3. Hook AppOpsService
    val hookedAppOpsService: IAppOpsService by lazy {
        Timber.tag(TAG).d("Creating on-demand hooked IAppOpsService...")

        val originalBinder = SystemServiceHelper.getSystemService(Context.APP_OPS_SERVICE)
        val originalAppOps = IAppOpsService.Stub.asInterface(originalBinder)

        val wrapper = ShizukuBinderWrapper(originalAppOps.asBinder())
        IAppOpsService.Stub.asInterface(wrapper).also {
            Timber.tag(TAG).i("On-demand hooked IAppOpsService created.")
        }
    }

    // 4. Hook SettingsBinder
    val hookedSettingsBinder: IBinder? by lazy {
        Timber.tag(TAG).d("Creating on-demand hooked Settings Binder...")
        try {
            val info = reflectManager.resolveSettingsBinder(Settings.Secure::class.java) ?: return@lazy null
            ShizukuBinderWrapper(info.originalBinder).also {
                Timber.tag(TAG).i("On-demand hooked Settings Binder created.")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to create hooked Settings Binder")
            null
        }
    }
}

data class SettingsReflectionInfo(
    val provider: Any,
    val remoteField: Field,
    val originalBinder: IBinder
)

fun ReflectManager.resolveSettingsBinder(settingsClass: Class<*>): SettingsReflectionInfo? {
    val holder = getStaticValue<Any>("sProviderHolder", settingsClass) ?: return null
    val provider = getValue<Any>(holder, "mContentProvider") ?: return null
    val remoteField = getDeclaredField("mRemote", provider.javaClass) ?: return null
    val originalBinder = remoteField.get(provider) as? IBinder ?: return null

    return SettingsReflectionInfo(provider, remoteField, originalBinder)
}
