package com.dontsitdown.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dontsitdown.checkin.Doodle
import com.dontsitdown.ui.theme.DarkPaper
import com.dontsitdown.ui.theme.LocalPaper

/** Fill roles, so the same drawing works on paper and on ink. */
private enum class Fill { NONE, CARD, INK, PINK, PINK_T, PEACH, PEACH_T, YELLOW_T, LAV_T, MINT_T, BLUE_T }

private class Part(val d: String, val fill: Fill = Fill.NONE, val stroke: Boolean = true, val dash: FloatArray? = null, val alpha: Float = 1f, val width: Float = 2.2f)

private fun circle(cx: Float, cy: Float, r: Float) = ellipse(cx, cy, r, r)
private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) =
    "M${cx - rx} $cy A$rx $ry 0 1 0 ${cx + rx} $cy A$rx $ry 0 1 0 ${cx - rx} $cy Z"
private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} $y H${x + w - r} A$r $r 0 0 1 ${x + w} ${y + r} V${y + h - r} A$r $r 0 0 1 ${x + w - r} ${y + h} H${x + r} A$r $r 0 0 1 $x ${y + h - r} V${y + r} A$r $r 0 0 1 ${x + r} $y Z"

/** The web app's ink doodles, 64×64, one per goal. */
private val ART: Map<Doodle, List<Part>> = mapOf(
    Doodle.MAP to listOf(
        Part("M5 17 L23 11 L41 17 L59 11 L59 49 L41 55 L23 49 L5 55 Z", Fill.CARD),
        Part("M23 11 V49 M41 17 V55"),
        Part("M11 45 C17 37, 25 43, 31 35 C35 30, 40 31, 44 27", dash = floatArrayOf(3f, 4f)),
        Part("M49 29 C45 24, 43 21, 43 18 A6 6 0 1 1 55 18 C55 21, 53 24, 49 29 Z", Fill.PINK),
        Part(circle(49f, 18f, 2f), Fill.INK, stroke = false),
    ),
    Doodle.RAMEN to listOf(
        Part("M36 4 L52 30 M43 3 L57 27"),
        Part("M6 31 H58 C58 46, 47 57, 32 57 C17 57, 6 46, 6 31 Z", Fill.PINK_T),
        Part(ellipse(32f, 31f, 26f, 5f), Fill.YELLOW_T),
        Part(ellipse(22f, 30f, 6f, 3.4f), Fill.CARD),
        Part(circle(22f, 30f, 1.8f), Fill.PEACH, stroke = false),
        Part("M31 31 q3 -3 6 0 t6 0"),
        Part("M14 43 H50", dash = floatArrayOf(2f, 4f), alpha = 0.6f),
        Part("M18 21 q-3 -4 0 -8 M26 19 q-3 -4 0 -8", alpha = 0.55f, width = 1.8f),
    ),
    Doodle.BOOK to listOf(
        Part("M14 11 H50 A4 4 0 0 1 54 15 V51 A4 4 0 0 1 50 55 H14 Z", Fill.CARD),
        Part(rect(10f, 7f, 40f, 46f, 4f), Fill.LAV_T),
        Part("M17 8 V52"),
        Part(rect(24f, 17f, 19f, 8f, 2f), Fill.CARD),
        Part("M38 53 V61 L41 58 L44 61 V53", Fill.PINK),
    ),
    Doodle.STAR to listOf(
        Part("M32 8 L38.5 22.6 L54.4 24.2 L42.4 34.8 L45.9 50.4 L32 42.4 L18.1 50.4 L21.6 34.8 L9.6 24.2 L25.5 22.6 Z", Fill.YELLOW_T),
        Part("M44 9 l2 -4 M51 15 l4 -2", alpha = 0.7f),
    ),
    Doodle.HEART to listOf(
        Part("M32 54 C15 43, 7 33, 10 23 C13 13, 27 12, 32 22 C37 12, 51 13, 54 23 C57 33, 49 43, 32 54 Z", Fill.PINK_T),
        Part("M18 24 Q19 19 24 18", alpha = 0.6f),
    ),
    Doodle.CLOUD to listOf(
        Part("M16 46 C8 46, 6 36, 13 33 C12 24, 22 19, 28 24 C31 15, 45 15, 47 25 C55 24, 59 33, 54 38 C59 42, 55 48, 49 46 Z", Fill.BLUE_T),
        Part("M22 55 l-2 4 M32 55 l-2 4 M42 55 l-2 4", alpha = 0.6f),
    ),
    Doodle.FLOWER to listOf(
        Part("M32 40 V60 M32 52 Q40 48 44 52"),
        Part(circle(32f, 14f, 8f), Fill.PINK_T), Part(circle(45f, 23f, 8f), Fill.PINK_T), Part(circle(40f, 37f, 8f), Fill.PINK_T),
        Part(circle(24f, 37f, 8f), Fill.PINK_T), Part(circle(19f, 23f, 8f), Fill.PINK_T),
        Part(circle(32f, 27f, 7f), Fill.YELLOW_T),
    ),
    Doodle.BEAR to listOf(
        Part(circle(17f, 19f, 7f), Fill.PEACH_T), Part(circle(47f, 19f, 7f), Fill.PEACH_T),
        Part(circle(32f, 35f, 20f), Fill.PEACH_T),
        Part(ellipse(32f, 42f, 7f, 5f), Fill.CARD),
        Part(ellipse(32f, 40f, 2.2f, 1.6f), Fill.INK, stroke = false),
        Part(circle(24.5f, 32f, 1.9f), Fill.INK, stroke = false), Part(circle(39.5f, 32f, 1.9f), Fill.INK, stroke = false),
    ),
)

