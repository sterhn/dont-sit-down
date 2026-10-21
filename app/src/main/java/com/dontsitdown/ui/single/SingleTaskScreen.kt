package com.dontsitdown.ui.single

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.app
import com.dontsitdown.chain.SessionMode
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.TopBar
import com.dontsitdown.ui.tasks.dueColor
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils

/** "Just one": no chain, no pressure. Pick it, time it, done. */
@Composable
fun SingleTaskScreen(onBack: () -> Unit, onStarted: () -> Unit) {
    val app = LocalContext.current.app
    val tasks by app.repository.observeTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val sorted = tasks.filter { it.isActive }.sortedWith(compareBy({ TimeUtils.dueInDays(it) ?: 999 }, { it.effectiveMinutes }))

    Column(Modifier.fillMaxSize()) {
        TopBar("just one", onBack = onBack)
        Text(
            "no chain. no pressure. pick one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            items(sorted, key = { it.id }) { t ->
                val due = TimeUtils.dueInDays(t)
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (app.session.state.value?.isRunning != true) {
                            app.session.plan(listOf(t), SessionMode.SINGLE)
                            app.session.begin()
                        }
                        onStarted()
                    }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.titleMedium)
                        Text(TimeUtils.dueLabel(due), style = MaterialTheme.typography.labelSmall, color = dueColor(due))
                    }
                    Text("${TimeUtils.wholeMinutes(t.effectiveMinutes)}m", style = Nums.medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Rule(Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}
