package com.wickedcoder.myadvisor.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** A card with its rules and exclusions, loaded as one aggregate (ADR-003 load pattern). */
data class CardWithRules(
    @Embedded val card: CardEntity,
    @Relation(parentColumn = "id", entityColumn = "cardId")
    val rules: List<RewardRuleEntity>,
    @Relation(parentColumn = "id", entityColumn = "cardId")
    val exclusions: List<ExclusionEntity>,
)

@Dao
interface CatalogDao {
    @Transaction
    @Query("SELECT * FROM cards ORDER BY id")
    suspend fun getAllCardsWithRules(): List<CardWithRules>

    @Transaction
    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardWithRules(cardId: String): CardWithRules?

    @Query("SELECT * FROM issuers ORDER BY id")
    suspend fun getIssuers(): List<IssuerEntity>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM merchants ORDER BY id")
    suspend fun getMerchants(): List<MerchantEntity>
}

@Dao
interface UserCardsDao {
    @Query("SELECT * FROM user_cards ORDER BY sortOrder, cardId")
    fun observeUserCards(): Flow<List<UserCardEntity>>

    @Query("SELECT cardId FROM user_cards")
    suspend fun getOwnedCardIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(userCard: UserCardEntity)

    @Query("DELETE FROM user_cards WHERE cardId = :cardId")
    suspend fun remove(cardId: String)
}

@Dao
interface DatasetMetaDao {
    @Query("SELECT * FROM dataset_meta WHERE id = 1")
    suspend fun get(): DatasetMetaEntity?
}
