package com.wickedcoder.myadvisor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * A warning surface for cap caveats and best-effort disclaimers — the honesty layer
 * the product leans on ("assumes full cap headroom; we can't see your prior spend").
 * Deliberately uses the tertiary/secondary container rather than the error color:
 * a caveat is *information to weigh*, not a failure. Callers pass the specific tone.
 */
enum class CaveatTone { Caution, Info }

@Composable
fun CaveatBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: CaveatTone = CaveatTone.Caution,
    glyph: String = "⚠",
) {
    val container = when (tone) {
        CaveatTone.Caution -> MaterialTheme.colorScheme.tertiaryContainer
        CaveatTone.Info -> MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = when (tone) {
        CaveatTone.Caution -> MaterialTheme.colorScheme.onTertiaryContainer
        CaveatTone.Info -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = onContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(glyph, style = MaterialTheme.typography.bodyMedium)
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Caveat: $text" },
            )
        }
    }
}
