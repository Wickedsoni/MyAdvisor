package com.wickedcoder.myadvisor.di

import com.wickedcoder.myadvisor.data.di.dataModule
import com.wickedcoder.myadvisor.data.di.platformDatabaseModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun appModules(): List<Module> = listOf(
    platformDatabaseModule,
    dataModule,
)

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModules())
    }
}
