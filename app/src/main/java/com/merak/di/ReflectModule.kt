package com.merak.di

import com.merak.core.os.reflect.ReflectManager
import com.merak.core.os.reflect.ReflectManagerImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val reflectModule = module {
    singleOf(::ReflectManagerImpl) { bind<ReflectManager>() }
}
