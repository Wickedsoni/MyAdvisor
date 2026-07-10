package com.wickedcoder.myadvisor.ui.recommend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card as CardContainer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wickedcoder.myadvisor.domain.engine.Recommendation
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

/**
 * Phase 2 flow: merchant OR category (+ optional amount) → ranked cards with
 * full explanations. Transparency is the product's moat (Spec §5 Step 8).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendScreen(viewModel: RecommendViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.hasSelection) {
            item {
                OutlinedTextField(
                    value = state.searchText,
                    onValueChange = viewModel::onSearchTextChange,
                    label = { Text("Where are you paying? (e.g. Swiggy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(state.merchantSuggestions, key = { it.id }) { merchant ->
                CardContainer(
                    onClick = { viewModel.selectMerchant(merchant) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = merchant.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            item {
                Text(
                    "…or pick a category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = viewModel::clearSelection,
                        label = {
                            Text(
                                (state.selectedMerchant?.name ?: state.selectedCategory?.name.orEmpty()) + "  ✕",
                            )
                        },
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Amount in ₹ (optional — matters for capped cards)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(onClick = viewModel::recommend, modifier = Modifier.fillMaxWidth()) {
                    Text("Which card should I use?")
                }
            }
        }

        state.error?.let { error ->
            item {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        val results = state.results
        if (results != null) {
            if (state.noCards) {
                item {
                    Text(
                        "You haven't added any cards yet — add the cards you own from the Catalog tab first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item { HorizontalDivider() }
                items(results, key = { it.card.id }) { recommendation ->
                    RecommendationRow(
                        rank = results.indexOf(recommendation) + 1,
                        recommendation = recommendation,
                        showValue = state.amountText.toIntOrNull() != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationRow(rank: Int, recommendation: Recommendation, showValue: Boolean) {
    val e = recommendation.explanation
    CardContainer(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$rank  ${recommendation.card.name}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatPct(recommendation.effectiveRatePct),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (showValue && recommendation.effectiveValueInr != null) {
                Text(
                    text = "≈ ₹${recommendation.effectiveValueInr} back on this purchase",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(e.earnDescription, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = e.ruleProvenance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            recommendation.routeInstruction?.let {
                Text(
                    text = "→ $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            recommendation.capCaveat?.let {
                Text(
                    text = "⚠ $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            e.exclusionNotes.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            e.valuationNote?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Data verified ${e.dataVerified} · Reward DB v${e.dataVersion}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatPct(pct: Double): String {
    val rounded = (pct * 100).roundToInt() / 100.0
    return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}%" else "$rounded%"
}
