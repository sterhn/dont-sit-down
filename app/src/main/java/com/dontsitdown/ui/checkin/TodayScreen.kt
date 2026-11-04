package com.dontsitdown.ui.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.app
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.CheckinLines
import com.dontsitdown.checkin.Evening
import com.dontsitdown.checkin.Face
import com.dontsitdown.checkin.Morning
import com.dontsitdown.data.model.TaskCompletion
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.CardTitle
import com.dontsitdown.ui.components.Chip
import com.dontsitdown.ui.components.DoodleIcon
import com.dontsitdown.ui.components.LocalToaster
import com.dontsitdown.ui.components.NoteField
import com.dontsitdown.ui.components.PaperCard
import com.dontsitdown.ui.components.Polaroid
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.Scale
import com.dontsitdown.ui.components.Segmented
import com.dontsitdown.ui.components.Tile
import com.dontsitdown.ui.components.WeekDots
import com.dontsitdown.ui.components.paperShadow
import com.dontsitdown.ui.theme.HandText
import com.dontsitdown.ui.theme.LocalPaper
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/** A short-lived face after something happens: a slip, a milestone, a blank check-in. */
private data class Reaction(val face: Face, val caption: String?, val ms: Long, val id: Int)

/** The photo develops once per app start, not every time you come back to the tab. */
private var developedThisRun = false

