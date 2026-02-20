package com.merak.core.os.shizuku.pool

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.Keep
import com.merak.core.os.shizuku.service.PrivilegedService
import com.merak.core.os.shizuku.service.UserService
import com.merak.core.os.shizuku.util.requireShizukuPermissionGranted
import com.merak.di.init.processModules
import com.merak.x.BuildConfig
import com.merak.x.IUserService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import rikka.shizuku.Shizuku
import timber.log.Timber
import kotlin.system.exitProcess

object ShizukuUserServiceRecycler :
    Recycler<ShizukuUserServiceRecycler.UserServiceProxy>(),
    KoinComponent {
    class UserServiceProxy(val service: IUserService) : UserService {
        override val privileged: IUserService = service

        override fun close() = service.destroy()
    }

    class ShizukuUserService @Keep constructor(context: Context) : IUserService.Stub() {
        init {
            if (BuildConfig.DEBUG && Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
            startKoin {
                modules(processModules)
                androidContext(context)
            }
        }

        private val privileged = PrivilegedService()

        override fun execArr(command: Array<String>): String = privileged.execArr(command)

        override fun setAccessibilityServiceState(componentName: ComponentName, enabled: Boolean) =
            privileged.setAccessibilityServiceState(componentName, enabled)

        override fun grantRuntimePermission(packageName: String, permission: String, userId: Int) =
            privileged.grantRuntimePermission(packageName, permission, userId)

        override fun setAppOpsMode(code: Int, uid: Int, packageName: String, mode: Int) =
            privileged.setAppOpsMode(code, uid, packageName, mode)

        override fun destroy() {
            exitProcess(0)
        }
    }

    private val context by inject<Context>()

    override fun onMake(): UserServiceProxy = runBlocking {
        requireShizukuPermissionGranted {
            onInnerMake()
        }
    }

    private suspend fun onInnerMake(): UserServiceProxy = callbackFlow {
        Shizuku.bindUserService(
            Shizuku.UserServiceArgs(
                ComponentName(
                    context, ShizukuUserService::class.java
                )
            ).processNameSuffix("shizuku_privileged"), object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    trySend(UserServiceProxy(IUserService.Stub.asInterface(service)))
                    service?.linkToDeath({
                        if (entity?.service?.asBinder() == service) recycleForcibly()
                    }, 0)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    close()
                }
            })
        awaitClose { }
    }.first()
}
