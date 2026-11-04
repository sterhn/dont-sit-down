package com.dontsitdown.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dontsitdown.ui.theme.LocalPaper
import com.dontsitdown.ui.theme.Mono
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One-line toasts with a bit of personality, plus the occasional confetti. */
@Stable
class Toaster {
    var message by mutableStateOf<String?>(null)
        private set
    var serial by mutableIntStateOf(0)
        private set
    var confetti by mutableIntStateOf(0)
        private set

    fun show(text: String) {
        message = text
        serial++
    }

    fun celebrate() {
        confetti++
    }

    internal fun hide(at: Int) {
        if (serial == at) message = null
    }
}

val LocalToaster = staticCompositionLocalOf { Toaster() }

/** Draws the current toast and confetti over [content]. [bottom] keeps it clear of the tab bar. */
@Composable
fun BoxScope.ToastLayer(toaster: Toaster, bottom: Dp) {
    val msg = toaster.message
    val serial = toaster.serial
    LaunchedEffect(serial) {
        val m = toaster.message ?: return@LaunchedEffect
        delay(maxOf(2200L, m.length * 55L))
        toaster.hide(serial)
    }
    AnimatedVisibility(
        visible = msg != null,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + 12.dp, start = 16.dp, end = 16.dp),
    ) {
        // Keep showing the last message while it fades out.
        val last = remember { arrayOf("") }
        if (msg != null) last[0] = msg
        Box(
            Modifier
                .widthIn(max = 420.dp)
                .paperShadow(shape = 6.dp, color = LocalPaper.current.shadow)
                .background(MaterialTheme.colorScheme.onBackground, MaterialTheme.shapes.large)
                .padding(horizontal = 18.dp, vertical = 11.dp),
        ) {
            Text(last[0], style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.background, textAlign = TextAlign.Center)
        }
    }
    if (toaster.confetti > 0) Confetti(toaster.confetti)
}

@Composable
private fun Confetti(trigger: Int) {
    val paper = LocalPaper.current
    val colors = listOf(paper.sleep, paper.mood, paper.body, MaterialTheme.colorScheme.primary, Color(0xFF8DB4E8))
    val pieces = remember(trigger) {
        List(28) { i ->
            val a = Random.nextFloat() * 6.283f
            val r = 90f + Random.nextFloat() * 130f
            Triple(Offset(cos(a) * r, sin(a) * r + 60f), colors[i % colors.size], i % 3 == 0)
        }
    }
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    if (t.value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2, size.height * 0.45f)
        val k = 1.dp.toPx()
        pieces.forEach { (d, color, round) ->
            val p = c + d * k * t.value
            val s = 9.dp.toPx()
            rotate(260f * t.value, p) {
                if (round) drawCircle(color.copy(alpha = 1f - t.value), s / 2, p)
                else drawRect(color.copy(alpha = 1f - t.value), p - Offset(s / 2, s / 2), Size(s, s))
            }
        }
    }
}