/**
 * Today: the check-in (morning and evening on one page, with the character watching),
 * and a strip pointing at tonight's chain, so the two halves of the app meet in one place.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(onOpenChain: () -> Unit, onOpenGoal: (String) -> Unit, onOpenGoals: () -> Unit, now: () -> LocalTime = { LocalTime.now() }) {
    val context = LocalContext.current
    val app = context.app
    val store = app.checkin
    val data by store.state.collectAsStateWithLifecycle()
    val settings by app.settings.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    val today = com.dontsitdown.ui.components.rememberToday()
    val hour = remember(today) { now().hour }
    // Before 5am it's still last night: open on the evening, pointed at yesterday.
    var evening by rememberSaveable(today) { mutableStateOf(hour < 5 || hour >= 17) }
    var eveningOffset by rememberSaveable(today) { mutableIntStateOf(if (hour < 5) -1 else 0) }
    val eveningDate = today.plusDays(eveningOffset.toLong())
    val away = settings.isAway(today)

    var reaction by remember { mutableStateOf<Reaction?>(null) }
    var serial by remember { mutableIntStateOf(0) }
    fun react(face: Face, ms: Long = 1800, caption: String? = null) {
        reaction = Reaction(face, caption, ms, ++serial)
    }
    LaunchedEffect(reaction?.id) {
        val r = reaction ?: return@LaunchedEffect
        delay(r.ms)
        if (reaction?.id == r.id) reaction = null
    }

    val filledToday = Checkin.dayFilled(data, today)

    val morning = data.day(Checkin.key(today)).morning
    val eveningRec = data.day(Checkin.key(eveningDate)).evening
    val small = hour < 7
    val face = reaction?.face ?: if (evening) {
        Checkin.faceFor(eveningRec?.mood, if (away) Face.AWAY else Face.COZY, small)
    } else {
        Checkin.faceFor(morning?.mood, if (away) Face.AWAY else Face.COFFEE, small || (morning?.sleep ?: 10) < 4)
    }
    val mood = if (evening) eveningRec?.mood else morning?.mood
    val caption = reaction?.let { it.caption ?: Checkin.FACE_CAPTIONS[it.face] ?: "" }
        ?: mood?.let { Checkin.moodWord(it) } ?: Checkin.FACE_CAPTIONS[face] ?: ""

    // Chores on the evening's day, from the other half of the app.
    val dayStart = TimeUtils.startOfDay(eveningDate)
    val chores by remember(dayStart) { app.repository.observeCompletionsBetween(dayStart, dayStart + TimeUtils.DAY) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val todayStart = TimeUtils.startOfDay(today)
    val choresToday by remember(todayStart) { app.repository.observeCompletionsBetween(todayStart, todayStart + TimeUtils.DAY) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val scroll = rememberScrollState()
    var headerBottom by remember { mutableIntStateOf(0) }
    val develop = remember { !developedThisRun }.also { developedThisRun = true }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .imePadding()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            // ---- header: the polaroid, the greeting, this week ----
            Row(Modifier.fillMaxWidth().onGloballyPositioned { headerBottom = it.size.height }, verticalAlignment = Alignment.Top) {
                Polaroid(face, caption, develop = develop, onPoke = {
                    val (f, c) = Checkin.POKES.random()
                    react(f, 1500, c)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                })
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).padding(top = 6.dp)) {
                    val date = if (evening) eveningDate else today
                    val saved = if (evening) eveningRec?.savedAt != null else morning?.savedAt != null
                    Text(
                        Checkin.pretty(date) + if (saved) " · saved" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            away && !evening -> "day off."
                            evening && eveningOffset == -1 -> "last night."
                            evening -> "good evening."
                            else -> "good morning."
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    val start = Checkin.weekStart(today)
                    WeekDots(
                        done = (0..6).map { Checkin.dayFilled(data, start.plusDays(it.toLong())) },
                        todayIndex = today.dayOfWeek.value - 1,
                        letters = (0..6).map { Checkin.narrow(start.plusDays(it.toLong())) },
                    )
                    Spacer(Modifier.height(5.dp))
                    // A record, not a scoreboard: no counting, no streaks.
                    Text(
                        if (filledToday) "today's written down" else "whenever you're ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Segmented(
                listOf(
                    false to "morning" + if (Checkin.morningFilled(morning)) " ✓" else "",
                    true to "evening" + if (Checkin.eveningFilled(data.day(Checkin.key(today)).evening)) " ✓" else "",
                ),
                evening, { evening = it }, Modifier.fillMaxWidth(),
            )

            if (!evening) {
                MorningPage(data, today, morning, onReact = ::react, onDone = {
                    if (!Checkin.morningFilled(data.day(Checkin.key(today)).morning)) {
                        react(Face.ANNOYED); toaster.show(CheckinLines.pick(CheckinLines.EMPTY))
                    } else {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        react(Face.NICE, 1700, "saved!")
                        toaster.show(CheckinLines.morningDone(now().hour))
                    }
                })
            } else {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-1 to "yesterday", 0 to "today").forEach { (off, label) ->
                        val done = Checkin.eveningFilled(data.day(Checkin.key(today.plusDays(off.toLong()))).evening)
                        Chip(label + if (done) " ✓" else "", eveningOffset == off, { eveningOffset = off })
                    }
                }
                EveningPage(
                    data = data, date = eveningDate, ev = eveningRec, chores = chores, away = settings.isAway(eveningDate),
                    yesterday = eveningOffset == -1,
                    onReact = ::react,
                    onDone = {
                        if (!Checkin.eveningFilled(store.current.day(Checkin.key(eveningDate)).evening)) {
                            react(Face.ANNOYED); toaster.show(CheckinLines.pick(CheckinLines.EMPTY))
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            react(Face.NICE, 1700, "saved!")
                            toaster.show(CheckinLines.eveningDone(now().hour, eveningOffset != 0))
                        }
                    },
                    onCopy = {
                        clipboard.setText(AnnotatedString(Checkin.dayMarkdown(store.current, eveningDate, chores.map { it.taskName to it.minutes })))
                        toaster.show("copied")
                    },
                )
            }

            Spacer(Modifier.height(18.dp))
            ChainStrip(away = away, choresToday = choresToday, onClick = onOpenChain)
            Spacer(Modifier.height(10.dp))
            GoalStrip(data, today, onOpenGoal, onOpenGoals)
            Spacer(Modifier.height(32.dp))
        }

        // When he's scrolled out of view, a little copy peeks in so a reaction is never missed.
        AnimatedVisibility(
            visible = reaction != null && scroll.value > headerBottom * 0.8f,
            enter = slideInVertically { it * 2 } + fadeIn(),
            exit = slideOutVertically { it * 2 } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 24.dp),
        ) {
            Polaroid(face, caption, width = 96.dp, tilt = 6f, tape = false)
        }
    }
}

@Composable
private fun MorningPage(data: CheckinData, today: LocalDate, m: Morning?, onReact: (Face, Long, String?) -> Unit, onDone: () -> Unit) {
    val store = LocalContext.current.app.checkin
    val paper = LocalPaper.current
    Spacer(Modifier.height(14.dp))
    PaperCard(tilt = -0.5f) {
        Scale("sleep", m?.sleep, paper.sleep, { v -> store.setMorning(today) { it.copy(sleep = v) } }, ends = "restless" to "deep")
        Divider()
        Scale("body", m?.body, paper.body, { v -> store.setMorning(today) { it.copy(body = v) } }, ends = "aching" to "strong")
        Divider()
        Scale("mood", m?.mood, paper.mood, { v -> store.setMorning(today) { it.copy(mood = v) } }, moodWord = true)
    }
    Spacer(Modifier.height(16.dp))
    PaperCard(tilt = 0.45f, tape = true) {
        CardTitle("intention for today")
        Spacer(Modifier.height(10.dp))
        NoteField(m?.intention ?: "", { t -> store.setMorning(today) { it.copy(intention = t) } }, "what's the day for?", key = Checkin.key(today))
    }
    Spacer(Modifier.height(20.dp))
    BigButton("done →", onClick = onDone)
}

@Composable
private fun Divider() {
    Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(LocalPaper.current.cardLine.copy(alpha = 0.5f)))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EveningPage(
    data: CheckinData,
    date: LocalDate,
    ev: Evening?,
    chores: List<TaskCompletion>,
    away: Boolean,
    yesterday: Boolean,
    onReact: (Face, Long, String?) -> Unit,
    onDone: () -> Unit,
    onCopy: () -> Unit,
) {
    val store = LocalContext.current.app.checkin
    val paper = LocalPaper.current
    val key = Checkin.key(date)
    val e = ev ?: Evening()

    Spacer(Modifier.height(14.dp))
    PaperCard(tilt = -0.5f) {
        CardTitle("how are you now?")
        Spacer(Modifier.height(12.dp))
        Scale("body", e.body, paper.body, { v -> store.setEvening(date) { it.copy(body = v) } }, ends = "aching" to "strong")
        Divider()
        Scale("mood", e.mood, paper.mood, { v -> store.setEvening(date) { it.copy(mood = v) } }, moodWord = true)
    }

    // The morning, read back: what you said the day was for.
    val m = data.day(key).morning
    if (Checkin.morningFilled(m)) {
        Spacer(Modifier.height(16.dp))
        PaperCard(tilt = 0.45f) {
            CardTitle(if (yesterday) "that morning" else "this morning")
            if (!m!!.intention.isBlank()) {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.padding(top = 4.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .rotate(-0.8f)
                            .background(paper.note, RoundedCornerShape(6.dp))
                            .border(1.dp, paper.cardLine, RoundedCornerShape(6.dp))
                            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
                    ) {
                        Text("you said ${if (yesterday) "yesterday" else "today"} was for".uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(m.intention.trim(), style = HandText.medium)
                    }
                    com.dontsitdown.ui.components.Tape(Modifier.align(Alignment.TopStart).padding(start = 16.dp).offset(y = (-10).dp), width = 46.dp, tilt = -6f)
                }
            }
            if (m.sleep != null || m.mood != null || m.body != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile(m.sleep?.toString() ?: "–", "sleep", Modifier.weight(1f), tint = paper.sleep.copy(alpha = 0.22f))
                    Tile(m.body?.toString() ?: "–", "body", Modifier.weight(1f), tint = paper.body.copy(alpha = 0.22f))
                    Tile(m.mood?.toString() ?: "–", "mood" + (m.mood?.let { " · ${Checkin.moodWord(it)}" } ?: ""), Modifier.weight(1.3f), tint = paper.mood.copy(alpha = 0.22f))
                }
            }
        }
    }

    // What the chores side saw today.
    Spacer(Modifier.height(16.dp))
    PaperCard(tilt = -0.35f) {
        CardTitle(if (yesterday) "moved that day" else "moved today", trailing = {
            if (chores.isNotEmpty()) Text("${chores.sumOf { it.minutes.toDouble() }.roundToInt().coerceAtLeast(1)} min", style = Nums.small, color = MaterialTheme.colorScheme.primary)
        })
        Spacer(Modifier.height(6.dp))
        val total = chores.sumOf { it.minutes.toDouble() }.roundToInt().coerceAtLeast(if (chores.isEmpty()) 0 else 1)
        Text(CheckinLines.choresLine(chores.size, total, away), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (chores.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            chores.groupBy { it.taskName }.forEach { (name, list) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text("✓  $name", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${TimeUtils.wholeMinutes(list.sumOf { it.minutes.toDouble() }.toFloat())}m", style = Nums.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Boundaries: held / slipped, tap again to clear.
    Spacer(Modifier.height(16.dp))
    var editing by rememberSaveable { mutableStateOf(false) }
    PaperCard(tilt = 0.4f) {
        CardTitle("boundaries", trailing = { QuietAction(if (editing) "done" else "edit", onClick = { editing = !editing }) })
        val names = (data.boundaries + e.boundaries.filterValues { it != null }.keys).distinct()
        names.forEach { b ->
            val st = e.boundaries[b]
            val tracked = b in data.boundaries
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(b.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).then(if (tracked) Modifier else Modifier.padding(end = 4.dp)), color = if (tracked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                Chip("held", st == "held", {
                    val next = if (st == "held") null else "held"
                    store.setEvening(date) { it.copy(boundaries = it.boundaries + (b to next)) }
                    if (next != null) onReact(Face.NICE, 1800, null)
                }, onColor = paper.held)
                Spacer(Modifier.width(6.dp))
                Chip("slipped", st == "slipped", {
                    val next = if (st == "slipped") null else "slipped"
                    store.setEvening(date) { it.copy(boundaries = it.boundaries + (b to next)) }
                    if (next != null) onReact(Face.SIGH, 1800, null)
                }, onColor = paper.slipped)
                if (editing && tracked) {
                    QuietAction("✕", onClick = { store.removeBoundary(b, date) })
                }
            }
        }
        if (editing) {
            var draft by remember { mutableStateOf("") }
            var fieldKey by remember { mutableIntStateOf(0) }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    NoteField("", { draft = it }, "new boundary…", singleLine = true, key = fieldKey, onDone = {
                        if (store.addBoundary(draft)) { draft = ""; fieldKey++ }
                    })
                }
                QuietAction("add", onClick = { if (store.addBoundary(draft)) { draft = ""; fieldKey++ } }, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    PaperCard(tilt = -0.45f) {
        CardTitle("gym")
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Triple("went", "went", paper.held), Triple("rest", "rest day", paper.rest), Triple("skipped", "skipped", paper.slipped)).forEach { (v, label, c) ->
                Chip(label, e.gym == v, {
                    val next = if (e.gym == v) null else v
                    store.setEvening(date) { it.copy(gym = next) }
                    if (next != null) onReact(mapOf("went" to Face.NICE, "rest" to Face.COZY, "skipped" to Face.SIGH).getValue(next), 1800, null)
                }, onColor = c)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    PaperCard(tilt = 0.35f) {
        CardTitle("spent ${if (yesterday) "that day" else "today"} (rough)")
        Spacer(Modifier.height(10.dp))
        NoteField(e.spent, { t -> store.setEvening(date) { it.copy(spent = t) } }, "ballpark is fine", singleLine = true, key = "$key-spent")
    }

    Spacer(Modifier.height(16.dp))
    PaperCard(tilt = -0.4f, tape = true) {
        CardTitle("what happened")
        Spacer(Modifier.height(10.dp))
        NoteField(e.happened, { t -> store.setEvening(date) { it.copy(happened = t) } }, "the shape of the day", key = "$key-happened")
        Spacer(Modifier.height(16.dp))
        CardTitle("one honest thing")
        Spacer(Modifier.height(10.dp))
        NoteField(e.honest, { t -> store.setEvening(date) { it.copy(honest = t) } }, "say it plainly", key = "$key-honest")
    }

    Spacer(Modifier.height(20.dp))
    BigButton("done →", onClick = onDone)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        QuietAction("copy daily note as markdown", onClick = onCopy)
    }
}

/** Where the two apps meet: tonight's chain, from the check-in page. */
@Composable
private fun ChainStrip(away: Boolean, choresToday: List<TaskCompletion>, onClick: () -> Unit) {
    val app = LocalContext.current.app
    val minutes by produceState<Float?>(null, away, choresToday.size) {
        value = if (away || choresToday.isNotEmpty()) null else app.generateChain().sumOf { it.effectiveMinutes.toDouble() }.toFloat()
    }
    val done = choresToday.isNotEmpty()
    val (kicker, title, go) = when {
        done -> Triple("chores · done today", "${choresToday.size} done, ${TimeUtils.wholeMinutes(choresToday.sumOf { it.minutes.toDouble() }.toFloat())} min", "✓")
        away -> Triple("chores · day off", "not home today", "›")
        minutes != null && minutes!! > 0f -> Triple("chores · tonight", "don't sit down. ${TimeUtils.wholeMinutes(minutes!!)} min.", "go ›")
        else -> Triple("chores", "nothing due. suspicious.", "›")
    }
    Strip(kicker, title, go, onClick, leading = {
        Box(Modifier.size(34.dp).background(if (done) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(4.dp)).border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            Text(if (away && !done) "—" else "▲", style = MaterialTheme.typography.labelMedium, color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
        }
    }, highlight = !done && !away)
}

@Composable
private fun GoalStrip(data: CheckinData, today: LocalDate, onOpenGoal: (String) -> Unit, onOpenGoals: () -> Unit) {
    if (data.goals.isEmpty()) return
    val pending = Checkin.pendingGoals(data, today)
    if (pending.isEmpty()) {
        Strip("goals · all done", "every goal is ticked off", "›", onOpenGoals, leading = { DoodleIcon(com.dontsitdown.checkin.Doodle.STAR, size = 34.dp) })
        return
    }
    val (g, n) = pending.first()
    val more = pending.size - 1
    Strip(
        "up next · ${Checkin.periodLeft(g, today)}" + if (more > 0) " · $more more" else "",
        g.title.replaceFirstChar { it.uppercase() },
        "$n/${g.target} ›",
        { onOpenGoal(g.id) },
        leading = { DoodleIcon(Checkin.doodleFor(data, g), size = 34.dp) },
    )
}

@Composable
internal fun Strip(kicker: String, title: String, go: String, onClick: () -> Unit, leading: @Composable () -> Unit, highlight: Boolean = false) {
    val paper = LocalPaper.current
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .paperShadow(color = paper.shadow)
            .background(paper.card, shape)
            .border(1.dp, if (highlight) MaterialTheme.colorScheme.primary else paper.cardLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(kicker.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, style = HandText.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(go, style = MaterialTheme.typography.labelMedium, color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
