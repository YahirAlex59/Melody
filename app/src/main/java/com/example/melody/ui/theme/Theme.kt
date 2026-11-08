package com.example.melody.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LarkDarkColorScheme = darkColorScheme(
    primary = LarkBlueAccent,
    onPrimary = Color(0xFF001E30),
    primaryContainer = LarkBlueContainer,
    onPrimaryContainer = LarkTextPrimary,
    secondary = LarkBlueAccent,
    onSecondary = Color(0xFF001E30),
    secondaryContainer = LarkSurfaceElevated,
    onSecondaryContainer = LarkTextPrimary,
    tertiary = LarkMiniPlayerGreen,
    onTertiary = LarkTextPrimary,
    background = LarkBackgroundDark,
    onBackground = LarkTextPrimary,
    surface = LarkSurfaceDark,
    onSurface = LarkTextPrimary,
    surfaceVariant = LarkSurfaceElevated,
    onSurfaceVariant = LarkTextSecondary,
    surfaceContainerLowest = LarkBackgroundDark,
    surfaceContainerLow = LarkSurfaceDark,
    surfaceContainer = LarkSurfaceDark,
    surfaceContainerHigh = LarkSurfaceElevated,
    surfaceContainerHighest = LarkSurfaceElevated,
    error = LarkErrorRed
)

@Composable
fun MelodyTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LarkDarkColorScheme,
        typography = Typography,
        content = content
    )
}
