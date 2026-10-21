package com.dontsitdown.ui.chain

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dontsitdown.chain.SessionState
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun TimerScreen(s: SessionState, onDone: () -> Unit, onBail: () -> Unit) {
    val task = s.current ?: return
    KeepScreenOn()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.currentStartedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val elapsed = (now - s.currentStartedAt).coerceAtLeast(0)
    val estimateMs = (task.estimate * 60_000).toLong().coerceAtLeast(60_000)
    var confirmBail by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (s.plan.size > 1) "${s.index + 1} of ${s.plan.size}" else "just one",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { confirmBail = true }) {
                Icon(Icons.Filled.Close, contentDescription = "stop", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(task.task.name, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimerRing(elapsed, estimateMs, Modifier.size(290.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(TimeUtils.clock(elapsed), style = Nums.huge)
                val over = elapsed - estimateMs
                Text(
                    if (over > 0) "+${TimeUtils.clock(over)} over" else "est ${TimeUtils.clock(estimateMs)}",
                    style = Nums.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            s.quip,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        BigButton("done", onClick = onDone)
        Spacer(Modifier.height(32.dp))
    }

    if (confirmBail) {
        AlertDialog(
            onDismissRequest = { confirmBail = false },
            title = { Text("stop here?") },
            text = { Text(if (s.runs.isEmpty()) "this one won't count." else "what's done stays done. this one won't count.") },
            confirmButton = { TextButton(onClick = { confirmBail = false; onBail() }) { Text("stop") } },
            dismissButton = { TextButton(onClick = { confirmBail = false }) { Text("keep going") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

/**
 * Count-up ring. The estimate sits at three-quarters of the way round as a tick,
 * so you can see yourself pass it; past that, the scale stretches with you.
 */
@Composable
private fun TimerRing(elapsedMs: Long, estimateMs: Long, modifier: Modifier) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val amber = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onBackground

    Canvas(modifier) {
        val stroke = 10.dp.toPx()
        val scale = max(estimateMs * 4f / 3f, elapsedMs * 1.04f)
        val inset = stroke / 2 + 6.dp.toPx()
        val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(amber.copy(alpha = pulse), -90f, 360f * (elapsedMs / scale), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))

        // Estimate marker: a short radial tick across the ring.
        val angle = Math.toRadians((-90.0 + 360.0 * (estimateMs / scale)))
        val r = arcSize.width / 2
        val c = Offset(size.width / 2, size.height / 2)
        val outer = r + stroke * 0.9f
        val inner = r - stroke * 0.9f
        drawLine(
            tick.copy(alpha = 0.75f),
            Offset(c.x + (inner * cos(angle)).toFloat(), c.y + (inner * sin(angle)).toFloat()),
            Offset(c.x + (outer * cos(angle)).toFloat(), c.y + (outer * sin(angle)).toFloat()),
            strokeWidth = 2.5.dp.toPx(),
        )
    }
}

@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
