package com.wickedcoder.myadvisor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * MyAdvisor shape scale — expressive corners.
 *
 * Slightly larger radii than the M3 baseline give the app a softer, more premium
 * feel (CRED / Revolut register). The base M3 [Shapes] drives standard components;
 * [AppShapeTokens] adds two bespoke shapes the design brief calls for: an oversized
 * `hero` radius for the #1-recommendation surface, and a fully rounded `pill` for
 * badges and chips.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object AppShapeTokens {
    /** Hero recommendation tile — deliberately larger than extraLarge. */
    val hero = RoundedCornerShape(32.dp)

    /** Badges, rate pills, chips. */
    val pill = RoundedCornerShape(percent = 50)
}
