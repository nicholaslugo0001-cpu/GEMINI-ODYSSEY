package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.ThemeMode

private val GeminiOdysseyDarkColorScheme = darkColorScheme(
    primary = GeminiCyan,
    onPrimary = DeepWebVoid,
    primaryContainer = GeminiBlue,
    onPrimaryContainer = TextPrimary,
    secondary = GoogleGold,
    onSecondary = DeepWebVoid,
    secondaryContainer = CyberSurfaceElevated,
    onSecondaryContainer = GoogleGold,
    tertiary = GoogleGreen,
    onTertiary = DeepWebVoid,
    background = DeepWebVoid,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CyberSurfaceBorder,
    error = VirusCrimson,
    onError = TextPrimary
)

private val GeminiOdysseyLightColorScheme = lightColorScheme(
    primary = GeminiBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF174EA6),
    secondary = GoogleGold,
    onSecondary = Color(0xFF202124),
    secondaryContainer = Color(0xFFFEF7E0),
    onSecondaryContainer = Color(0xFF202124),
    tertiary = GoogleGreen,
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202124),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFFDADCE0),
    error = VirusCrimson,
    onError = Color.White
)

@Composable
fun GeminiOdysseyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val baseScheme = if (isDark) GeminiOdysseyDarkColorScheme else GeminiOdysseyLightColorScheme
    val colorScheme = if (highContrast) {
        if (isDark) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color(0xFF121212),
                onBackground = Color.White,
                onSurface = Color.White,
                outline = Color.White
            )
        } else {
            baseScheme.copy(
                background = Color.White,
                surface = Color(0xFFF1F3F4),
                onBackground = Color.Black,
                onSurface = Color.Black,
                outline = Color.Black
            )
        }
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
