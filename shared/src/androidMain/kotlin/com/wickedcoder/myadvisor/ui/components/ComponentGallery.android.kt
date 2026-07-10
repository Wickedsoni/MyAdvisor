package com.wickedcoder.myadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wickedcoder.myadvisor.ui.theme.MyAdvisorTheme

/**
 * Android-only design-system gallery. The E1 components live in `commonMain`, but the
 * `@Preview` tooling annotation is Android-flavored, so the previews are hosted here.
 * This file exists purely so the components are inspectable in Android Studio's preview
 * pane before E2/E3 wire them into real screens — it ships no user-facing surface.
 */

@Composable
private fun Gallery() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(title = "Rate badges", subtitle = "Hero + compact, tabular figures")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RateBadge(ratePct = 7.5, emphasized = true)
            RateBadge(ratePct = 2.5)
            RateBadge(ratePct = 1.33)
        }

        SectionHeader(title = "Card tiles")
        CardTile(
            title = "HDFC Swiggy Credit Card",
            subtitle = "HDFC Bank",
            monogram = "HD",
            supporting = "Base 1% · 3 bonus rules · verified 2026-06-01",
            trailing = { RateBadge(ratePct = 10.0) },
        )
        CardTile(
            title = "Axis ACE",
            subtitle = "Axis Bank",
            monogram = "AX",
            supporting = "Base 1.5% · 2 bonus rules",
        )

        SectionHeader(title = "Route + caveat")
        RouteChip(label = "via HDFC SmartBuy")
        CaveatBanner("Assumes the full ₹1,000/month cap is available — we can't see your prior spend.")
        CaveatBanner(
            "Rate shown is best-effort from the merchant's category.",
            tone = CaveatTone.Info,
            glyph = "ℹ",
        )

        SectionHeader(title = "Loading")
        ResultSkeleton()

        SectionHeader(title = "Buttons")
        PrimaryButton(text = "Which card should I use?", onClick = {})
        PrimaryButton(text = "Recommending…", onClick = {}, loading = true)

        SectionHeader(title = "Empty state")
        EmptyState(
            title = "No cards yet",
            body = "Add the cards you own from the Catalog and MyAdvisor will tell you which one to swipe.",
            cta = { PrimaryButton(text = "Browse catalog", onClick = {}, fillWidth = false) },
        )
    }
}

@Preview(name = "Components · Light", showBackground = true, heightDp = 1400)
@Composable
private fun GalleryLightPreview() {
    MyAdvisorTheme(darkTheme = false) { Gallery() }
}

@Preview(name = "Components · Dark", showBackground = true, heightDp = 1400)
@Composable
private fun GalleryDarkPreview() {
    MyAdvisorTheme(darkTheme = true) { Gallery() }
}
