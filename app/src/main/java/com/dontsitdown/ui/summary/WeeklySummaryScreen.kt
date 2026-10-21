package com.dontsitdown.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.app
import com.dontsitdown.chain.SummaryCalculator
import com.dontsitdown.chain.WeekSummary
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.SectionLabel
import com.dontsitdown.ui.components.TopBar
import com.dontsitdown.ui.theme.Mono
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils
import java.time.DayOfWeek

@Composable
fun WeeklySummaryScreen() {
    val repo = LocalContext.current.app.repository
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val weekStart = remember(offset) { TimeUtils.weekStart(TimeUtils.today()).plusWeeks(offset.toLong()) }
    // Re-run whenever this week's log changes.
    val log by remember(weekStart) {
        repo.observeCompletionsBetween(TimeUtils.startOfDay(weekStart), TimeUtils.startOfDay(weekStart.plusDays(7)))
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val summary by produceState<WeekSummary?>(null, weekStart, log) { value = SummaryCalculator.week(repo, weekStart) }

    Column(Modifier.fillMaxSize()) {
        TopBar("week") {
            IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "previous week") }
            IconButton(onClick = { offset++ }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "next week") }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text(
                "${TimeUtils.dateLabel(weekStart)} – ${TimeUtils.dateLabel(weekStart.plusDays(6))}" + if (offset == 0) " · this week" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val s = summary ?: return@Column
            Spacer(Modifier.height(16.dp))
            if (s.tasksDone == 0 && s.score.couch == 0) {
                Text("nothing logged yet.", style = MaterialTheme.typography.headlineMedium)
                Text("the grid fills itself in as you go.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${s.chains}", style = Nums.large)
                Text(" chain${if (s.chains == 1) "" else "s"}  ·  ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${s.totalMinutes.toInt()}", style = Nums.large)
                Text(" min total", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "the couch: ${s.score.couch}, you: ${s.score.you}",
                style = MaterialTheme.typography.titleLarge,
                color = if (s.score.you >= s.score.couch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
            if (s.score.skipped.isNotEmpty()) {
                Text(
                    "skipped: " + s.score.skipped.joinToString(", ") { TimeUtils.dayName(it.dayOfWeek) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (s.notable.isNotEmpty()) {
                SectionLabel("the big ones")
                s.notable.forEach { Text("$it ✓", style = MaterialTheme.typography.bodyLarge) }
            }

            SectionLabel("reality")
            s.fastest?.let { (m, d) -> Fact("fastest chain", "${TimeUtils.wholeMinutes(m)} min (${TimeUtils.dayName(d.dayOfWeek)})") }
            s.avgReality?.let { Fact("avg chain", "${TimeUtils.wholeMinutes(it.toFloat())} min") }
            s.avgDread?.let { Fact("avg dread", String.format(java.util.Locale.US, "%.1f 💀", it)) }
            Fact("conclusion", s.conclusion)

            SectionLabel("the log")
            Grid(s)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A factual log, not a streak chart: which days you did what, and for how long. */
@Composable
private fun Grid(s: WeekSummary) {
    val cell = 34.dp
    val today = TimeUtils.today()
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(130.dp))
            DayOfWeek.entries.forEachIndexed { i, d ->
                val isToday = s.weekStart.plusDays(i.toLong()) == today
                Text(
                    TimeUtils.dayInitial(d).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(cell),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Rule(Modifier.width(130.dp + cell * 7))
        s.grid.forEach { row ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(row.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(130.dp).padding(end = 8.dp))
                row.minutesByDay.forEach { m ->
                    Box(Modifier.width(cell), contentAlignment = Alignment.Center) {
                        if (m != null) {
                            Box(
                                Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${TimeUtils.wholeMinutes(m)}", fontFamily = Mono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        } else {
                            Text("·", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
        if (s.score.skipped.isNotEmpty()) {
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("the couch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(130.dp))
                (0..6).forEach { i ->
                    val skipped = s.weekStart.plusDays(i.toLong()) in s.score.skipped
                    Box(Modifier.width(cell), contentAlignment = Alignment.Center) {
                        Text(if (skipped) "×" else "", fontFamily = Mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
