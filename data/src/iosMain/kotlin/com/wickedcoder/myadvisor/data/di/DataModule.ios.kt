package com.wickedcoder.myadvisor.data.di

import androidx.room.RoomDatabase
import com.wickedcoder.myadvisor.data.db.AppDatabase
import com.wickedcoder.myadvisor.data.db.appDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDatabaseModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> { appDatabaseBuilder() }
}
