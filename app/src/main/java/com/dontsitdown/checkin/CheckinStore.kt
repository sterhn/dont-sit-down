package com.dontsitdown.checkin

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * The check-in lives in one small JSON file (same shape as the web app's export).
 * Every change lands in memory at once and is written a moment later, so typing
 * a note doesn't hit the disk on every key, and nothing is ever lost to a forgotten "save".
 */
class CheckinStore(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    private val file = AtomicFile(File(context.filesDir, "checkin.json"))
    private val _state = MutableStateFlow(load())
    val state: StateFlow<CheckinData> = _state.asStateFlow()
    val current: CheckinData get() = _state.value
    private var pending: Job? = null

    private fun load(): CheckinData = runCatching {
        if (!file.baseFile.exists()) return@runCatching CheckinData()
        CheckinJson.decode(file.readFully().decodeToString()) ?: CheckinData()
    }.getOrElse {
        Log.w("CheckinStore", "unreadable check-in file, starting fresh", it)
        CheckinData()
    }

    fun update(change: (CheckinData) -> CheckinData) {
        _state.update(change)
        pending?.cancel()
        pending = scope.launch {
            delay(SAVE_DELAY_MS)
            write(_state.value)
        }
    }

    /** Write now (app going to the background, before an export). */
    fun flush() {
        pending?.cancel()
        pending = null
        write(_state.value)
    }

    @Synchronized
    private fun write(data: CheckinData) {
        val out = file.startWrite()
        try {
            out.write(CheckinJson.encode(data).toByteArray())
            file.finishWrite(out)
            onSaved()
        } catch (e: Exception) {
            file.failWrite(out)
            Log.e("CheckinStore", "couldn't save the check-in", e)
        }
    }

    /** After every save: keep the home-screen widget in step. */
    private fun onSaved() = runCatching { com.dontsitdown.widget.CheckinWidget.refresh(appContext) }

    fun replaceAll(data: CheckinData) {
        _state.value = data
        flush()
    }

    fun export(): String {
        flush()
        return CheckinJson.encode(current)
    }

    // ---- edits ----

    fun setMorning(date: LocalDate, change: (Morning) -> Morning) = update { d ->
        val k = Checkin.key(date)
        val day = d.day(k)
        d.copy(days = d.days + (k to day.copy(morning = change(day.morning ?: Morning()).copy(savedAt = now()))))
    }

    fun setEvening(date: LocalDate, change: (Evening) -> Evening) = update { d ->
        val k = Checkin.key(date)
        val day = d.day(k)
        d.copy(days = d.days + (k to day.copy(evening = change(day.evening ?: Evening()).copy(savedAt = now()))))
    }

    fun addBoundary(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || current.boundaries.any { it.equals(n, ignoreCase = true) }) return false
        update { it.copy(boundaries = it.boundaries + n) }
        return true
    }

    /** Past check-ins that recorded it keep it; an empty slot on [today] is dropped. */
    fun removeBoundary(name: String, today: LocalDate) = update { d ->
        val k = Checkin.key(today)
        val ev = d.days[k]?.evening
        val days = if (ev != null && ev.boundaries.containsKey(name) && ev.boundaries[name] == null) {
            d.days + (k to d.day(k).copy(evening = ev.copy(boundaries = ev.boundaries - name)))
        } else d.days
        d.copy(boundaries = d.boundaries - name, days = days)
    }

    fun addGoal(title: String, period: String, target: Int): String? {
        val t = title.trim().replaceFirstChar { it.uppercase() }
        if (t.isEmpty()) return "give it a name first"
        if (current.goals.any { it.title.equals(t, ignoreCase = true) }) return "you already have that one"
        update { it.copy(goals = it.goals + Goal(Checkin.uid(), t, period, target.coerceIn(1, 31), "star")) }
        return null
    }

    fun removeGoal(id: String) = update { d -> d.copy(goals = d.goals.filter { it.id != id }, log = d.log.filter { it.goal != id }) }

    fun setCurrent(goalId: String, text: String) = update { d ->
        d.copy(goals = d.goals.map { if (it.id == goalId) it.copy(current = text) else it })
    }

    fun log(goalId: String, date: LocalDate, text: String, tag: String?) = update { d ->
        d.copy(log = d.log + LogEntry(Checkin.uid(), goalId, Checkin.key(date), text.trim(), tag))
    }

    fun removeEntry(id: String) = update { d -> d.copy(log = d.log.filter { it.id != id }) }

    private fun now() = Instant.now().toString()

    companion object {
        const val SAVE_DELAY_MS = 400L
    }
}
