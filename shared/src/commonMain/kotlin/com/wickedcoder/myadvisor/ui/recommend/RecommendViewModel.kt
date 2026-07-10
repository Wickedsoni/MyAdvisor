package com.wickedcoder.myadvisor.ui.recommend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wickedcoder.myadvisor.domain.engine.GetRecommendationsUseCase
import com.wickedcoder.myadvisor.domain.engine.PurchaseQuery
import com.wickedcoder.myadvisor.domain.engine.Recommendation
import com.wickedcoder.myadvisor.domain.model.Category
import com.wickedcoder.myadvisor.domain.model.Merchant
import com.wickedcoder.myadvisor.domain.repository.CardCatalogRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class RecommendViewModel(
    private val getRecommendations: GetRecommendationsUseCase,
    private val catalogRepository: CardCatalogRepository,
) : ViewModel() {

    data class UiState(
        val merchants: List<Merchant> = emptyList(),
        val categories: List<Category> = emptyList(),
        val searchText: String = "",
        val selectedMerchant: Merchant? = null,
        val selectedCategory: Category? = null,
        val amountText: String = "",
        val results: List<Recommendation>? = null, // null = not asked yet
        val noCards: Boolean = false,
        val error: String? = null,
    ) {
        val hasSelection: Boolean get() = selectedMerchant != null || selectedCategory != null
        val merchantSuggestions: List<Merchant>
            get() = if (searchText.isBlank() || hasSelection) emptyList()
            else merchants.filter { it.name.contains(searchText, ignoreCase = true) }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(
                        merchants = catalogRepository.getMerchants().sortedBy { m -> m.name },
                        categories = catalogRepository.getCategories().sortedBy { c -> c.name },
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun onSearchTextChange(text: String) {
        _state.update { it.copy(searchText = text) }
    }

    fun selectMerchant(merchant: Merchant) {
        _state.update {
            it.copy(selectedMerchant = merchant, selectedCategory = null, searchText = "", results = null)
        }
    }

    fun selectCategory(category: Category) {
        _state.update {
            it.copy(selectedCategory = category, selectedMerchant = null, searchText = "", results = null)
        }
    }

    fun clearSelection() {
        _state.update {
            it.copy(selectedMerchant = null, selectedCategory = null, results = null, noCards = false)
        }
    }

    fun onAmountChange(text: String) {
        if (text.isEmpty() || text.all { it.isDigit() }) {
            _state.update { it.copy(amountText = text.take(8)) }
        }
    }

    fun recommend() {
        val current = _state.value
        if (!current.hasSelection) return
        val query = PurchaseQuery(
            merchantId = current.selectedMerchant?.id,
            categoryId = if (current.selectedMerchant == null) current.selectedCategory?.id else null,
            amountInr = current.amountText.toIntOrNull(),
        )
        viewModelScope.launch {
            try {
                val results = getRecommendations(query, Clock.System.todayIn(TimeZone.currentSystemDefault()))
                _state.update { it.copy(results = results, noCards = results.isEmpty(), error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Recommendation failed") }
            }
        }
    }
}
