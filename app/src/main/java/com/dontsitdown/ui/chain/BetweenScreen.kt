package com.dontsitdown.ui.chain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dontsitdown.chain.SessionState
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.SectionLabel
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils

@Composable
fun BetweenScreen(s: SessionState, onNext: () -> Unit, onEnough: () -> Unit) {
    val run = s.runs.lastOrNull() ?: return
    val next = s.next ?: return
    KeepScreenOn()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(72.dp))
        Text("done.", style = MaterialTheme.typography.displayLarge)
        Text(TimeUtils.minutesLabel(run.actual) + ".", style = Nums.large, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(s.quip, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.weight(1f))
        SectionLabel("next · ${s.index + 2} of ${s.plan.size}")
        Rule()
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(next.task.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            Text("${TimeUtils.wholeMinutes(next.estimate)}m", style = Nums.medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Rule()
        Spacer(Modifier.height(32.dp))
        BigButton("keep moving", onClick = onNext)
        QuietAction("that's enough for now", onClick = onEnough, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
    }
}
