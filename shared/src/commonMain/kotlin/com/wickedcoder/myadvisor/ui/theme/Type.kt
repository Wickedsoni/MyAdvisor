package com.wickedcoder.myadvisor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * MyAdvisor type system.
 *
 * The base scale is the M3 default (system font, cross-platform safe — no bundled
 * font files in `:shared` to keep the iOS bundle lean). What we add is deliberate:
 * a set of *tabular-figure* number styles for reward rates and rupee values.
 * `fontFeatureSettings = "tnum"` locks every digit to the same advance width so a
 * ranked list of "7.5% / 2.5% / 1.33%" stays optically aligned and never jitters
 * as values animate in — reward numbers are the hero of every screen and must read
 * as precise, not decorative.
 */
object AppTypeTokens {
    /** Hero rate on the #1 recommendation (RateBadge, emphasized). */
    val rateHero = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )

    /** Rate on secondary results and compact tiles. */
    val rateMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )

    /** Inline rupee values ("≈ ₹1,500 back"). */
    val moneyValue = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )
}

/** Access the tokens from composition; a CompositionLocal keeps the door open for
 *  future theme variants without threading a param through every component. */
val LocalAppType = staticCompositionLocalOf { AppTypeTokens }

/**
 * Expressive base scale. Display/headline get slightly tighter tracking and heavier
 * weight than the M3 default for a more confident, premium voice; body/label are
 * left at comfortable reading defaults.
 */
val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.015).em),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}
