package com.merak.core.os.shizuku.service

import com.merak.x.IUserService
import java.io.Closeable

interface UserService : Closeable {
    val privileged: IUserService
}