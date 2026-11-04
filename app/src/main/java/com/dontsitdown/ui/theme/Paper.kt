package com.dontsitdown.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The notebook half of the look, on top of ink + amber: graph-paper lines, tape, the polaroid,
 * and three soft data colours (sleep / mood / body) that came over from the check-in.
 */
@Immutable
data class PaperColors(
    val gridFine: Color,
    val gridBold: Color,
    val card: Color,
    val cardLine: Color,
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
    val note: Color,
    val stamp: Color,
    val good: Color,
)

val DarkPaper = PaperColors(
    gridFine = Color(0x0DEDEAE4),
    gridBold = Color(0x1CEDEAE4),
    card = Color(0xFF151513),
    cardLine = Color(0xFF34322E),
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
    stamp = Color(0xFFD4920A),
    good = Color(0xFF7FD6A4),
)

val LightPaper = PaperColors(
    gridFine = Color(0x146C92BE),
    gridBold = Color(0x306C92BE),
    card = Color(0xFFFFFEFB),
    cardLine = Color(0xFF2B3550).copy(alpha = 0.55f),
    shadow = Color(0x242B3550),
    tapeA = Color(0xCCE8C77A),
    tapeB = Color(0xB3F3DFA8),
    polaroid = Color(0xFFFFFFFF),
    photo = Color(0xFFE7EFF8),
    polaroidInk = Color(0xFF26304A),
    sleep = Color(0xFFA99BE3),
    mood = Color(0xFFF08AA6),
    body = Color(0xFF78C49A),
    held = Color(0xFFD3EFDF),
    slipped = Color(0xFFFDE3CF),
    rest = Color(0xFFE4DDF8),
    note = Color(0xFFFBEFD0),
    stamp = Color(0xFF9A6A06),
    good = Color(0xFF3B8F62),
)

val LocalPaper = staticCompositionLocalOf { DarkPaper }
