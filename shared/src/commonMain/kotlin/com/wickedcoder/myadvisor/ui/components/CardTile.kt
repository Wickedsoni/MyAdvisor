package com.wickedcoder.myadvisor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The tactile card row used everywhere a credit card is listed (Catalog, My Cards).
 * A monogram leading slot (issuer initial — real logos are a post-launch nicety),
 * a title/subtitle stack, an optional supporting line, and a trailing action slot.
 * Elevated with a generous corner radius so the list reads as a stack of physical
 * cards, not table rows.
 */
@Composable
fun CardTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    monogram: String? = null,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (monogram != null) Monogram(monogram)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (supporting != null) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (trailing != null) trailing()
        }
    }

    val colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
    val elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    if (onClick != null) {
        ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, elevation = elevation) {
            content()
        }
    } else {
        ElevatedCard(modifier = modifier.fillMaxWidth(), colors = colors, elevation = elevation) {
            content()
        }
    }
}

@Composable
private fun Monogram(text: String) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text.take(2).uppercase(),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
