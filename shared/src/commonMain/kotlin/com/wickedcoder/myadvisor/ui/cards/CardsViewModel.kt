package com.wickedcoder.myadvisor.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wickedcoder.myadvisor.data.importer.DatasetImporter
import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.domain.model.Issuer
import com.wickedcoder.myadvisor.domain.repository.CardCatalogRepository
import com.wickedcoder.myadvisor.domain.repository.DatasetMetaRepository
import com.wickedcoder.myadvisor.domain.repository.UserCardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CardsViewModel(
    private val importer: DatasetImporter,
    private val catalogRepository: CardCatalogRepository,
    private val userCardsRepository: UserCardsRepository,
    private val datasetMetaRepository: DatasetMetaRepository,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val error: String? = null,
        val catalog: List<Card> = emptyList(),
        val issuersById: Map<String, Issuer> = emptyMap(),
        val ownedCardIds: Set<String> = emptySet(),
        val dataVersionLabel: String? = null,
    ) {
        val ownedCards: List<Card> get() = catalog.filter { it.id in ownedCardIds }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                importer.importBundledDatasetIfNewer()
                val meta = datasetMetaRepository.getDatasetMeta()
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        catalog = catalogRepository.getAllCards(),
                        issuersById = catalogRepository.getIssuers().associateBy { it.id },
                        dataVersionLabel = meta?.let { "Reward DB v${it.dataVersion} · verified ${it.generatedAt}" },
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load card data") }
            }
        }
        viewModelScope.launch {
            userCardsRepository.observeOwnedCards().collect { owned ->
                _state.update { state -> state.copy(ownedCardIds = owned.map { it.id }.toSet()) }
            }
        }
    }

    fun addCard(cardId: String) {
        viewModelScope.launch { userCardsRepository.addCard(cardId) }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch { userCardsRepository.removeCard(cardId) }
    }
}