private val PARSED = ART.mapValues { (_, parts) -> parts.map { it to PathParser().parsePathString(it.d).toPath() } }

/** Background tint for a goal's strip, matching its doodle. */
@Composable
fun doodleTint(d: Doodle): Color {
    val dark = LocalPaper.current === DarkPaper
    return fillColor(
        when (d) {
            Doodle.MAP, Doodle.STAR -> Fill.MINT_T
            Doodle.RAMEN -> Fill.YELLOW_T
            Doodle.BOOK -> Fill.LAV_T
            Doodle.BEAR, Doodle.CLOUD -> Fill.BLUE_T
            Doodle.HEART, Doodle.FLOWER -> Fill.PINK_T
        },
        dark,
        Color.Unspecified,
        Color.Unspecified,
    )
}

private fun fillColor(f: Fill, dark: Boolean, card: Color, ink: Color): Color = if (dark) when (f) {
    Fill.NONE -> Color.Transparent
    Fill.CARD -> card
    Fill.INK -> ink
    Fill.PINK -> Color(0xFFEF86A3)
    Fill.PINK_T -> Color(0xFF4A2535)
    Fill.PEACH -> Color(0xFFF0A06A)
    Fill.PEACH_T -> Color(0xFF45301F)
    Fill.YELLOW_T -> Color(0xFF3D3620)
    Fill.LAV_T -> Color(0xFF2E2A52)
    Fill.MINT_T -> Color(0xFF1F3E31)
    Fill.BLUE_T -> Color(0xFF22304A)
} else when (f) {
    Fill.NONE -> Color.Transparent
    Fill.CARD -> card
    Fill.INK -> ink
    Fill.PINK -> Color(0xFFF08AA6)
    Fill.PINK_T -> Color(0xFFFBD8E2)
    Fill.PEACH -> Color(0xFFF4A36D)
    Fill.PEACH_T -> Color(0xFFFDE3CF)
    Fill.YELLOW_T -> Color(0xFFFBEFC4)
    Fill.LAV_T -> Color(0xFFE4DDF8)
    Fill.MINT_T -> Color(0xFFD3EFDF)
    Fill.BLUE_T -> Color(0xFFDCEAF8)
}

@Composable
fun DoodleIcon(d: Doodle, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val paper = LocalPaper.current
    val dark = paper === DarkPaper
    val ink = if (dark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f) else Color(0xFF2B3550)
    val card = paper.card
    val parts = PARSED.getValue(d)
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 64f
        scale(k, k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            for ((p, path) in parts) {
                if (p.fill != Fill.NONE) drawPath(path, fillColor(p.fill, dark, card, ink), alpha = p.alpha)
                if (p.stroke) drawPath(
                    path, ink, alpha = p.alpha,
                    style = Stroke(width = p.width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = p.dash?.let { PathEffect.dashPathEffect(it) }),
                )
            }
        }
    }
}
