package com.wickedcoder.myadvisor.data.repository

import com.wickedcoder.myadvisor.data.db.CatalogDao
import com.wickedcoder.myadvisor.data.db.DatasetMetaDao
import com.wickedcoder.myadvisor.data.db.UserCardEntity
import com.wickedcoder.myadvisor.data.db.UserCardsDao
import com.wickedcoder.myadvisor.data.mapper.toDomain
import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.domain.model.Category
import com.wickedcoder.myadvisor.domain.model.Issuer
import com.wickedcoder.myadvisor.domain.model.Merchant
import com.wickedcoder.myadvisor.domain.model.MerchantFamily
import com.wickedcoder.myadvisor.domain.repository.CardCatalogRepository
import com.wickedcoder.myadvisor.domain.repository.DatasetMeta
import com.wickedcoder.myadvisor.domain.repository.DatasetMetaRepository
import com.wickedcoder.myadvisor.domain.repository.UserCardsRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RoomCardCatalogRepository(
    private val catalogDao: CatalogDao,
) : CardCatalogRepository {
    override suspend fun getAllCards(): List<Card> =
        catalogDao.getAllCardsWithRules().map { it.toDomain() }

    override suspend fun getCard(cardId: String): Card? =
        catalogDao.getCardWithRules(cardId)?.toDomain()

    override suspend fun getIssuers(): List<Issuer> =
        catalogDao.getIssuers().map { it.toDomain() }

    override suspend fun getCategories(): List<Category> =
        catalogDao.getCategories().map { it.toDomain() }

    override suspend fun getMerchants(): List<Merchant> =
        catalogDao.getMerchants().map { it.toDomain() }

    override suspend fun getMerchantFamilies(): List<MerchantFamily> =
        catalogDao.getMerchantFamilies().map { it.toDomain() }
}

class RoomUserCardsRepository(
    private val userCardsDao: UserCardsDao,
    private val catalogDao: CatalogDao,
) : UserCardsRepository {
    override fun observeOwnedCards(): Flow<List<Card>> =
        userCardsDao.observeUserCards().map { owned ->
            owned.mapNotNull { catalogDao.getCardWithRules(it.cardId)?.toDomain() }
        }

    override suspend fun addCard(cardId: String) {
        userCardsDao.add(
            UserCardEntity(
                cardId = cardId,
                addedAt = Clock.System.now().toString(),
                sortOrder = 0, // insertion order refinement lands with edit/reorder (Should-scope)
            ),
        )
    }

    override suspend fun removeCard(cardId: String) {
        userCardsDao.remove(cardId)
    }
}

class RoomDatasetMetaRepository(
    private val datasetMetaDao: DatasetMetaDao,
) : DatasetMetaRepository {
    override suspend fun getDatasetMeta(): DatasetMeta? =
        datasetMetaDao.get()?.let {
            DatasetMeta(
                schemaVersion = it.schemaVersion,
                dataVersion = it.dataVersion,
                generatedAt = LocalDate.parse(it.generatedAt),
            )
        }
}
