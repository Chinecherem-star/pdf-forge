package com.nexe.pdfforge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Indigo40,
    onPrimary = Paper,
    primaryContainer = Indigo90,
    onPrimaryContainer = IndigoDark,
    secondary = Teal40,
    onSecondary = Paper,
    secondaryContainer = Teal90,
    tertiary = Teal40,
    background = Paper,
    surface = Paper
)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = IndigoDark,
    primaryContainer = Indigo40,
    onPrimaryContainer = Indigo90,
    secondary = Teal80,
    onSecondary = Ink,
    secondaryContainer = Teal40,
    tertiary = Teal80,
    background = Ink,
    surface = Ink,
    surfaceContainerLow = InkSurface
)

@Composable
fun PdfForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
