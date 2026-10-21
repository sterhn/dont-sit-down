package com.dontsitdown.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.app
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.chain.SessionMode
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.SectionLabel
import com.dontsitdown.ui.components.TopBar
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TaskDetailScreen(id: String, onBack: () -> Unit, onEdit: (String) -> Unit, onStart: () -> Unit) {
    val app = LocalContext.current.app
    val repo = app.repository
    val scope = rememberCoroutineScope()
    val task by repo.observeTask(id).collectAsStateWithLifecycle(initialValue = null)
    val history by repo.observeCompletions(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val t = task ?: return

    val count = history.size
    val total = history.sumOf { it.minutes.toDouble() }.toFloat()
    val avg = if (count > 0) total / count else null
    val fastest = history.minByOrNull { it.minutes }
    val slowest = history.maxByOrNull { it.minutes }
    val slowNote = remember(slowest?.id, avg) { if (slowest != null && avg != null && count >= 3) MessageGenerator.slowestNote(slowest.minutes, avg) else null }
    val due = TimeUtils.dueInDays(t)

    Column(Modifier.fillMaxSize()) {
        TopBar("", onBack = onBack) {
            IconButton(onClick = { onEdit(t.id) }) { Icon(Icons.Filled.Edit, contentDescription = "edit") }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding(),
        ) {
            Text(t.name, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${t.frequency.label} · ${if (t.isActive) TimeUtils.dueLabel(due) else "switched off"}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (t.isActive) dueColor(due) else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionLabel("actually, how long")
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium).padding(18.dp),
            ) {
                Stat("estimate", "${t.estimateMinutes} min")
                Stat("your average", avg?.let { String.format(Locale.US, "%.1f min", it) } ?: "—")
                Stat("fastest", fastest?.let { "${TimeUtils.minutesLabel(it.minutes)} (${TimeUtils.dateLabel(it.date)})" } ?: "—")
                Stat("slowest", slowest?.let { "${TimeUtils.minutesLabel(it.minutes)}" + (slowNote?.let { n -> " $n" } ?: " (${TimeUtils.dateLabel(it.date)})") } ?: "—")
                Spacer(Modifier.height(14.dp))
                if (count > 0) {
                    Text("you've done this $count time${if (count == 1) "" else "s"} with this app.", style = MaterialTheme.typography.bodyLarge)
                    Text("total: ${TimeUtils.durationLabel(total)} of your life.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                }
                Text(MessageGenerator.lifetimeKicker(count, total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (history.isNotEmpty()) {
                SectionLabel("history")
                history.take(12).forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(TimeUtils.dateLabel(c.date), style = Nums.small, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(90.dp))
                        Text(if (c.chainId != null) "in a chain" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(TimeUtils.minutesLabel(c.minutes), style = Nums.small)
                    }
                    Rule()
                }
            }

            Spacer(Modifier.height(32.dp))
            if (t.isActive) {
                BigButton("just this one", onClick = {
                    app.session.plan(listOf(t), SessionMode.SINGLE)
                    app.session.begin()
                    onStart()
                })
            }
            QuietAction(
                if (t.isActive) "switch off (keeps history)" else "switch back on",
                onClick = { scope.launch { repo.save(t.copy(isActive = !t.isActive)) } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(120.dp))
        Text(value, style = Nums.medium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize))
    }
}
