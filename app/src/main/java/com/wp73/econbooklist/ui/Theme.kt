package com.wp73.econbooklist.ui

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
import com.wp73.econbooklist.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF146456),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E9DF),
    onPrimaryContainer = Color(0xFF0B3D34),
    background = Color(0xFFF6F4ED),
    onBackground = Color(0xFF17312D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17312D),
    surfaceVariant = Color(0xFFE5ECE5),
    onSurfaceVariant = Color(0xFF52665F),
    outline = Color(0xFFB7C7BD)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF76D4C0),
    onPrimary = Color(0xFF06382F),
    primaryContainer = Color(0xFF174E44),
    onPrimaryContainer = Color(0xFFC9F1E7),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFE4ECE8),
    surface = Color(0xFF17201D),
    onSurface = Color(0xFFE4ECE8),
    surfaceVariant = Color(0xFF22302B),
    onSurfaceVariant = Color(0xFFB6C7C0),
    outline = Color(0xFF647871)
)

@Composable
fun EconBookTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
