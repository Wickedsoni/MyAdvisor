package com.wickedcoder.myadvisor.ui.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.ui.components.CardTile
import com.wickedcoder.myadvisor.ui.components.EmptyState
import com.wickedcoder.myadvisor.ui.theme.MotionTokens

/**
 * The full curated catalog: every card the dataset ships, each a tactile [CardTile]
 * with an add affordance that morphs to an "Added ✓" state. Presentation-only over
 * the frozen [CardsViewModel] contract (E3). Navigation/host stays in `HomeScreen`.
 */
@Composable
fun CatalogScreen(
    state: CardsViewModel.UiState,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.catalog.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Catalog is empty",
                body = "No cards have been curated into the dataset yet.",
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.catalog, key = { it.id }) { card ->
            val issuerName = state.issuersById[card.issuerId]?.name ?: card.issuerId
            val owned = card.id in state.ownedCardIds
            CardTile(
                modifier = Modifier.animateItem(),
                title = card.name,
                subtitle = issuerName,
                monogram = issuerName,
                supporting = cardSupporting(card),
                trailing = { AddAction(owned = owned, onClick = { onAdd(card.id) }) },
            )
        }
    }
}

/** "Base 1% cashback · 9 bonus rules · Verified 2025-11-02" — the trust line. */
internal fun cardSupporting(card: Card): String {
    val bonus = "${card.rules.size} bonus rule${if (card.rules.size == 1) "" else "s"}"
    return "Base ${card.baseRule.reward.earnDescription} · $bonus · Verified ${card.lastVerified}"
}

/** The add affordance: a tonal "Add" button that morphs into an "Added ✓" pill. */
@Composable
private fun AddAction(owned: Boolean, onClick: () -> Unit) {
    AnimatedContent(
        targetState = owned,
        transitionSpec = {
            val spec = tween<Float>(durationMillis = MotionTokens.DurationMedium, easing = MotionTokens.emphasized)
            (fadeIn(spec) + scaleIn(spec, initialScale = 0.8f)) togetherWith
                (fadeOut(spec) + scaleOut(spec, targetScale = 0.8f))
        },
        label = "addMorph",
    ) { isOwned ->
        if (isOwned) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "✓ Added",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            FilledTonalButton(onClick = onClick) {
                Text("Add")
            }
        }
    }
}
