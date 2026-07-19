package com.mathymove.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TextPrimary,
    onPrimary = GreyBackground,
    secondary = TextSecondary,
    background = GreyBackground,
    surface = GreySurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = GreySurfaceVariant,
    outline = GreyBorder
)

@Composable
fun MathyMoveTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
