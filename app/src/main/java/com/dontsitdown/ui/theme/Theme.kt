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
import androidx.compose.ui.graphics.luminance
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
        ) { CompositionLocalProvider(LocalWorld provides World.CHAIN) { Ground(content = content) } }
    }
}

/**
 * The don't-sit-down half: ink and amber, no paper. Re-applies the app theme inside a screen
 * (the app root already is this, but a world wrapper keeps the status bar icons right too).
 */

/**
 * The check-in half: the graph-paper notebook, exactly as the web app drew it. Follows the
 * phone's light/dark setting, like the web app did, whatever the chain side is set to.
 */
@Composable
fun CheckinWorld(dark: Boolean = isSystemInDarkTheme(), background: Boolean = true, content: @Composable () -> Unit) {
    val paper = if (dark) NotebookDark else NotebookLight
    CompositionLocalProvider(LocalPaper provides paper, LocalWorld provides World.CHECKIN) {
        MaterialTheme(
            colorScheme = if (dark) NotebookDarkScheme else NotebookLightScheme,
            typography = NotebookTypography,
            shapes = NotebookShapes,
        ) {
            // Overlays (tab bar, toasts) borrow the look without painting a page of their own.
            if (!background) return@MaterialTheme content()
            Surface(
                modifier = Modifier.fillMaxSize().graphPaper(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                content = content,
            )
        }
    }
}

/** Dark status/nav bar icons on light pages and the other way round. */
@Composable
fun SystemBarIcons(light: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    if (view.isInEditMode) return
    androidx.compose.runtime.SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }
}

private val NotebookLightScheme = lightColorScheme(
    primary = Color(0xFF2B3550),
    onPrimary = Color(0xFFFFFEFB),
    primaryContainer = Color(0xFFDCEAF8),
    onPrimaryContainer = Color(0xFF26304A),
    secondary = Color(0xFFF08AA6),
    onSecondary = Color(0xFF26304A),
    background = Color(0xFFF6F7F3),
    onBackground = Color(0xFF26304A),
    surface = Color(0xFFF6F7F3),
    onSurface = Color(0xFF26304A),
    surfaceVariant = Color(0xFFECEEF2),
    onSurfaceVariant = Color(0xFF6F7A95),
    surfaceContainer = Color(0xFFFFFEFB),
    surfaceContainerHigh = Color(0xFFFFFEFB),
    surfaceContainerHighest = Color(0xFFFFFEFB),
    surfaceContainerLow = Color(0xFFFFFEFB),
    outline = Color(0xFFB5BDCF),
    outlineVariant = Color(0xFFB5BDCF),
    error = Color(0xFFD9536F),
    onError = Color.White,
)

private val NotebookDarkScheme = darkColorScheme(
    primary = Color(0xFFE6EDF8),
    onPrimary = Color(0xFF121724),
    primaryContainer = Color(0xFF22304A),
    onPrimaryContainer = Color(0xFFE6EDF8),
    secondary = Color(0xFFEF86A3),
    onSecondary = Color(0xFF121724),
    background = Color(0xFF121724),
    onBackground = Color(0xFFE6EDF8),
    surface = Color(0xFF121724),
    onSurface = Color(0xFFE6EDF8),
    surfaceVariant = Color(0xFF232B40),
    onSurfaceVariant = Color(0xFF93A0BB),
    surfaceContainer = Color(0xFF192032),
    surfaceContainerHigh = Color(0xFF192032),
    surfaceContainerHighest = Color(0xFF192032),
    surfaceContainerLow = Color(0xFF192032),
    outline = Color(0xFF4D5A76),
    outlineVariant = Color(0xFF4D5A76),
    error = Color(0xFFF07A93),
    onError = Color(0xFF121724),
)

private val NotebookShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

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

/** Graph paper: a fine line every 14dp, a stronger one every 56dp. Drawn behind everything. */
fun Modifier.graphPaper(): Modifier = composed {
    val paper = LocalPaper.current
    if (paper.gridBold.alpha == 0f) return@composed Modifier
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
