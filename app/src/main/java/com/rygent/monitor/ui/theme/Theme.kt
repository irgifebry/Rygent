package com.rygent.monitor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    tertiary = WarningOrange,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = ErrorRed
)

// We want to force Dark Mode as per design implication, or at least default to it.
// However, the requirement says "support dark mode", usually meaning it follows system or toggle.
// But the design is heavily dark. I'll stick to system check but ensure dark scheme is beautiful.

@Composable
fun SystemMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to match design exactness
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Fallback to Dark for now if light not designed, or use Light if implemented later.
        // For now, let's force DarkColorScheme if "darkTheme" is false just to be safe with the design provided?
        // No, let's respect the flag but maybe provide a light scheme later.
        // Actually, the user designs are ONLY dark. It's safer to default to DarkColorScheme for both for now to ensure consistency with the mockup until Light mode is designed.
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
