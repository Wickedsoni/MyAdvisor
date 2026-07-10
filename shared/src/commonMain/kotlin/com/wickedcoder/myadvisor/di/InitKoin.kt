package com.wickedcoder.myadvisor.di

import com.wickedcoder.myadvisor.data.ComposeResourceDatasetSource
import com.wickedcoder.myadvisor.data.di.dataModule
import com.wickedcoder.myadvisor.data.di.platformDatabaseModule
import com.wickedcoder.myadvisor.data.importer.DatasetSource
import com.wickedcoder.myadvisor.domain.engine.GetRecommendationsUseCase
import com.wickedcoder.myadvisor.ui.cards.CardsViewModel
import com.wickedcoder.myadvisor.ui.recommend.RecommendViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {
    single<DatasetSource> { ComposeResourceDatasetSource() }
    factoryOf(::GetRecommendationsUseCase)
    viewModelOf(::CardsViewModel)
    viewModelOf(::RecommendViewModel)
}

fun appModules(): List<Module> = listOf(
    platformDatabaseModule,
    dataModule,
    appModule,
)

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModules())
    }
}
