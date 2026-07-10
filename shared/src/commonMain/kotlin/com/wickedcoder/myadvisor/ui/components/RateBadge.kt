package com.wickedcoder.myadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wickedcoder.myadvisor.ui.theme.AppShapeTokens
import com.wickedcoder.myadvisor.ui.theme.AppTypeTokens

/**
 * The reward-rate pill — the single most important number on any result. Two sizes:
 * [emphasized] for the hero (#1) recommendation, compact otherwise. Uses tabular
 * figures so stacked rates align. Colored with the primary container to read as a
 * positive, money-good signal without shouting.
 */
@Composable
fun RateBadge(
    ratePct: Double,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val text = formatRatePct(ratePct)
    val hPad = if (emphasized) 18.dp else 12.dp
    val vPad = if (emphasized) 8.dp else 4.dp
    Text(
        text = text,
        style = if (emphasized) AppTypeTokens.rateHero else AppTypeTokens.rateMedium,
        color = contentColor,
        modifier = modifier
            .semantics { contentDescription = "$text reward rate" }
            .background(containerColor, AppShapeTokens.pill)
            .padding(horizontal = hPad, vertical = vPad),
    )
}
