package com.dontsitdown.ui.checkin

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.app
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.Face
import com.dontsitdown.checkin.Goal
import com.dontsitdown.checkin.LogEntry
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.CardTitle
import com.dontsitdown.ui.components.Chip
import com.dontsitdown.ui.components.DoodleIcon
import com.dontsitdown.ui.components.LocalToaster
import com.dontsitdown.ui.components.NoteField
import com.dontsitdown.ui.components.OutlineButton
import com.dontsitdown.ui.components.PaperCard
import com.dontsitdown.ui.components.Polaroid
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.SectionLabel
import com.dontsitdown.ui.components.Segmented
import com.dontsitdown.ui.components.Stepper
import com.dontsitdown.ui.components.ThinBar
import com.dontsitdown.ui.components.Tile
import com.dontsitdown.ui.components.TopBar
import com.dontsitdown.ui.components.doodleTint
import com.dontsitdown.ui.components.paperShadow
import com.dontsitdown.ui.theme.HandText
import com.dontsitdown.ui.theme.LocalPaper
import com.dontsitdown.util.TimeUtils
import java.time.LocalDate

/** Small adventures, week by week: slim strips, a doodle on each. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val data by LocalContext.current.app.checkin.state.collectAsStateWithLifecycle()
    val today = com.dontsitdown.ui.components.rememberToday()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(28.dp))
        Text("Small adventures, week by week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Goals", style = MaterialTheme.typography.displayMedium)

        val weekly = data.goals.filter { !it.monthly }
        val monthly = data.goals.filter { it.monthly }
        if (weekly.isNotEmpty()) {
            SectionLabel("This week")
            weekly.forEachIndexed { i, g -> GoalRow(data, g, today, i, onOpen) }
        }
        if (monthly.isNotEmpty()) {
            SectionLabel("This month")
            monthly.forEachIndexed { i, g -> GoalRow(data, g, today, i, onOpen) }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { Text("+ Add a goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        val counts = data.goals.map { it to Checkin.entries(data, it).size }.filter { it.second > 0 }
        if (counts.isNotEmpty()) {
            SectionLabel("So far")
            counts.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (g, n) -> Tile("$n", Checkin.collectionName(g), Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun GoalRow(data: CheckinData, g: Goal, today: LocalDate, i: Int, onOpen: (String) -> Unit) {
    val n = Checkin.periodCount(data, g, Checkin.periodOf(g, today))
    val met = n >= g.target
    val doodle = Checkin.doodleFor(data, g)
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .rotate(if (i % 2 == 0) -0.45f else 0.45f)
            .paperShadow(color = LocalPaper.current.shadow)
            .background(doodleTint(doodle), shape)
            .border(1.dp, LocalPaper.current.cardLine, shape)
            .clickable { onOpen(g.id) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DoodleIcon(doodle)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(g.title.replaceFirstChar { it.uppercase() }, style = HandText.medium)
            Text(
                if (met) "Done this ${g.period}" else "$n of ${g.target} · ${Checkin.periodLeft(g, today)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (met) {
            Box(Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text("✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** One goal: log it, and see what's been logged. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalDetailScreen(id: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = context.app.checkin
    val data by store.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val haptics = LocalHapticFeedback.current
    val today = remember { TimeUtils.today() }
    val g = data.goals.find { it.id == id }
    if (g == null) {
        Column(Modifier.fillMaxSize()) { TopBar("goal", onBack = onBack) }
        return
    }
    val p = Checkin.periodOf(g, today)
    val all = Checkin.entries(data, g).sortedByDescending { it.date }
    val cur = all.filter { runCatching { Checkin.periodOf(g, Checkin.date(it.date)) }.getOrNull() == p }
    val earlier = all - cur.toSet()
    val n = cur.size
    val met = n >= g.target
    val doodle = Checkin.doodleFor(data, g)
    val total = all.size

    var text by remember { mutableStateOf("") }
    var fieldKey by remember { mutableIntStateOf(0) }
    var tag by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf(today) }
    var confirmRemove by remember { mutableStateOf(false) }
    var removeEntry by remember { mutableStateOf<LogEntry?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopBar("", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp)) {
            PaperCard(color = doodleTint(doodle), tilt = -0.4f) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    DoodleIcon(doodle, size = 88.dp)
                    Text(g.title.replaceFirstChar { it.uppercase() }, style = HandText.large, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(
                            if (g.monthly) "Monthly" else "Weekly",
                            if (g.target > 1) "${g.target} times" else null,
                            if (met) "done this ${g.period}" else Checkin.periodLeft(g, today),
                            if (total > 0) "$total logged so far" else null,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    ThinBar(n.toFloat() / g.target, Modifier.width(220.dp))
                }
            }

            if (g.currentLabel != null) {
                Spacer(Modifier.height(16.dp))
                PaperCard(tilt = 0.4f) {
                    CardTitle(g.currentLabel)
                    Spacer(Modifier.height(8.dp))
                    NoteField(g.current ?: "", { store.setCurrent(g.id, it) }, "Title, author…", singleLine = true, key = g.id)
                }
            }

            Spacer(Modifier.height(16.dp))
            PaperCard(tilt = -0.3f, tape = true) {
                CardTitle("Log it")
                Spacer(Modifier.height(8.dp))
                val submit = {
                    val t = text.trim()
                    if (t.isEmpty()) toaster.show("Write a few words first")
                    else {
                        val before = Checkin.periodCount(store.current, g, p)
                        store.log(g.id, date, t, tag)
                        text = ""; tag = null; fieldKey++
                        val completed = Checkin.periodOf(g, date) == p && before < g.target && before + 1 >= g.target
                        if (completed) {
                            toaster.show("Done for this ${g.period}")
                            toaster.celebrate()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else toaster.show("Logged")
                        date = today
                    }
                }
                NoteField("", { text = it }, g.prompt ?: "What did you do?", singleLine = true, key = fieldKey, onDone = submit)
                if (!g.tags.isNullOrEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        g.tags.forEach { t -> Chip(t, tag == t, { tag = if (tag == t) null else t }) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip("Today", date == today, { date = today })
                    Chip("Yesterday", date == today.minusDays(1), { date = today.minusDays(1) })
                    val other = date != today && date != today.minusDays(1)
                    Chip(if (other) Checkin.dayMonth(date) else "Other day…", other, {
                        DatePickerDialog(context, { _, y, mo, d -> date = LocalDate.of(y, mo + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
                            datePicker.maxDate = System.currentTimeMillis()
                        }.show()
                    })
                }
                Spacer(Modifier.height(14.dp))
                BigButton("Save", onClick = submit)
            }

            if (cur.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                PaperCard(tilt = 0.4f) {
                    CardTitle("This ${g.period}", trailing = { Text("$n of ${g.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) })
                    cur.forEach { EntryRow(it, today, false) { removeEntry = it } }
                }
            }
            if (earlier.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                PaperCard(tilt = -0.4f) {
                    CardTitle("Earlier", trailing = { Text("${earlier.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) })
                    earlier.forEach { EntryRow(it, today, true) { removeEntry = it } }
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                QuietAction("Remove this goal", onClick = { confirmRemove = true }, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmRemove) {
        Confirm(
            "Remove \"${g.title}\"?",
            "Everything you logged for it is deleted too.",
            "Remove",
            onYes = { confirmRemove = false; store.removeGoal(g.id); onBack() },
            onNo = { confirmRemove = false },
        )
    }
    removeEntry?.let { e ->
        Confirm("Delete \"${e.text}\"?", null, "Delete", onYes = { store.removeEntry(e.id); removeEntry = null }, onNo = { removeEntry = null })
    }
}

@Composable
private fun EntryRow(e: LogEntry, today: LocalDate, withMonth: Boolean, onRemove: () -> Unit) {
    val d = runCatching { Checkin.date(e.date) }.getOrNull()
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            d?.let { if (withMonth) Checkin.short(it, today) else Checkin.short(it, today).substringBeforeLast(" ") } ?: e.date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(78.dp),
        )
        Text(e.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        e.tag?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp)) }
        QuietAction("✕", onClick = onRemove)
    }
}

@Composable
internal fun Confirm(title: String, text: String?, yes: String, onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = text?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        confirmButton = { TextButton(onClick = onYes) { Text(yes, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onNo) { Text("Keep", style = MaterialTheme.typography.labelLarge) } },
        containerColor = LocalPaper.current.card,
        shape = MaterialTheme.shapes.large,
    )
}

/** Something small you'd like to do again and again. */
@Composable
fun AddGoalScreen(onBack: () -> Unit) {
    val store = LocalContext.current.app.checkin
    val data by store.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    var title by rememberSaveable { mutableStateOf("") }
    var monthly by rememberSaveable { mutableStateOf(false) }
    var target by rememberSaveable { mutableIntStateOf(1) }

    fun add(t: String, period: String, n: Int) {
        val err = store.addGoal(t, period, n)
        if (err != null) toaster.show(err) else {
            toaster.show("Goal added")
            onBack()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Polaroid(Face.THINKING, "hmm…", width = 110.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Add a goal", style = MaterialTheme.typography.headlineMedium)
                    Text("Something small you'd like to do again and again", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(18.dp))
            PaperCard(tilt = -0.4f, tape = true) {
                CardTitle("Your own")
                Spacer(Modifier.height(8.dp))
                NoteField("", { title = it }, "e.g. Call grandma", singleLine = true, onDone = { add(title, if (monthly) "month" else "week", target) })
                Spacer(Modifier.height(12.dp))
                Segmented(listOf(false to "Weekly", true to "Monthly"), monthly, { monthly = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Times per ${if (monthly) "month" else "week"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Stepper(target, { target = it }, 1..7, suffix = "×")
                }
                Spacer(Modifier.height(14.dp))
                BigButton("Add goal", onClick = { add(title, if (monthly) "month" else "week", target) })
            }
            val existing = data.goals.map { it.title.lowercase() }.toSet()
            val ideas = Checkin.GOAL_IDEAS.filter { it.first.lowercase() !in existing }
            if (ideas.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                PaperCard(tilt = 0.4f) {
                    CardTitle("Or pick an idea")
                    ideas.forEach { (t, period) ->
                        Row(Modifier.fillMaxWidth().clickable { add(t, period, 1) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(if (period == "week") "Weekly" else "Monthly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("  +", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
