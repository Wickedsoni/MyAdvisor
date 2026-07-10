package com.wickedcoder.myadvisor.domain.repository

import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.domain.model.Category
import com.wickedcoder.myadvisor.domain.model.Issuer
import com.wickedcoder.myadvisor.domain.model.Merchant
import com.wickedcoder.myadvisor.domain.model.MerchantFamily
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Read-only catalog zone: replaced wholesale by the import pipeline,
 * never mutated at runtime (ADR-002, data-model.md).
 */
interface CardCatalogRepository {
    suspend fun getAllCards(): List<Card>
    suspend fun getCard(cardId: String): Card?
    suspend fun getIssuers(): List<Issuer>
    suspend fun getCategories(): List<Category>
    suspend fun getMerchants(): List<Merchant>
    suspend fun getMerchantFamilies(): List<MerchantFamily>
}

/**
 * User zone: the user's owned cards. Survives catalog re-imports (ADR-002).
 */
interface UserCardsRepository {
    fun observeOwnedCards(): Flow<List<Card>>
    suspend fun addCard(cardId: String)
    suspend fun removeCard(cardId: String)
}

/** Backs the "Reward DB v1.0.4 · verified 10 Jul 2026" display. */
data class DatasetMeta(
    val schemaVersion: Int,
    val dataVersion: String,
    val generatedAt: LocalDate,
)

interface DatasetMetaRepository {
    suspend fun getDatasetMeta(): DatasetMeta?
}
