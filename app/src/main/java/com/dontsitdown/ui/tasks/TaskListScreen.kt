package com.dontsitdown.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.app
import com.dontsitdown.data.model.Task
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.TopBar
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TaskListScreen(onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val repo = LocalContext.current.app.repository
    val scope = rememberCoroutineScope()
    val tasks by repo.observeTasks().collectAsStateWithLifecycle(initialValue = null)
    var list by remember { mutableStateOf<List<Task>>(emptyList()) }
    LaunchedEffect(tasks) { tasks?.let { list = it } }

    val lazyState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyState) { from, to ->
        val a = list.indexOfFirst { it.id == from.key }
        val b = list.indexOfFirst { it.id == to.key }
        if (a >= 0 && b >= 0) list = list.toMutableList().apply { add(b, removeAt(a)) }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("tasks") {
            IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "add task") }
        }
        Text(
            "drag ≡ to sort. higher = picked a bit more often.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyColumn(state = lazyState, modifier = Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { t ->
                ReorderableItem(reorderState, key = t.id) { dragging ->
                    Column(Modifier.background(if (dragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(t.id) }.padding(start = 20.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TaskRowContent(t, Modifier.weight(1f))
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(onDragStopped = { scope.launch { repo.reorder(list.map { it.id }) } }),
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Rule(Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun dueColor(days: Int?): Color = when {
    days == null -> MaterialTheme.colorScheme.onSurfaceVariant
    days < 0 -> MaterialTheme.colorScheme.primary
    days == 0 -> MaterialTheme.colorScheme.onBackground
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun TaskRowContent(t: Task, modifier: Modifier) {
    val due = TimeUtils.dueInDays(t)
    Row(modifier.alpha(if (t.isActive) 1f else 0.4f), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(t.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${t.frequency.label} · last done ${TimeUtils.lastDoneLabel(t.lastDone)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${TimeUtils.wholeMinutes(t.effectiveMinutes)}m", style = Nums.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (t.isActive) TimeUtils.dueLabel(due) else "off",
                style = MaterialTheme.typography.labelSmall,
                color = if (t.isActive) dueColor(due) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
