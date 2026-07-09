package com.wickedcoder.myadvisor.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

const val DATABASE_NAME = "myadvisor.db"

@Database(
    entities = [
        IssuerEntity::class,
        CategoryEntity::class,
        MerchantFamilyEntity::class,
        MerchantEntity::class,
        CardEntity::class,
        RewardRuleEntity::class,
        ExclusionEntity::class,
        TemporaryOfferEntity::class,
        UserCardEntity::class,
        DatasetMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun userCardsDao(): UserCardsDao
    abstract fun datasetMetaDao(): DatasetMetaDao
    abstract fun importDao(): ImportDao
}

// The Room KMP compiler generates the actual implementations per target.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * Builds the database from a platform-supplied builder (see
 * `platformDatabaseModule` — Android needs a Context for the db path, iOS
 * doesn't, so the builder itself is the platform seam).
 */
fun createAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
