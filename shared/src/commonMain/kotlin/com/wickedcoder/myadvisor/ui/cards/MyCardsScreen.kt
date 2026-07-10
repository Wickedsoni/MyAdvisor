package com.wickedcoder.myadvisor.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wickedcoder.myadvisor.ui.components.CardTile
import com.wickedcoder.myadvisor.ui.components.EmptyState

/**
 * The cards the user actually owns — the input set the recommendation engine ranks.
 * Empty until the user adds from the Catalog, so it leans on a rich [EmptyState] with
 * a CTA back to the Catalog tab. Presentation-only over the frozen [CardsViewModel]
 * contract (E3). Navigation/host stays in `HomeScreen`.
 */
@Composable
fun MyCardsScreen(
    state: CardsViewModel.UiState,
    onRemove: (String) -> Unit,
    onBrowseCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val owned = state.ownedCards
    if (owned.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "No cards yet",
                body = "Add the cards you own from the Catalog and we'll tell you which one to swipe on every purchase.",
                glyph = "💳",
                cta = {
                    OutlinedButton(onClick = onBrowseCatalog) {
                        Text("Browse the Catalog")
                    }
                },
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(owned, key = { it.id }) { card ->
            val issuerName = state.issuersById[card.issuerId]?.name ?: card.issuerId
            CardTile(
                modifier = Modifier.animateItem(),
                title = card.name,
                subtitle = issuerName,
                monogram = issuerName,
                supporting = cardSupporting(card),
                trailing = {
                    OutlinedButton(onClick = { onRemove(card.id) }) {
                        Text("Remove")
                    }
                },
            )
        }
    }
}
