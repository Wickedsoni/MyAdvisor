package com.wickedcoder.myadvisor.data.di

import com.wickedcoder.myadvisor.data.db.AppDatabase
import com.wickedcoder.myadvisor.data.db.createAppDatabase
import com.wickedcoder.myadvisor.data.repository.RoomCardCatalogRepository
import com.wickedcoder.myadvisor.data.repository.RoomDatasetMetaRepository
import com.wickedcoder.myadvisor.data.repository.RoomUserCardsRepository
import com.wickedcoder.myadvisor.domain.repository.CardCatalogRepository
import com.wickedcoder.myadvisor.domain.repository.DatasetMetaRepository
import com.wickedcoder.myadvisor.domain.repository.UserCardsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/** Provides the platform RoomDatabase.Builder (Android needs a Context; iOS doesn't). */
expect val platformDatabaseModule: Module

val dataModule = module {
    single<AppDatabase> { createAppDatabase(get()) }
    single { get<AppDatabase>().catalogDao() }
    single { get<AppDatabase>().userCardsDao() }
    single { get<AppDatabase>().datasetMetaDao() }

    single<CardCatalogRepository> { RoomCardCatalogRepository(get()) }
    single<UserCardsRepository> { RoomUserCardsRepository(get(), get()) }
    single<DatasetMetaRepository> { RoomDatasetMetaRepository(get()) }
}
