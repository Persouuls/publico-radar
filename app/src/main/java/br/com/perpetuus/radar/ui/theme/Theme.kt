package br.com.perpetuus.radar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF202A32)
private val Muted = Color(0xFF697782)
private val Canvas = Color(0xFFF5F7F8)
private val Surface = Color(0xFFFFFFFF)
private val Accent = Color(0xFF315E68)
private val AccentSoft = Color(0xFFE6EFF0)
private val DarkCanvas = Color(0xFF121A1F)
private val DarkSurface = Color(0xFF1D282E)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Color(0xFF193B42),
    secondary = Color(0xFF667783),
    background = Canvas,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF1F3),
    onSurfaceVariant = Muted,
    outline = Color(0xFFD8E0E3),
    error = Color(0xFFB64B43)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BC6CC),
    onPrimary = Color(0xFF15343A),
    primaryContainer = Color(0xFF29474D),
    onPrimaryContainer = Color(0xFFD8EFF1),
    secondary = Color(0xFFB1C0C7),
    background = DarkCanvas,
    onBackground = Color(0xFFE5ECEF),
    surface = DarkSurface,
    onSurface = Color(0xFFE5ECEF),
    surfaceVariant = Color(0xFF29363D),
    onSurfaceVariant = Color(0xFFAFBDC3),
    outline = Color(0xFF3B4A52),
    error = Color(0xFFFFB4AB)
)

@Composable
fun RadarTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val background = if (dark) android.graphics.Color.rgb(18, 26, 31) else android.graphics.Color.rgb(245, 247, 248)
            window.statusBarColor = background
            window.navigationBarColor = background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
