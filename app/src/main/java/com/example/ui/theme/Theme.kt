package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FusionDarkColorScheme = darkColorScheme(
    primary = NeonPurpleLight,
    onPrimary = Color.White,
    primaryContainer = NeonPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D5A),
    onSecondaryContainer = NeonCyan,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = ObsidianDark,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorder.copy(alpha = 0.5f)
)

private val FusionLightColorScheme = lightColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF007A8A),
    onSecondary = Color.White,
    background = Color(0xFFF7F7FC),
    onBackground = Color(0xFF14141E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14141E),
    surfaceVariant = Color(0xFFEAEAF2),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0)
)

@Composable
fun FusionMusicTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode for music experience
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FusionDarkColorScheme else FusionLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FusionMusicTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

