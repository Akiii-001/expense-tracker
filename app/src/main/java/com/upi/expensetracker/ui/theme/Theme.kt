package com.upi.expensetracker.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = IndigoDark,
    background = SurfaceLight,
    onBackground = Color(0xFF1A1A1F),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1A1F),
    surfaceVariant = IndigoLight,
    onSurfaceVariant = OnSurfaceMuted
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7B5FF),
    onPrimary = IndigoDark,
    primaryContainer = IndigoDark,
    onPrimaryContainer = Color.White,
    background = SurfaceDark,
    onBackground = Color(0xFFE8E8EE),
    surface = SurfaceDark,
    onSurface = Color(0xFFE8E8EE),
    surfaceVariant = CardDark,
    onSurfaceVariant = Color(0xFFA8A8B3)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
