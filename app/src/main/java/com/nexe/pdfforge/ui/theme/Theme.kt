package com.nexe.pdfforge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = VioletDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E2FF),
    onPrimaryContainer = Color(0xFF1A1470),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF4FC),
    onSecondaryContainer = Color(0xFF00363D),
    tertiary = Color(0xFFDB2777),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9EA),
    onTertiaryContainer = Color(0xFF4A0B2B),
    background = Snow,
    onBackground = Color(0xFF14152B),
    surface = Snow,
    onSurface = Color(0xFF14152B),
    surfaceVariant = Color(0xFFE4E5F7),
    onSurfaceVariant = Color(0xFF494C6B),
    outline = Color(0xFF7A7DA0),
    outlineVariant = Color(0xFFD3D5EC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F1FF),
    surfaceContainerHigh = Color(0xFFE9EAFB),
    surfaceContainerHighest = Color(0xFFE2E3F7),
    error = Color(0xFFD32F4F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9D92FF),
    onPrimary = Color(0xFF14104A),
    primaryContainer = Color(0xFF2E2A7A),
    onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFF5EE1F2),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF0E4A55),
    onSecondaryContainer = Color(0xFFC4F3FA),
    tertiary = Color(0xFFFF9BCB),
    onTertiary = Color(0xFF4A0B2B),
    tertiaryContainer = Color(0xFF6B1F45),
    onTertiaryContainer = Color(0xFFFFD9EA),
    background = Night,
    onBackground = Color(0xFFE8E9F7),
    surface = Night,
    onSurface = Color(0xFFE8E9F7),
    surfaceVariant = Color(0xFF23274A),
    onSurfaceVariant = Color(0xFFB5B8D6),
    outline = Color(0xFF6D7199),
    outlineVariant = Color(0xFF2C3059),
    surfaceContainerLowest = Color(0xFF070914),
    surfaceContainerLow = NightLow,
    surfaceContainer = NightMid,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightTop,
    error = Color(0xFFFF8A9B)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun PdfForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
