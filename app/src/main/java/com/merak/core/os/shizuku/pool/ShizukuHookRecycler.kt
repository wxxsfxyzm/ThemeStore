package com.merak.core.os.shizuku.pool

import com.merak.core.os.shizuku.service.PrivilegedService
import com.merak.core.os.shizuku.service.UserService
import com.merak.core.os.shizuku.util.requireShizukuPermissionGranted
import com.merak.x.IUserService
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import timber.log.Timber

object ShizukuHookRecycler : Recycler<ShizukuHookRecycler.HookedUserService>(), KoinComponent {
    class HookedUserService : UserService, KoinComponent {

        override val privileged: IUserService by lazy {
            PrivilegedService()
        }

        override fun close() {
            Timber.tag("ShizukuHookRecycler").d("close() called, no IPC destruction needed in hook mode.")
        }
    }

    override fun onMake(): HookedUserService = runBlocking {
        requireShizukuPermissionGranted {
            HookedUserService()
        }
    }
}