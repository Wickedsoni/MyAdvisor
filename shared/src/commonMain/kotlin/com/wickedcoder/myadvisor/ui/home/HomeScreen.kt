package com.wickedcoder.myadvisor.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wickedcoder.myadvisor.ui.cards.CardsViewModel
import com.wickedcoder.myadvisor.ui.cards.CatalogScreen
import com.wickedcoder.myadvisor.ui.cards.MyCardsScreen
import com.wickedcoder.myadvisor.ui.recommend.RecommendScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * The app shell. A `TopAppBar` carries the app name and the Reward-DB provenance
 * (moved out of the old footer), and a bottom `NavigationBar` puts the three
 * destinations within one-handed reach. Each destination's content lives in its own
 * screen composable — `HomeScreen` owns only navigation and the shared loading/error
 * gate. (E4: chrome + accessibility pass.)
 */
private data class Destination(val label: String, val glyph: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: CardsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    val destinations = listOf(
        Destination("Recommend", "✦"),
        Destination("Catalog", "▤"),
        Destination("My Cards", "▣"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MyAdvisor", style = MaterialTheme.typography.titleLarge)
                        state.dataVersionLabel?.let { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, dest ->
                    val label = if (index == 2) "${dest.label} (${state.ownedCardIds.size})" else dest.label
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            // Glyph placeholder icon; the label supplies the accessible
                            // name, so the icon itself is hidden from TalkBack.
                            Text(
                                dest.glyph,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                selectedTab == 0 -> RecommendScreen(onNavigateToCatalog = { selectedTab = 1 })
                selectedTab == 1 -> CatalogScreen(state = state, onAdd = viewModel::addCard)
                else -> MyCardsScreen(
                    state = state,
                    onRemove = viewModel::removeCard,
                    onBrowseCatalog = { selectedTab = 1 },
                )
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
