package com.wickedcoder.myadvisor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Phase 0 theming skeleton — palette refinement is a Phase 4 (polish) concern.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E4B),
    secondary = Color(0xFF4A635B),
    tertiary = Color(0xFF41617D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D6BE),
    secondary = Color(0xFFB1CCC1),
    tertiary = Color(0xFFA9CAE9),
)

@Composable
fun MyAdvisorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
