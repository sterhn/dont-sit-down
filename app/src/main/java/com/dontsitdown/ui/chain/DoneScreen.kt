package com.dontsitdown.ui.chain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.chain.SessionMode
import com.dontsitdown.chain.SessionState
import com.dontsitdown.data.model.Task
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.SectionLabel
import com.dontsitdown.ui.theme.Mono
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun DoneScreen(s: SessionState, vm: ChainViewModel) {
    val results by vm.results.collectAsStateWithLifecycle(initialValue = emptyList())
    var oneMore by remember { mutableStateOf<Task?>(null) }
    LaunchedEffect(s.runs.size) { oneMore = vm.oneMoreCandidate(s) }
    val single = s.mode == SessionMode.SINGLE && s.runs.size == 1

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(56.dp))
        Text(s.quip, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(TimeUtils.minutesLabel(s.totalActual), style = Nums.large, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                "actual · ${TimeUtils.wholeMinutes(s.totalEstimate)} min estimated",
                style = Nums.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        if (!single) {
            SectionLabel("breakdown")
            s.runs.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(TimeUtils.minutesLabel(r.actual), style = Nums.medium)
                    Text(
                        "  / ${TimeUtils.wholeMinutes(r.estimate)}",
                        style = Nums.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Rule()
            }
        }

        if (s.combo >= 2) Combo(s)
        if (s.dread > 0) {
            val chains = results.filter { !it.isSingle }
            val dreads = chains.filter { it.totalDreadBefore > 0 }.map { it.totalDreadBefore.toDouble() }
            val avgDread = dreads.takeIf { it.isNotEmpty() }?.average() ?: s.dread.toDouble()
            val avgChain = chains.map { it.totalActual.toDouble() }.takeIf { it.isNotEmpty() }?.average() ?: s.totalActual.toDouble()
            DreadPayoff(s, avgDread, avgChain)
        }

        Spacer(Modifier.height(40.dp))
        BigButton(if (single) "back" else "sit down", onClick = vm::sitDown)
        oneMore?.let { t ->
            QuietAction(
                "${MessageGenerator.oneMorePrompt()} ${t.name} · ${TimeUtils.wholeMinutes(t.effectiveMinutes)}m",
                onClick = { vm.oneMore(t) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** The punchline. Allowed to be dramatic. */
@Composable
private fun Combo(s: SessionState) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(450)
        show = true
    }
    Spacer(Modifier.height(28.dp))
    Text(
        s.runs.joinToString("  →  ") { "${it.name} ✓ (${TimeUtils.wholeMinutes(it.actual)}m)" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    AnimatedVisibility(
        visible = show,
        enter = scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
    ) {
        Column(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                "🔥 ${s.combo}-TASK COMBO",
                fontFamily = Mono,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                "TOTAL: ${TimeUtils.wholeMinutes(s.totalActual)} MIN",
                fontFamily = Mono,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                MessageGenerator.comboLabel(s.combo).uppercase(),
                fontFamily = Mono,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun DreadPayoff(s: SessionState, avgDread: Double, avgChain: Double) {
    SectionLabel("reality check")
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(18.dp),
    ) {
        Line("dread level", "${MessageGenerator.dreadSkulls(s.dread)} ${MessageGenerator.dreadLevelName(s.dread)}")
        Line("actual time", "${TimeUtils.wholeMinutes(s.totalActual)} minutes")
        Line("reality check", MessageGenerator.realityCheck(s.dread, s.totalActual, s.totalEstimate))
        Spacer(Modifier.height(14.dp))
        Line("your average dread", String.format(Locale.US, "%.1f 💀", avgDread))
        Line("your average chain", "${TimeUtils.wholeMinutes(avgChain.toFloat())} minutes")
        Spacer(Modifier.height(6.dp))
        Text(
            "conclusion: ${MessageGenerator.dreadConclusion(avgDread, avgChain)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(150.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
