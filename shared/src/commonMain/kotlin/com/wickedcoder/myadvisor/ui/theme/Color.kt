package com.wickedcoder.myadvisor.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * MyAdvisor color system.
 *
 * Brand seed: deep emerald (`0xFF006B5B`), evolved from the Phase 0 skeleton seed
 * `0xFF1B5E4B`. Green reads as trust + money in Indian fintech (the product's
 * whole promise is "trust me, use this card"), while staying distinct from the
 * blue every bank app already owns. Tertiary is a calm cyan-blue reserved for
 * *portal routing* affordances (RouteChip) so "pay via SmartBuy" never visually
 * collides with the primary recommendation.
 *
 * Full M3 tonal roles for both themes, including the surfaceContainer elevation
 * ladder (material3 1.11) that the tactile card tiles depend on.
 */

// ---- Light ----------------------------------------------------------------
private val Light = lightColorScheme(
    primary = Color(0xFF006B5B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7DF8DD),
    onPrimaryContainer = Color(0xFF00201A),
    inversePrimary = Color(0xFF5FDBC4),

    secondary = Color(0xFF4A635B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8DE),
    onSecondaryContainer = Color(0xFF06201A),

    tertiary = Color(0xFF3E6374),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F2A),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF5FBF6),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FBF6),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DF),
    onSurfaceVariant = Color(0xFF3F4945),
    surfaceTint = Color(0xFF006B5B),
    inverseSurface = Color(0xFF2B322F),
    inverseOnSurface = Color(0xFFECF2ED),

    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBFC9C3),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFFF5FBF6),
    surfaceDim = Color(0xFFD5DBD6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F0),
    surfaceContainer = Color(0xFFE9EFEA),
    surfaceContainerHigh = Color(0xFFE3EAE5),
    surfaceContainerHighest = Color(0xFFDEE4DF),
)

// ---- Dark -----------------------------------------------------------------
private val Dark = darkColorScheme(
    primary = Color(0xFF5FDBC4),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFF7DF8DD),
    inversePrimary = Color(0xFF006B5B),

    secondary = Color(0xFFB1CCC1),
    onSecondary = Color(0xFF1C352E),
    secondaryContainer = Color(0xFF334B44),
    onSecondaryContainer = Color(0xFFCCE8DE),

    tertiary = Color(0xFFA6CCDE),
    onTertiary = Color(0xFF093543),
    tertiaryContainer = Color(0xFF254B5B),
    onTertiaryContainer = Color(0xFFC1E8FB),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C3),
    surfaceTint = Color(0xFF5FDBC4),
    inverseSurface = Color(0xFFDEE4DF),
    inverseOnSurface = Color(0xFF2B322F),

    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4945),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF353B38),
    surfaceDim = Color(0xFF0F1512),
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633),
)

internal val LightColorScheme: ColorScheme = Light
internal val DarkColorScheme: ColorScheme = Dark
