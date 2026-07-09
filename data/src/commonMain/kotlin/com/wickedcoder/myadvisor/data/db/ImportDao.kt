package com.wickedcoder.myadvisor.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * The ONLY writer of the catalog zone (ADR-002/004): atomic replace inside
 * one transaction — delete-all catalog rows → insert new dataset → update
 * dataset_meta. No row-level diffing; the dataset is tens of cards.
 */
@Dao
interface ImportDao {

    @Transaction
    suspend fun replaceCatalog(
        issuers: List<IssuerEntity>,
        categories: List<CategoryEntity>,
        merchantFamilies: List<MerchantFamilyEntity>,
        merchants: List<MerchantEntity>,
        cards: List<CardEntity>,
        rules: List<RewardRuleEntity>,
        exclusions: List<ExclusionEntity>,
        offers: List<TemporaryOfferEntity>,
        meta: DatasetMetaEntity,
    ) {
        clearTemporaryOffers()
        clearExclusions()
        clearRewardRules()
        clearCards()
        clearMerchants()
        clearMerchantFamilies()
        clearCategories()
        clearIssuers()

        insertIssuers(issuers)
        insertCategories(categories)
        insertMerchantFamilies(merchantFamilies)
        insertMerchants(merchants)
        insertCards(cards)
        insertRewardRules(rules)
        insertExclusions(exclusions)
        insertTemporaryOffers(offers)
        upsertMeta(meta)
    }

    @Query("DELETE FROM issuers") suspend fun clearIssuers()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM merchant_families") suspend fun clearMerchantFamilies()
    @Query("DELETE FROM merchants") suspend fun clearMerchants()
    @Query("DELETE FROM cards") suspend fun clearCards()
    @Query("DELETE FROM reward_rules") suspend fun clearRewardRules()
    @Query("DELETE FROM exclusions") suspend fun clearExclusions()
    @Query("DELETE FROM temporary_offers") suspend fun clearTemporaryOffers()

    @Insert suspend fun insertIssuers(items: List<IssuerEntity>)
    @Insert suspend fun insertCategories(items: List<CategoryEntity>)
    @Insert suspend fun insertMerchantFamilies(items: List<MerchantFamilyEntity>)
    @Insert suspend fun insertMerchants(items: List<MerchantEntity>)
    @Insert suspend fun insertCards(items: List<CardEntity>)
    @Insert suspend fun insertRewardRules(items: List<RewardRuleEntity>)
    @Insert suspend fun insertExclusions(items: List<ExclusionEntity>)
    @Insert suspend fun insertTemporaryOffers(items: List<TemporaryOfferEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: DatasetMetaEntity)
}
