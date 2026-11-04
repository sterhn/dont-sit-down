package com.dontsitdown.ui.checkin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.app
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.DayNumbers
import com.dontsitdown.checkin.Face
import com.dontsitdown.data.model.TaskCompletion
import com.dontsitdown.ui.components.CardTitle
import com.dontsitdown.ui.components.Chip
import com.dontsitdown.ui.components.LocalToaster
import com.dontsitdown.ui.components.OutlineButton
import com.dontsitdown.ui.components.PaperCard
import com.dontsitdown.ui.components.Polaroid
import com.dontsitdown.ui.components.Segmented
import com.dontsitdown.ui.components.ThinBar
import com.dontsitdown.ui.components.Tile
import com.dontsitdown.ui.components.TopBar
import com.dontsitdown.ui.components.paperShadow
import com.dontsitdown.ui.summary.WeeklySummaryScreen
import com.dontsitdown.ui.theme.HandText
import com.dontsitdown.ui.theme.LocalPaper
import com.dontsitdown.ui.theme.Mono
import com.dontsitdown.util.TimeUtils
import java.time.LocalDate
import kotlin.math.roundToInt

/** Progress: how the check-in has been going. (The chores' week lives on the chain side.) */
@Composable
fun StatsScreen(onWeekStory: () -> Unit, onJournal: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("How it's going", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Progress", style = MaterialTheme.typography.displayMedium)
        }
        CheckinStats(onWeekStory, onJournal)
    }
}

