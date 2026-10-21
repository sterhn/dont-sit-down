package com.dontsitdown.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dontsitdown.util.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF120C00),
    primaryContainer = Color(0xFF3A2803),
    onPrimaryContainer = Amber,
    secondary = Amber,
    onSecondary = Color(0xFF120C00),
    background = Ink,
    onBackground = Bone,
    surface = Ink,
    onSurface = Bone,
    surfaceVariant = InkHigh,
    onSurfaceVariant = Ash,
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHigh,
    surfaceContainerLow = InkRaised,
    surfaceContainerLowest = Ink,
    outline = Iron,
    outlineVariant = Iron,
    error = Rust,
    onError = Ink,
)

private val LightScheme = lightColorScheme(
    primary = AmberDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E2B8),
    onPrimaryContainer = Color(0xFF3A2803),
    secondary = AmberDeep,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Graphite,
    surface = Paper,
    onSurface = Graphite,
    surfaceVariant = PaperHigh,
    onSurfaceVariant = Slate,
    surfaceContainer = PaperRaised,
    surfaceContainerHigh = PaperHigh,
    surfaceContainerHighest = PaperHigh,
    surfaceContainerLow = PaperRaised,
    surfaceContainerLowest = PaperRaised,
    outline = Rule,
    outlineVariant = Rule,
    error = Color(0xFFB3261E),
)

/** Square-ish corners. No rounded pastel cards. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun AppTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark(mode)) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
    ) { Ground(content) }
}

/** Background plus default text colour, so bare Text() never falls back to black-on-ink. */
@Composable
private fun Ground(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = content,
    )
}

/** The takeover is always stark: black, white, amber. Theme setting doesn't apply. */
@Composable
fun TakeoverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme.copy(background = Color.Black, surface = Color.Black, onBackground = Color.White, onSurface = Color.White),
        typography = AppTypography,
        shapes = AppShapes,
    ) { Ground(content) }
}
