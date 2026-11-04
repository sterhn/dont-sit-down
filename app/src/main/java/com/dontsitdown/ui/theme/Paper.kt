package com.dontsitdown.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Which half of the app a screen belongs to. They look like two different apps, on purpose. */
enum class World {
    /** don't sit down: ink, amber, monospace. Stark. */
    CHAIN,

    /** the check-in: the graph-paper notebook, the character, soft colours. Gentle. */
    CHECKIN,
}

val LocalWorld = staticCompositionLocalOf { World.CHAIN }

/**
 * The paper bits shared by the check-in components: cards, tape, the polaroid, the 1–10 scales,
 * and the soft data colours (sleep / mood / body). The notebook palettes are the web app's own.
 */
@Immutable
data class PaperColors(
    val gridFine: Color,
    val gridBold: Color,
    val card: Color,
    /** Card and tile outlines. */
    val cardLine: Color,
    /** Scale boxes and field outlines. */
    val segLine: Color,
    val shadow: Color,
    val tapeA: Color,
    val tapeB: Color,
    val polaroid: Color,
    val photo: Color,
    val polaroidInk: Color,
    val sleep: Color,
    val mood: Color,
    val body: Color,
    val held: Color,
    val slipped: Color,
    val rest: Color,
    /** The pink note the intention is read back on. */
    val note: Color,
    /** The blue strip / hero note. */
    val noteBlue: Color,
    val stamp: Color,
    val good: Color,
    val border: Dp,
    val radius: Dp,
    /** Chips and toggles are pills in the notebook, squared off on ink. */
    val pills: Boolean,
    val dark: Boolean,
)

/** Stark, for the few paper bits that show up on the chain side. */
val DarkPaper = PaperColors(
    gridFine = Color.Transparent,
    gridBold = Color.Transparent,
    card = Color(0xFF151513),
    cardLine = Color(0xFF34322E),
    segLine = Color(0xFF4A4740),
    shadow = Color(0x99000000),
    tapeA = Color(0xB3D4920A),
    tapeB = Color(0x80D4920A),
    polaroid = Color(0xFFEDEAE4),
    photo = Color(0xFFD9DEE6),
    polaroidInk = Color(0xFF26304A),
    sleep = Color(0xFF9D8FE6),
    mood = Color(0xFFEF86A3),
    body = Color(0xFF6FC293),
    held = Color(0xFF1F3E31),
    slipped = Color(0xFF45301F),
    rest = Color(0xFF2E2A52),
    note = Color(0xFF221D14),
    noteBlue = Color(0xFF171715),
    stamp = Color(0xFFD4920A),
    good = Color(0xFF7FD6A4),
    border = 1.dp,
    radius = 4.dp,
    pills = false,
    dark = true,
)

val LightPaper = DarkPaper.copy(
    card = Color(0xFFFFFFFF),
    cardLine = Color(0xFFD3CFC6),
    segLine = Color(0xFFB9B4AA),
    shadow = Color(0x22000000),
    note = Color(0xFFF6E2B8),
    noteBlue = Color(0xFFE7E4DD),
    stamp = Color(0xFF9A6A06),
    good = Color(0xFF3B8F62),
    dark = false,
)

/** The notebook, light: a blue grid page, white cards outlined in the character's navy ink. */
val NotebookLight = PaperColors(
    gridFine = Color(0xFF6C92BE).copy(alpha = 0.11f),
    gridBold = Color(0xFF6C92BE).copy(alpha = 0.26f),
    card = Color(0xFFFFFEFB),
    cardLine = Color(0xFF2B3550),
    segLine = Color(0xFF2B3550).copy(alpha = 0.7f),
    shadow = Color(0xFF2B3550).copy(alpha = 0.14f),
    tapeA = Color(0xFFBCD5EF),
    tapeB = Color(0xFFD9E8F7),
    polaroid = Color(0xFFFFFFFF),
    photo = Color(0xFFE7EFF8),
    polaroidInk = Color(0xFF26304A),
    sleep = Color(0xFFA99BE3),
    mood = Color(0xFFF08AA6),
    body = Color(0xFF78C49A),
    held = Color(0xFFD3EFDF),
    slipped = Color(0xFFFDE3CF),
    rest = Color(0xFFE4DDF8),
    note = Color(0xFFFBE3EA),
    noteBlue = Color(0xFFDCEAF8),
    stamp = Color(0xFFD9536F),
    good = Color(0xFF3B8F62),
    border = 1.6.dp,
    radius = 12.dp,
    pills = true,
    dark = false,
)

/** The notebook, dark: navy night paper. */
val NotebookDark = PaperColors(
    gridFine = Color(0xFF8CAAD7).copy(alpha = 0.065f),
    gridBold = Color(0xFF8CAAD7).copy(alpha = 0.15f),
    card = Color(0xFF192032),
    cardLine = Color(0xFF6F82A8),
    segLine = Color(0xFFC8D7F0).copy(alpha = 0.42f),
    shadow = Color.Black.copy(alpha = 0.4f),
    tapeA = Color(0xFF4A6A94),
    tapeB = Color(0xFF3A5478),
    polaroid = Color(0xFFE9EDF4),
    photo = Color(0xFFC9D7EA),
    polaroidInk = Color(0xFF26304A),
    sleep = Color(0xFF9D8FE6),
    mood = Color(0xFFEF86A3),
    body = Color(0xFF6FC293),
    held = Color(0xFF1F3E31),
    slipped = Color(0xFF45301F),
    rest = Color(0xFF2E2A52),
    note = Color(0xFF3A2533),
    noteBlue = Color(0xFF22304A),
    stamp = Color(0xFFF07A93),
    good = Color(0xFF7FD6A4),
    border = 1.6.dp,
    radius = 12.dp,
    pills = true,
    dark = true,
)

val LocalPaper = staticCompositionLocalOf { DarkPaper }
