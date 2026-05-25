package com.merak.di

import com.merak.util.timber.LogController
import org.koin.dsl.module

val logModule = module {
    single(createdAtStart = true) {
        LogController(get(), get()).apply { init() }
    }
}