@Composable
private fun CheckinStats(onWeekStory: () -> Unit, onJournal: () -> Unit) {
    val context = LocalContext.current
    val data by context.app.checkin.state.collectAsStateWithLifecycle()
    val paper = LocalPaper.current
    val today = com.dontsitdown.ui.components.rememberToday()
    var range by rememberSaveable { mutableIntStateOf(7) }
    val days = Checkin.window(data, today, range)
    val prev = Checkin.window(data, today, range, range)
    val journalCount = Checkin.journalDates(data).size

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        // How you've been, not how consistent you've been.
        val recent = Checkin.window(data, today, 7)
        val lately = Checkin.avg(recent.mapNotNull { it.mood })?.roundToInt()
        PaperCard(tilt = -0.4f, color = paper.noteBlue) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Polaroid(
                    if (lately != null) Checkin.faceFor(lately) else Face.THINKING,
                    if (lately != null) Checkin.moodWord(lately) else "hmm",
                    width = 104.dp,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("LATELY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (lately != null) "Mostly ${Checkin.moodWord(lately)}." else "Not much to go on yet.", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (lately != null) "Your mood over the last 7 days" else "Your mood shows up here after a few check-ins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReadLink("This week's story", "Read it like a diary", Modifier.weight(1f), 0.6f, onWeekStory)
            ReadLink("Journal", if (journalCount > 0) "$journalCount ${if (journalCount == 1) "day" else "days"} written" else "Past entries", Modifier.weight(1f), -0.5f, onJournal)
        }

        Spacer(Modifier.height(14.dp))
        PaperCard(tilt = 0.4f) {
            val last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }
            CardTitle("Last 7 days", trailing = { Text("${last7.count { Checkin.dayFilled(data, it) }} of 7", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) })
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                last7.forEach { d ->
                    val done = Checkin.dayFilled(data, d)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Checkin.narrow(d), style = MaterialTheme.typography.labelSmall, color = if (d == today) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.size(30.dp).then(
                                if (done) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier.border(if (d == today) 2.dp else 1.dp, if (d == today) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline, CircleShape)
                            ),
                            contentAlignment = Alignment.Center,
                        ) { if (done) Text("✓", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary) }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(7, 14, 30).forEach { n -> Chip("$n days", range == n, { range = n }) }
        }

        // Boundary hold rates over the window.
        var heldAll = 0
        var totalAll = 0
        days.forEach { d -> d.boundaries?.values?.forEach { if (it == "held") { heldAll++; totalAll++ } else if (it == "slipped") totalAll++ } }
        fun holdRate(name: String): Int? {
            var held = 0
            var total = 0
            days.forEach { d -> when (d.boundaries?.get(name)) { "held" -> { held++; total++ }; "slipped" -> total++ } }
            return if (total > 0) (100.0 * held / total).roundToInt() else null
        }

        Spacer(Modifier.height(12.dp))
        val first = days.first().date
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("${days.count { Checkin.dayFilled(data, it.date) }}/$range", "days checked in", Modifier.weight(1f))
            Tile("${days.count { d -> Checkin.storyLines(data, d.date, false).isNotEmpty() }}", "days written", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("${data.log.count { it.date >= first.toString() }}", "adventures logged", Modifier.weight(1f))
            Tile(if (totalAll > 0) "${(100.0 * heldAll / totalAll).roundToInt()}%" else "–", "boundaries held", Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        PaperCard(tilt = -0.35f) {
            CardTitle("Averages", trailing = { Text("vs the $range days before", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) })
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvgTile("Sleep", days, prev, { it.sleep }, paper.sleep, Modifier.weight(1f))
                AvgTile("Body", days, prev, { it.body }, paper.body, Modifier.weight(1f))
                AvgTile("Mood", days, prev, { it.mood }, paper.mood, Modifier.weight(1f))
            }
            val hasData = days.any { it.sleep != null || it.mood != null || it.body != null }
            Spacer(Modifier.height(14.dp))
            if (hasData) {
                Chart(days)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                    Legend("Sleep", paper.sleep); Legend("Body", paper.body); Legend("Mood", paper.mood)
                }
            } else {
                Text("Your chart appears after a few check-ins.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(14.dp))
        PaperCard(tilt = 0.4f) {
            CardTitle("Boundaries")
            Spacer(Modifier.height(4.dp))
            data.boundaries.forEach { b -> BarRow(b, holdRate(b)?.let { it / 100f }, holdRate(b)?.let { "$it%" } ?: "–", paper.body) }
            // ones you've stopped tracking but that still show up in this window
            days.flatMap { it.boundaries?.keys.orEmpty() }.distinct().filter { it !in data.boundaries }.forEach { b ->
                holdRate(b)?.let { BarRow(b, it / 100f, "$it%", paper.body, faded = true) }
            }
        }

        Spacer(Modifier.height(14.dp))
        PaperCard(tilt = -0.4f) {
            CardTitle("Mood after sleep")
            Spacer(Modifier.height(4.dp))
            val paired = days.filter { it.sleep != null && it.mood != null }
            val split = if (paired.size >= 2) {
                val mean = paired.map { it.sleep!! }.average()
                val hi = paired.filter { it.sleep!! >= mean }.map { it.mood!! }
                val lo = paired.filter { it.sleep!! < mean }.map { it.mood!! }
                if (hi.isNotEmpty() && lo.isNotEmpty()) hi.average() to lo.average() else null
            } else null
            if (split != null) {
                BarRow("After better sleep", (split.first / 10).toFloat(), Checkin.fmt1(split.first), paper.mood)
                BarRow("After worse sleep", (split.second / 10).toFloat(), Checkin.fmt1(split.second), paper.mood.copy(alpha = 0.45f))
            } else {
                Text("Needs a few days with both sleep and mood logged.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ReadLink(title: String, sub: String, modifier: Modifier, tilt: Float, onClick: () -> Unit) {
    val paper = LocalPaper.current
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier
            .rotate(tilt)
            .paperShadow(color = paper.shadow)
            .background(paper.card, shape)
            .border(1.dp, paper.cardLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, style = HandText.small)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
private fun AvgTile(label: String, days: List<DayNumbers>, prev: List<DayNumbers>, f: (DayNumbers) -> Int?, tint: Color, modifier: Modifier) {
    val a = Checkin.avg(days.mapNotNull(f))
    val p = Checkin.avg(prev.mapNotNull(f))
    Tile(Checkin.fmt1(a), label, modifier, tint = tint.copy(alpha = 0.2f), extra = {
        if (a != null && p != null) {
            val d = ((a - p) * 10).roundToInt() / 10.0
            if (d != 0.0) Text(
                (if (d > 0) " ↑" else " ↓") + String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(d)),
                style = MaterialTheme.typography.labelSmall,
                color = if (d > 0) LocalPaper.current.good else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    })
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(" $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BarRow(name: String, fraction: Float?, shown: String, color: Color, faded: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = if (faded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground)
        ThinBar(fraction ?: 0f, Modifier.weight(1f), color = color, height = 10.dp)
        Text(shown, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
    }
}

/** Sleep, body and mood over the window, drawn in like ink. */
@Composable
private fun Chart(days: List<DayNumbers>) {
    val paper = LocalPaper.current
    val grid = paper.cardLine
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val reveal = remember(days.size) { Animatable(0f) }
    LaunchedEffect(days.size) { reveal.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    val series = listOf<Pair<(DayNumbers) -> Int?, Color>>({ d: DayNumbers -> d.sleep } to paper.sleep, { d: DayNumbers -> d.mood } to paper.mood, { d: DayNumbers -> d.body } to paper.body)
    Canvas(Modifier.fillMaxWidth().height(170.dp).clipToBounds()) {
        val padL = 26.dp.toPx()
        val padR = 8.dp.toPx()
        val padT = 10.dp.toPx()
        val padB = 22.dp.toPx()
        val w = size.width
        val h = size.height
        fun x(i: Int) = padL + if (days.size == 1) 0f else i * (w - padL - padR) / (days.size - 1)
        fun y(v: Int) = padT + (10 - v) * (h - padT - padB) / 9f
        val label = TextStyle(fontFamily = Mono, fontSize = 10.sp, color = muted)
        for (g in listOf(1, 4, 7, 10)) {
            drawLine(grid, Offset(padL, y(g)), Offset(w - padR, y(g)), 1.dp.toPx())
            val t = measurer.measure("$g", label)
            drawText(t, topLeft = Offset(padL - 8.dp.toPx() - t.size.width, y(g) - t.size.height / 2f))
        }
        val every = maxOf(1, (days.size + 6) / 7)
        days.forEachIndexed { i, d ->
            if (i % every == 0 || i == days.size - 1) {
                val t = measurer.measure("${d.date.dayOfMonth}.${d.date.monthValue}", label)
                drawText(t, topLeft = Offset(x(i) - t.size.width / 2f, h - t.size.height))
            }
        }
        clipRect(right = padL + (w - padL) * reveal.value + 8.dp.toPx()) {
            for ((f, color) in series) {
                val path = Path()
                var pen = false
                days.forEachIndexed { i, d ->
                    val v = f(d)
                    if (v == null) { pen = false; return@forEachIndexed }
                    if (pen) path.lineTo(x(i), y(v)) else path.moveTo(x(i), y(v))
                    pen = true
                }
                drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                days.forEachIndexed { i, d ->
                    f(d)?.let {
                        drawCircle(paper.card, 5.5.dp.toPx(), Offset(x(i), y(it)))
                        drawCircle(color, 4.dp.toPx(), Offset(x(i), y(it)))
                    }
                }
            }
        }
    }
}

// ---------- reading back ----------

/** How he looked that day: the day's mood as a face and a word. */
@Composable
private fun DayPolaroid(data: CheckinData, d: LocalDate, tilt: Float) {
    val mood = Checkin.numbers(data, d).mood
    Polaroid(if (mood != null) Checkin.faceFor(mood) else Face.THINKING, if (mood != null) Checkin.moodWord(mood) else "–", width = 68.dp, tilt = tilt, tape = false)
}

@Composable
private fun StoryDay(data: CheckinData, d: LocalDate, title: String, full: Boolean, chores: List<TaskCompletion>, index: Int) {
    val bits = Checkin.scoreBits(data, d)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        DayPolaroid(data, d, if (index % 2 == 0) -3f else 2.5f)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = HandText.small)
            if (bits.isNotEmpty()) {
                Text(
                    bits.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Checkin.storyLines(data, d, full).forEach { (label, text) -> StoryLine(label, text) }
            if (chores.isNotEmpty()) {
                StoryLine("chores", chores.groupBy { it.taskName }.map { (n, l) -> "$n ${TimeUtils.wholeMinutes(l.sumOf { it.minutes.toDouble() }.toFloat())}m" }.joinToString(", "))
            }
        }
    }
}

@Composable
private fun StoryLine(label: String, text: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Every intention, "what happened" and "one honest thing", newest first. */
@Composable
fun JournalScreen(onBack: () -> Unit) {
    val data by LocalContext.current.app.checkin.state.collectAsStateWithLifecycle()
    val today = remember { TimeUtils.today() }
    val dates = Checkin.journalDates(data)
    var limit by rememberSaveable { mutableIntStateOf(14) }
    Column(Modifier.fillMaxSize()) {
        TopBar("", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dates.isNotEmpty()) Polaroid(Face.FLUSTERED, "don't read this", width = 112.dp) else Polaroid(Face.AWAY, "nothing yet", width = 112.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Journal", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (dates.isNotEmpty()) "${dates.size} ${if (dates.size == 1) "day" else "days"} in your own words" else "Nothing written yet",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (dates.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                PaperCard { Text("Intentions, \"what happened\" and \"one honest thing\" show up here once you've written a few.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            val shown = dates.take(limit)
            shown.forEachIndexed { i, d ->
                if (i == 0 || shown[i - 1].withDayOfMonth(1) != d.withDayOfMonth(1)) {
                    Text(Checkin.monthYear(d), style = HandText.medium, modifier = Modifier.padding(top = 22.dp, start = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                PaperCard(tilt = if (i % 2 == 0) -0.4f else 0.4f) { StoryDay(data, d, Checkin.short(d, today), false, emptyList(), i) }
            }
            if (dates.size > shown.size) {
                Spacer(Modifier.height(14.dp))
                OutlineButton("Show more", onClick = { limit += 14 }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** The week stitched together day by day, like a short diary. */
@Composable
fun WeekStoryScreen(onBack: () -> Unit) {
    val app = LocalContext.current.app
    val data by app.checkin.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val clipboard = LocalClipboardManager.current
    val today = remember { TimeUtils.today() }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val start = Checkin.weekStart(today).plusWeeks(offset.toLong())
    val completions by produceState(emptyList<TaskCompletion>(), start) {
        value = app.repository.completionsBetween(TimeUtils.startOfDay(start), TimeUtils.startOfDay(start.plusDays(7)))
    }
    val byDay = completions.groupBy { TimeUtils.toDate(it.date) }
    val days = (0..6).map { start.plusDays(it.toLong()) }.filter { !it.isAfter(today) }
    val has = days.associateWith { Checkin.scoreBits(data, it).isNotEmpty() || Checkin.storyLines(data, it, true).isNotEmpty() || byDay[it].orEmpty().isNotEmpty() }
    val written = days.filter { has.getValue(it) }
    val moods = written.mapNotNull { Checkin.numbers(data, it).mood }
    val avgMood = Checkin.avg(moods)?.roundToInt()

    Column(Modifier.fillMaxSize()) {
        TopBar("", onBack = onBack) {
            androidx.compose.material3.IconButton(onClick = { offset-- }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
            androidx.compose.material3.IconButton(onClick = { offset++ }, enabled = offset < 0) { Text("›", style = MaterialTheme.typography.titleLarge, color = if (offset < 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Polaroid(
                    when { avgMood != null -> Checkin.faceFor(avgMood); written.isNotEmpty() -> Face.THINKING; else -> Face.AWAY },
                    when { avgMood != null -> Checkin.moodWord(avgMood); written.isNotEmpty() -> "hmm"; else -> "nothing yet" },
                    width = 112.dp,
                )
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(when (offset) { 0 -> "This week's story"; -1 -> "Last week's story"; else -> "Week in review" }, style = MaterialTheme.typography.headlineMedium)
                    Text("${Checkin.dayMonth(start)} – ${Checkin.dayMonth(start.plusDays(6))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            PaperCard(tilt = -0.3f, tape = true) {
                if (written.isEmpty()) {
                    Text("Nothing written this week${if (offset == 0) " yet" else ""}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else days.forEachIndexed { i, d ->
                    if (i > 0) Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(LocalPaper.current.cardLine.copy(alpha = 0.5f)))
                    if (has.getValue(d)) StoryDay(data, d, Checkin.pretty(d), true, byDay[d].orEmpty(), i)
                    else Row { Text(Checkin.pretty(d), style = HandText.small, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (written.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                OutlineButton("Copy week as markdown", onClick = {
                    clipboard.setText(AnnotatedString(Checkin.weekMarkdown(data, start, today, byDay.mapValues { (_, l) -> l.map { it.taskName to it.minutes } })))
                    toaster.show("Week copied")
                }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
