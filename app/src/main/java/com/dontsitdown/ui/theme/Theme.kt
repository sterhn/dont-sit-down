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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val dark = isDark(mode)
    CompositionLocalProvider(LocalPaper provides if (dark) DarkPaper else LightPaper) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
        ) { Ground(grid = true, content) }
    }
}

/** Background plus default text colour, so bare Text() never falls back to black-on-ink. */
@Composable
private fun Ground(grid: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize().then(if (grid) Modifier.graphPaper() else Modifier),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = content,
    )
}

/** Graph paper: a fine line every 14dp, a stronger one every 56dp. Drawn behind everything. */
fun Modifier.graphPaper(): Modifier = composed {
    val paper = LocalPaper.current
    drawBehind {
        val step = 14.dp.toPx()
        val px = 1f
        var i = 0
        var x = 0f
        while (x < size.width) {
            drawRect(if (i % 4 == 0) paper.gridBold else paper.gridFine, Offset(x, 0f), Size(px, size.height))
            x += step; i++
        }
        i = 0
        var y = 0f
        while (y < size.height) {
            drawRect(if (i % 4 == 0) paper.gridBold else paper.gridFine, Offset(0f, y), Size(size.width, px))
            y += step; i++
        }
    }
}

/** The takeover is always stark: black, white, amber. Theme setting doesn't apply. */
@Composable
fun TakeoverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme.copy(background = Color.Black, surface = Color.Black, onBackground = Color.White, onSurface = Color.White),
        typography = AppTypography,
        shapes = AppShapes,
    ) { CompositionLocalProvider(LocalPaper provides DarkPaper) { Ground(content = content) } }
}
