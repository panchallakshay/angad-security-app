package com.spidey.js.angad.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MahabharatColorScheme = darkColorScheme(
    primary = RoyalGold,
    secondary = DivineSaffron,
    tertiary = MutedGold,
    background = DeepEarth,
    surface = TempleSurface,
    onPrimary = DeepEarth,
    onSecondary = DeepEarth,
    onBackground = AncientWhite,
    onSurface = AncientWhite,
    error = LavaCrimson
)

@Composable
fun AngadTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = MahabharatColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
