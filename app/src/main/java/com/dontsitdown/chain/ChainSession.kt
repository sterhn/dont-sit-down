package com.dontsitdown.chain

import android.content.Context
import com.dontsitdown.data.model.ChainResult
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskResult
import com.dontsitdown.data.repository.TaskRepository
import com.dontsitdown.service.TimerService
import com.dontsitdown.util.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class SessionMode { CHAIN, SINGLE }

/** Ready → (dread) → active ⇄ between → complete. */
enum class Phase { READY, DREAD, ACTIVE, BETWEEN, COMPLETE }

data class PlannedTask(val task: Task, val estimate: Float)

data class TaskRun(
    val taskId: String,
    val name: String,
    val estimate: Float,
    val startedAt: Long,
    val finishedAt: Long,
) {
    val actual: Float get() = (finishedAt - startedAt) / 60_000f
}

data class SessionState(
    val id: String = UUID.randomUUID().toString(),
    val mode: SessionMode,
    val plan: List<PlannedTask>,
    val phase: Phase = Phase.READY,
    val index: Int = 0,
    val currentStartedAt: Long = 0L,
    val runs: List<TaskRun> = emptyList(),
    val dread: Int = 0,
    val quip: String = "",
) {
    val current: PlannedTask? get() = plan.getOrNull(index)
    val next: PlannedTask? get() = plan.getOrNull(index + 1)
    val plannedMinutes: Float get() = plan.sumOf { it.estimate.toDouble() }.toFloat()
    val totalActual: Float get() = runs.sumOf { it.actual.toDouble() }.toFloat()
    val totalEstimate: Float get() = runs.sumOf { it.estimate.toDouble() }.toFloat()
    val isRunning: Boolean get() = phase == Phase.ACTIVE || phase == Phase.BETWEEN

    /** Longest streak of tasks started within [COMBO_GAP_MS] of the previous one finishing. */
    val combo: Int
        get() {
            if (runs.isEmpty()) return 0
            var best = 1
            var cur = 1
            for (i in 1 until runs.size) {
                cur = if (runs[i].startedAt - runs[i - 1].finishedAt <= COMBO_GAP_MS) cur + 1 else 1
                best = maxOf(best, cur)
            }
            return best
        }

    companion object {
        const val COMBO_GAP_MS = 2 * 60_000L
    }
}

/**
 * The one in-progress chain (or single task). Lives in the application so the timer
 * notification, the takeover screen and the UI all drive the same state.
 */
class ChainSession(
    private val context: Context,
    private val repo: TaskRepository,
    private val settings: Settings,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = _state.asStateFlow()

    private val spite get() = settings.current.spite
    private fun now() = System.currentTimeMillis()

    fun plan(tasks: List<Task>, mode: SessionMode = SessionMode.CHAIN) {
        val planned = tasks.map { PlannedTask(it, it.effectiveMinutes) }
        _state.value = SessionState(
            mode = mode,
            plan = planned,
            quip = MessageGenerator.readySubline(spite, planned.sumOf { it.estimate.toDouble() }.toFloat()),
        )
    }

    /** START pressed: the dread question first if it's on, otherwise straight into the timer. */
    fun begin() {
        val s = _state.value ?: return
        if (s.phase != Phase.READY || s.plan.isEmpty()) return
        if (s.mode == SessionMode.CHAIN && settings.current.dread) _state.value = s.copy(phase = Phase.DREAD)
        else startTimer(s)
    }

    fun setDread(level: Int) {
        val s = _state.value ?: return
        if (s.phase != Phase.DREAD) return
        startTimer(s.copy(dread = level))
    }

    private fun startTimer(s: SessionState) {
        _state.value = s.copy(phase = Phase.ACTIVE, index = 0, currentStartedAt = now(), quip = MessageGenerator.starting(spite))
        TimerService.start(context)
    }

    fun done() {
        val s = _state.value ?: return
        if (s.phase != Phase.ACTIVE) return
        val task = s.current ?: return
        val run = TaskRun(task.task.id, task.task.name, task.estimate, s.currentStartedAt, now())
        val chainId = if (s.mode == SessionMode.CHAIN) s.id else null
        scope.launch { repo.recordCompletion(run.taskId, run.actual, run.finishedAt, chainId) }
        val withRun = s.copy(runs = s.runs + run, quip = MessageGenerator.taskDone(spite, run.actual, run.estimate))
        if (s.next != null) _state.value = withRun.copy(phase = Phase.BETWEEN)
        else complete(withRun)
    }

    fun next() {
        val s = _state.value ?: return
        if (s.phase != Phase.BETWEEN) return
        _state.value = s.copy(phase = Phase.ACTIVE, index = s.index + 1, currentStartedAt = now(), quip = MessageGenerator.starting(spite))
    }

    /** "that's enough for now" between tasks: keep what's done, drop the rest. */
    fun finishEarly() {
        val s = _state.value ?: return
        if (s.runs.isEmpty()) {
            clear()
            return
        }
        complete(s.copy(plan = s.plan.take(s.runs.size), index = s.runs.size - 1))
    }

    /** From the complete screen: tack one more task on. Combo keeps counting if you're quick. */
    fun oneMore(task: Task) {
        val s = _state.value ?: return
        if (s.phase != Phase.COMPLETE) return
        _state.value = s.copy(
            plan = s.plan + PlannedTask(task, task.effectiveMinutes),
            index = s.plan.size,
            phase = Phase.ACTIVE,
            currentStartedAt = now(),
            quip = MessageGenerator.starting(spite),
        )
        TimerService.start(context)
    }

    /** Bail out of the timer. What's done stays done (and stays recorded). */
    fun abandon() {
        val s = _state.value ?: return
        if (s.runs.isNotEmpty()) save(s.copy(plan = s.plan.take(s.runs.size)))
        clear()
    }

    fun clear() {
        _state.value = null
    }

    private fun complete(s: SessionState) {
        save(s)
        val quip = if (s.mode == SessionMode.SINGLE && s.runs.size == 1) {
            val r = s.runs.first()
            MessageGenerator.singleComplete(spite, r.name, r.actual, r.estimate)
        } else MessageGenerator.chainComplete(spite, s.totalActual)
        _state.value = s.copy(phase = Phase.COMPLETE, quip = quip)
    }

    private fun save(s: SessionState) {
        val result = ChainResult(
            id = s.id,
            date = s.runs.lastOrNull()?.finishedAt ?: now(),
            tasks = s.runs.map { TaskResult(it.taskId, it.name, it.estimate, it.actual, s.dread) },
            totalActual = s.totalActual,
            totalEstimate = s.totalEstimate,
            totalDreadBefore = s.dread,
            combo = s.combo,
            isSingle = s.mode == SessionMode.SINGLE && s.runs.size == 1,
        )
        scope.launch { repo.saveChainResult(result) }
    }
}
