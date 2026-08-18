package com.lorecanvas.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Parchment,
    background = Parchment,
    onBackground = Ink900,
    surface = Parchment,
    onSurface = Ink900,
    outline = Border
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Parchment,
    background = Ink900,
    onBackground = Parchment,
    surface = Ink900,
    onSurface = Parchment,
    outline = Ink600
)

/**
 * Root theme wrapper. Phase 1 keeps this minimal (system light/dark only) —
 * a real design system arrives with LCD-010 (User Interface Specification)
 * work in a later phase.
 */
@Composable
fun LoreCanvasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LoreCanvasTypography,
        content = content
    )
}
