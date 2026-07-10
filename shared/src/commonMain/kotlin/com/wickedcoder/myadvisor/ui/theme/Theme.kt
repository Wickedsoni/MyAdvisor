package com.wickedcoder.myadvisor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The single theme entry point for the app (wired in [com.wickedcoder.myadvisor.App]).
 * Composes the deliberate color scheme ([Color.kt]), expressive type ([Type.kt]) and
 * shape ([Shape.kt]) scales. Motion tokens ([Motion.kt]) are consumed directly by
 * animating components rather than living on [MaterialTheme].
 */
@Composable
fun MyAdvisorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
