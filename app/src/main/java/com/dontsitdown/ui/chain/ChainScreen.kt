package com.dontsitdown.ui.chain

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dontsitdown.DontSitDownApp
import com.dontsitdown.chain.ChainGenerator
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.chain.Phase
import com.dontsitdown.chain.SessionMode
import com.dontsitdown.chain.SessionState
import com.dontsitdown.chain.SummaryCalculator
import com.dontsitdown.data.model.ReminderEventType
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskSize
import com.dontsitdown.service.Reminders
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DontSitDownApp
    val session = app.session.state
    val settings = app.settings.state
    val tasks = app.repository.observeTasks()
    val results = app.repository.observeChainResults()
    private val shown = mutableSetOf<String>()

    /** Set right after "sit down": the main screen rests instead of serving another chain. */
    var restingAfter by mutableStateOf<Float?>(null)
        private set

    fun ensurePlan() {
        if (session.value == null && restingAfter == null) generate(shuffle = false)
    }

    fun shuffle() = generate(shuffle = true)

    private fun generate(shuffle: Boolean) = viewModelScope.launch {
        val current = session.value
        if (current != null && current.phase != Phase.READY) return@launch
        current?.let { s -> shown += ChainGenerator.comboKey(s.plan.map { it.task }) }
        var chain = app.generateChain(exclude = shown, shuffle = shuffle)
        if (shuffle && chain.isNotEmpty() && ChainGenerator.comboKey(chain) in shown) {
            // Every combo's been seen; start the rotation over rather than serving nothing.
            shown.clear()
            chain = app.generateChain(shuffle = true)
        }
        app.session.plan(chain)
    }

    fun begin() = app.session.begin()
    fun setDread(level: Int) = app.session.setDread(level)
    fun done() = app.session.done()
    fun next() = app.session.next()
    fun finishEarly() = app.session.finishEarly()
    fun abandon() {
        app.session.abandon()
        generate(shuffle = false)
    }

    fun sitDown() {
        restingAfter = session.value?.totalActual
        app.session.clear()
    }

    fun wakeUp() {
        restingAfter = null
        generate(shuffle = false)
    }

    /** A quick task not already in this session, for "one more". */
    suspend fun oneMoreCandidate(s: SessionState): Task? {
        val used = s.plan.map { it.task.id }.toSet()
        val now = System.currentTimeMillis()
        return app.repository.activeTasks()
            .filter { it.id !in used && it.size == TaskSize.QUICK }
            .sortedBy { (TimeUtils.dueInDays(it, now) ?: 99) * 10 + it.effectiveMinutes }
            .firstOrNull()
    }

    fun oneMore(task: Task) = app.session.oneMore(task)

    fun planSingle(task: Task) {
        app.session.plan(listOf(task), SessionMode.SINGLE)
        app.session.begin()
    }

    suspend fun refusalLogged(): String? {
        app.repository.logEvent(ReminderEventType.REFUSED)
        if (!app.settings.current.spite) return null
        val score = SummaryCalculator.yearScore(app.repository)
        return MessageGenerator.skipped(true, score.couch, score.you)
    }

    fun negotiationOffers(all: List<Task>) = Reminders.negotiationOffers(all)
    suspend fun allTasks() = tasks.first()
}

@Composable
fun ChainScreen(onJustOne: () -> Unit, vm: ChainViewModel = viewModel()) {
    val state by vm.session.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(state == null, vm.restingAfter) { vm.ensurePlan() }

    val key = state?.let { it.phase to it.index } ?: (Phase.READY to -1)
    AnimatedContent(
        targetState = key,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "phase",
    ) { (phase, _) ->
        val s = state
        when {
            vm.restingAfter != null && s == null -> Resting(vm.restingAfter!!, onAnother = vm::wakeUp)
            s == null || phase == Phase.READY -> ReadyContent(s, settings.negotiator || settings.ghost, vm, onJustOne)
            phase == Phase.DREAD -> DreadScreen(onPick = vm::setDread)
            phase == Phase.ACTIVE -> TimerScreen(s, onDone = vm::done, onBail = vm::abandon)
            phase == Phase.BETWEEN -> BetweenScreen(s, onNext = vm::next, onEnough = vm::finishEarly)
            else -> DoneScreen(s, vm)
        }
    }
}

@Composable
private fun ReadyContent(s: SessionState?, canRefuse: Boolean, vm: ChainViewModel, onJustOne: () -> Unit) {
    var refusing by rememberSaveable { mutableStateOf(false) }
    val settings by vm.settings.collectAsStateWithLifecycle()

    if (refusing && s != null) {
        var offers by remember { mutableStateOf<List<Task>?>(null) }
        LaunchedEffect(Unit) { offers = vm.negotiationOffers(vm.allTasks()) }
        offers?.let {
            RefusalFlow(
                ghostTask = s.plan.firstOrNull()?.task?.name,
                offers = it,
                ghost = settings.ghost,
                negotiator = settings.negotiator,
                onAcceptChain = { refusing = false; vm.begin() },
                onAcceptTask = { t -> refusing = false; vm.planSingle(t) },
                onFinalRefusal = { vm.refusalLogged() },
                onFinished = { refusing = false; vm.sitDown() },
            )
        }
        return
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(MessageGenerator.HEADLINE, style = MaterialTheme.typography.displayLarge)
        if (s == null) return@Column

        if (s.plan.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(MessageGenerator.emptyChain(settings.spite), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            QuietAction("shuffle anyway", onClick = vm::shuffle)
            QuietAction("just one", onClick = onJustOne)
            return@Column
        }

        Spacer(Modifier.height(10.dp))
        Text(s.quip, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))

        s.plan.forEach { p ->
            val due = TimeUtils.dueInDays(p.task)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.task.name, style = MaterialTheme.typography.titleLarge)
                    if (due != null && due < 0) {
                        Text(TimeUtils.dueLabel(due), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("${TimeUtils.wholeMinutes(p.estimate)}m", style = Nums.medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Rule()
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.Bottom) {
            Text("${TimeUtils.wholeMinutes(s.plannedMinutes)} minutes.", style = Nums.large, modifier = Modifier.weight(1f))
            Text("that's it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(48.dp))
        BigButton("start", onClick = vm::begin)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            QuietAction("shuffle", onClick = vm::shuffle)
            QuietAction("just one", onClick = onJustOne)
            if (canRefuse) QuietAction("not now", onClick = { refusing = true })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Resting(minutes: Float, onAnother: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text("now sit down.", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "${TimeUtils.minutesLabel(minutes)} well spent. the couch is all yours.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))
        QuietAction("actually, another chain", onClick = onAnother)
    }
}

@Composable
internal fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Start, modifier = modifier)
}
