package com.merak.di.init

import com.merak.di.coreModule
import com.merak.di.logModule
import com.merak.di.settingsModule
import com.merak.di.viewModelModule

val appModules = listOf(
    coreModule,
    logModule,
    settingsModule,
    viewModelModule
)