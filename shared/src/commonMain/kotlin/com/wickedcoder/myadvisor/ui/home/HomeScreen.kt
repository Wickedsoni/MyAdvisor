package com.wickedcoder.myadvisor.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card as CardContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.ui.cards.CardsViewModel
import com.wickedcoder.myadvisor.ui.recommend.RecommendScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Phase 1 surface: browse the curated catalog + manage My Cards.
 * Phase 2 adds the recommendation flow on top.
 */
@Composable
fun HomeScreen(viewModel: CardsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Recommend") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Catalog") })
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("My Cards (${state.ownedCardIds.size})") },
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> Centered { CircularProgressIndicator() }
                    state.error != null -> Centered {
                        Text(
                            text = "Card data failed to load:\n${state.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    selectedTab == 0 -> RecommendScreen()
                    selectedTab == 1 -> CardList(
                        cards = state.catalog,
                        state = state,
                        emptyText = "No cards in the catalog yet.",
                        actionFor = { card ->
                            if (card.id in state.ownedCardIds) {
                                CardAction("Added ✓", enabled = false) {}
                            } else {
                                CardAction("Add") { viewModel.addCard(card.id) }
                            }
                        },
                    )
                    else -> CardList(
                        cards = state.ownedCards,
                        state = state,
                        emptyText = "No cards yet — add the cards you own from the Catalog tab.",
                        actionFor = { card -> CardAction("Remove") { viewModel.removeCard(card.id) } },
                    )
                }
            }

            state.dataVersionLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
        }
    }
}

private data class CardAction(val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun CardList(
    cards: List<Card>,
    state: CardsViewModel.UiState,
    emptyText: String,
    actionFor: (Card) -> CardAction,
) {
    if (cards.isEmpty()) {
        Centered {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards, key = { it.id }) { card ->
            CatalogCardRow(
                card = card,
                issuerName = state.issuersById[card.issuerId]?.name ?: card.issuerId,
                action = actionFor(card),
            )
        }
    }
}

@Composable
private fun CatalogCardRow(card: Card, issuerName: String, action: CardAction) {
    CardContainer(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(card.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = issuerName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Base: ${card.baseRule.reward.earnDescription} · " +
                    "${card.rules.size} bonus rule${if (card.rules.size == 1) "" else "s"} · " +
                    "verified ${card.lastVerified}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (action.enabled) {
                Button(onClick = action.onClick, modifier = Modifier.padding(top = 8.dp)) {
                    Text(action.label)
                }
            } else {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.padding(top = 8.dp)) {
                    Text(action.label)
                }
            }
        }
    }
}
