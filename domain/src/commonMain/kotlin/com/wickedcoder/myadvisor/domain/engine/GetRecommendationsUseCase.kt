package com.wickedcoder.myadvisor.domain.engine

import com.wickedcoder.myadvisor.domain.repository.CardCatalogRepository
import com.wickedcoder.myadvisor.domain.repository.DatasetMetaRepository
import com.wickedcoder.myadvisor.domain.repository.UserCardsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * Loads full aggregates (ADR-003 load pattern) and hands them to the pure
 * engine. All I/O lives here; the engine never queries.
 */
class GetRecommendationsUseCase(
    private val catalogRepository: CardCatalogRepository,
    private val userCardsRepository: UserCardsRepository,
    private val datasetMetaRepository: DatasetMetaRepository,
) {
    suspend operator fun invoke(query: PurchaseQuery, today: LocalDate): List<Recommendation> {
        val ownedCards = userCardsRepository.observeOwnedCards().first()
        if (ownedCards.isEmpty()) return emptyList()

        val engine = DefaultRecommendationEngine(
            merchantsById = catalogRepository.getMerchants().associateBy { it.id },
            categoriesById = catalogRepository.getCategories().associateBy { it.id },
            familiesById = catalogRepository.getMerchantFamilies().associateBy { it.id },
            dataVersion = datasetMetaRepository.getDatasetMeta()?.dataVersion ?: "unknown",
            today = today,
        )
        return engine.recommend(query, ownedCards)
    }
}
