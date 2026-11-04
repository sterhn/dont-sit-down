package com.dontsitdown.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Numbers and timers are monospace: slightly technical, never cute. */
val Mono = FontFamily.Monospace

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.02).em),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.12.em),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.1.em),
    labelSmall = TextStyle(fontFamily = Mono, fontSize = 11.sp, letterSpacing = 0.08.em),
)

object Nums {
    val huge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-0.02).em)
    val large = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 28.sp)
    val medium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 17.sp)
    val small = TextStyle(fontFamily = Mono, fontSize = 13.sp)
}

/** Gaegu, subset to latin: the character's handwriting. Captions, scale values, little notes. Never body text. */
val Hand = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.dontsitdown.R.font.gaegu_bold, FontWeight.Bold))

object HandText {
    val huge = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 40.sp)
    val large = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 30.sp)
    val medium = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 25.sp)
    val small = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 21.sp)
}
