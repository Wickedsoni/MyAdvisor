package com.wickedcoder.myadvisor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wickedcoder.myadvisor.ui.theme.AppShapeTokens

/**
 * A portal-routing indicator ("via HDFC SmartBuy"). Tertiary-toned so routing — the
 * "there's a better way to pay" signal — is visually distinct from the primary
 * reward rate. Small, pill-shaped, sits inline above the route instruction.
 */
@Composable
fun RouteChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { contentDescription = "Pay via $label" },
        shape = AppShapeTokens.pill,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("→", style = MaterialTheme.typography.labelMedium)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
