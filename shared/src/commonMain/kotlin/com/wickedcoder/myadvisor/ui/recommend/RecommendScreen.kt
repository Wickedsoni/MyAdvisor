package com.wickedcoder.myadvisor.ui.recommend

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wickedcoder.myadvisor.domain.engine.Recommendation
import com.wickedcoder.myadvisor.ui.components.CaveatBanner
import com.wickedcoder.myadvisor.ui.components.CaveatTone
import com.wickedcoder.myadvisor.ui.components.EmptyState
import com.wickedcoder.myadvisor.ui.components.RateBadge
import com.wickedcoder.myadvisor.ui.components.ResultSkeleton
import com.wickedcoder.myadvisor.ui.components.RouteChip
import com.wickedcoder.myadvisor.ui.components.PrimaryButton
import com.wickedcoder.myadvisor.ui.components.SectionHeader
import com.wickedcoder.myadvisor.ui.components.formatInr
import com.wickedcoder.myadvisor.ui.theme.AppShapeTokens
import com.wickedcoder.myadvisor.ui.theme.AppTypeTokens
import com.wickedcoder.myadvisor.ui.theme.MotionTokens
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * Phase 2 flow: merchant OR category (+ optional amount) → ranked cards with full
 * explanations. Transparency is the product's moat (Spec §5 Step 8).
 *
 * E2 redesign: presentation-only rewrite over the frozen [RecommendViewModel]
 * contract. The #1 result gets a hero treatment; results 2..n are compact; results
 * reveal with a staggered animation. All copy/values still come straight from the
 * engine — no fake precision, caveats surfaced honestly.
 *
 * [onNavigateToCatalog] is invoked by the no-cards empty state's CTA; navigation
 * itself stays in `HomeScreen` (the tab host).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendScreen(
    viewModel: RecommendViewModel = koinViewModel(),
    onNavigateToCatalog: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The ViewModel contract is frozen and exposes no loading flag, so the skeleton
    // state lives locally: raised on tap, cleared whenever results/error settle.
    var isRecommending by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(state.results, state.error) {
        isRecommending = false
    }

    val showValue = state.amountText.toIntOrNull() != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.hasSelection) {
            item {
                SectionHeader(
                    title = "Where are you paying?",
                    subtitle = "Search a merchant, or pick a category below.",
                )
            }
            item {
                OutlinedTextField(
                    value = state.searchText,
                    onValueChange = viewModel::onSearchTextChange,
                    label = { Text("e.g. Swiggy, Amazon, fuel") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    trailingIcon = {
                        if (state.searchText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { viewModel.onSearchTextChange("") }
                                    .semantics { contentDescription = "Clear search" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(state.merchantSuggestions, key = { it.id }) { merchant ->
                Surface(
                    onClick = { viewModel.selectMerchant(merchant) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = merchant.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
            if (state.merchantSuggestions.isEmpty()) {
                item {
                    SectionHeader(title = "Or pick a category")
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        state.categories.forEach { category ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.selectCategory(category) },
                                label = { Text(category.name) },
                            )
                        }
                    }
                }
            }
        } else {
            item {
                val selectionName = state.selectedMerchant?.name ?: state.selectedCategory?.name.orEmpty()
                InputChip(
                    selected = true,
                    onClick = viewModel::clearSelection,
                    label = { Text(selectionName) },
                    trailingIcon = {
                        Text("✕", style = MaterialTheme.typography.labelLarge)
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "$selectionName selected. Tap to change."
                    },
                )
            }
            item {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Amount (optional — matters for capped cards)") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                PrimaryButton(
                    text = "Which card should I use?",
                    onClick = {
                        isRecommending = true
                        viewModel.recommend()
                    },
                    loading = isRecommending,
                )
            }
        }

        state.error?.let { error ->
            item {
                CaveatBanner(text = error, tone = CaveatTone.Caution, glyph = "!")
            }
        }

        if (isRecommending) {
            items(2, key = { "skeleton_$it" }) {
                ResultSkeleton()
            }
        }

        val results = state.results
        if (results != null && !isRecommending) {
            if (state.noCards) {
                item {
                    EmptyState(
                        title = "No cards to compare yet",
                        body = "Add the cards you own from the Catalog, then we'll tell you which one to swipe.",
                        glyph = "✦",
                        cta = {
                            PrimaryButton(
                                text = "Browse the Catalog",
                                onClick = onNavigateToCatalog,
                                fillWidth = false,
                            )
                        },
                    )
                }
            } else {
                item {
                    SectionHeader(
                        title = "Best pick",
                        subtitle = "Ranked by effective reward on this purchase.",
                    )
                }
                itemsIndexed(results) { index, recommendation ->
                    StaggeredReveal(index = index) {
                        if (index == 0) {
                            HeroResult(recommendation = recommendation, showValue = showValue)
                        } else {
                            if (index == 1) {
                                Column {
                                    SectionHeader(title = "Other cards")
                                    Spacer(Modifier.height(8.dp))
                                    CompactResult(rank = index + 1, recommendation = recommendation, showValue = showValue)
                                }
                            } else {
                                CompactResult(rank = index + 1, recommendation = recommendation, showValue = showValue)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Fade + rise entrance, delayed by index so results cascade in on arrival. */
@Composable
private fun StaggeredReveal(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }
    val spec = tween<Float>(durationMillis = MotionTokens.DurationLong, easing = MotionTokens.emphasizedDecelerate)
    val alpha by animateFloatAsState(if (visible) 1f else 0f, animationSpec = spec, label = "revealAlpha")
    val offsetY by animateFloatAsState(if (visible) 0f else 40f, animationSpec = spec, label = "revealOffset")
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        },
    ) {
        content()
    }
}

/** The #1 recommendation: elevated hero surface, oversized rate, "best pick" affordance. */
@Composable
private fun HeroResult(recommendation: Recommendation, showValue: Boolean) {
    val e = recommendation.explanation
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.hero,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BestPickPill()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = recommendation.card.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RateBadge(ratePct = recommendation.effectiveRatePct, emphasized = true)
            }
            val value = recommendation.effectiveValueInr
            if (showValue && value != null) {
                Text(
                    text = "≈ ₹${formatInr(value)} back on this purchase",
                    style = AppTypeTokens.moneyValue,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(e.earnDescription, style = MaterialTheme.typography.bodyMedium)
            recommendation.routeInstruction?.let { instruction ->
                RouteChip(label = instruction)
            }
            recommendation.capCaveat?.let { caveat ->
                CaveatBanner(text = caveat, tone = CaveatTone.Caution)
            }
            ExplanationDetails(recommendation)
        }
    }
}

/** Results 2..n: compact tile, rate trailing, explanation retained (transparency). */
@Composable
private fun CompactResult(rank: Int, recommendation: Recommendation, showValue: Boolean) {
    val e = recommendation.explanation
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(
                    text = recommendation.card.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RateBadge(ratePct = recommendation.effectiveRatePct)
            }
            val value = recommendation.effectiveValueInr
            if (showValue && value != null) {
                Text(
                    text = "≈ ₹${formatInr(value)} back",
                    style = AppTypeTokens.moneyValue,
                )
            }
            Text(e.earnDescription, style = MaterialTheme.typography.bodyMedium)
            recommendation.routeInstruction?.let { instruction ->
                RouteChip(label = instruction)
            }
            recommendation.capCaveat?.let { caveat ->
                CaveatBanner(text = caveat, tone = CaveatTone.Caution)
            }
            ExplanationDetails(recommendation)
        }
    }
}

/** The provenance / exclusions / valuation / data-trail block shared by both tiles. */
@Composable
private fun ExplanationDetails(recommendation: Recommendation) {
    val e = recommendation.explanation
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = e.ruleProvenance,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        e.exclusionNotes.forEach { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        e.valuationNote?.let { note ->
            Text(
                text = note,
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

@Composable
private fun BestPickPill() {
    Surface(
        shape = AppShapeTokens.pill,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = "★ BEST PICK",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
