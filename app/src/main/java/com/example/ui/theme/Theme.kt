package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MayaDarkColorScheme = darkColorScheme(
    primary = MayaCyan,
    onPrimary = MayaMidnight,
    primaryContainer = MayaViolet,
    onPrimaryContainer = TextPrimaryDark,
    secondary = MayaViolet,
    onSecondary = TextPrimaryDark,
    secondaryContainer = MayaSurfaceElevated,
    onSecondaryContainer = MayaCyan,
    tertiary = MayaEmerald,
    background = MayaMidnight,
    onBackground = TextPrimaryDark,
    surface = MayaSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = MayaCardBg,
    onSurfaceVariant = TextSecondaryDark,
    outline = MayaVioletGlow,
    error = MayaCoral
)

private val MayaLightColorScheme = lightColorScheme(
    primary = MayaViolet,
    onPrimary = TextPrimaryDark,
    primaryContainer = MayaCyan,
    onPrimaryContainer = MayaMidnight,
    secondary = MayaCyan,
    background = MayaMidnight, // Maya AI is designed with an immersive dark aesthetic
    onBackground = TextPrimaryDark,
    surface = MayaSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = MayaCardBg,
    onSurfaceVariant = TextSecondaryDark
)

@Composable
fun MayaTheme(
    darkTheme: Boolean = true, // Default to dark immersive aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MayaDarkColorScheme else MayaLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = MayaMidnight.toArgb()
                window.navigationBarColor = MayaMidnight.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
