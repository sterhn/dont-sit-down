package com.dontsitdown.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class SettingsState(
    val onboarded: Boolean = false,
    val homeHour: Int = 19,
    val homeMinute: Int = 0,
    val weekendHour: Int = 12,
    val weekendMinute: Int = 0,
    /** java.time.DayOfWeek values, 1 = monday. */
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val snoozeLimit: Int = 3,
    val chainMaxMinutes: Int = 20,
    val spite: Boolean = false,
    val negotiator: Boolean = true,
    val ghost: Boolean = false,
    val scrollShame: Boolean = false,
    val weeklySummary: Boolean = true,
    val summaryDay: Int = 7,
    val summaryHour: Int = 20,
    val dread: Boolean = true,
    val theme: ThemeMode = ThemeMode.DARK,
) {
    fun homeTimeFor(dayOfWeek: Int): Pair<Int, Int> =
        if (dayOfWeek >= 6) weekendHour to weekendMinute else homeHour to homeMinute
}

/**
 * SharedPreferences rather than DataStore: alarm receivers need synchronous reads.
 * Also holds a little runtime bookkeeping for the reminder (snoozes, pending chain).
 */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    val current: SettingsState get() = _state.value

    fun update(change: (SettingsState) -> SettingsState) {
        val next = change(current)
        write(next)
        _state.value = next
    }

    private fun read(): SettingsState {
        val d = SettingsState()
        return SettingsState(
            onboarded = prefs.getBoolean("onboarded", d.onboarded),
            homeHour = prefs.getInt("homeHour", d.homeHour),
            homeMinute = prefs.getInt("homeMinute", d.homeMinute),
            weekendHour = prefs.getInt("weekendHour", d.weekendHour),
            weekendMinute = prefs.getInt("weekendMinute", d.weekendMinute),
            reminderDays = prefs.getStringSet("reminderDays", null)?.mapNotNull { it.toIntOrNull() }?.toSet() ?: d.reminderDays,
            snoozeLimit = prefs.getInt("snoozeLimit", d.snoozeLimit),
            chainMaxMinutes = prefs.getInt("chainMaxMinutes", d.chainMaxMinutes),
            spite = prefs.getBoolean("spite", d.spite),
            negotiator = prefs.getBoolean("negotiator", d.negotiator),
            ghost = prefs.getBoolean("ghost", d.ghost),
            scrollShame = prefs.getBoolean("scrollShame", d.scrollShame),
            weeklySummary = prefs.getBoolean("weeklySummary", d.weeklySummary),
            summaryDay = prefs.getInt("summaryDay", d.summaryDay),
            summaryHour = prefs.getInt("summaryHour", d.summaryHour),
            dread = prefs.getBoolean("dread", d.dread),
            theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", null) ?: "") }.getOrDefault(d.theme),
        )
    }

    private fun write(s: SettingsState) {
        prefs.edit()
            .putBoolean("onboarded", s.onboarded)
            .putInt("homeHour", s.homeHour)
            .putInt("homeMinute", s.homeMinute)
            .putInt("weekendHour", s.weekendHour)
            .putInt("weekendMinute", s.weekendMinute)
            .putStringSet("reminderDays", s.reminderDays.map { it.toString() }.toSet())
            .putInt("snoozeLimit", s.snoozeLimit)
            .putInt("chainMaxMinutes", s.chainMaxMinutes)
            .putBoolean("spite", s.spite)
            .putBoolean("negotiator", s.negotiator)
            .putBoolean("ghost", s.ghost)
            .putBoolean("scrollShame", s.scrollShame)
            .putBoolean("weeklySummary", s.weeklySummary)
            .putInt("summaryDay", s.summaryDay)
            .putInt("summaryHour", s.summaryHour)
            .putBoolean("dread", s.dread)
            .putString("theme", s.theme.name)
            .apply()
    }

    // --- reminder bookkeeping ---

    /** Task ids of the chain shown on the last takeover, so START opens exactly that chain. */
    var pendingChain: List<String>
        get() = prefs.getString("pendingChain", "")!!.split(",").filter { it.isNotBlank() }
        set(v) = prefs.edit().putString("pendingChain", v.joinToString(",")).apply()

    var lastFiredAt: Long
        get() = prefs.getLong("lastFiredAt", 0)
        set(v) = prefs.edit().putLong("lastFiredAt", v).apply()

    var lastOpenedAt: Long
        get() = prefs.getLong("lastOpenedAt", 0)
        set(v) = prefs.edit().putLong("lastOpenedAt", v).apply()

    var shameNudgedDay: Long
        get() = prefs.getLong("shameNudgedDay", -1)
        set(v) = prefs.edit().putLong("shameNudgedDay", v).apply()

    fun snoozesToday(): Int =
        if (prefs.getLong("snoozeDay", -1) == TimeUtils.today().toEpochDay()) prefs.getInt("snoozeCount", 0) else 0

    fun addSnooze(): Int {
        val n = snoozesToday() + 1
        prefs.edit().putLong("snoozeDay", TimeUtils.today().toEpochDay()).putInt("snoozeCount", n).apply()
        return n
    }

    fun resetSnoozes() = prefs.edit().putInt("snoozeCount", 0).apply()

    /** Last-shown message index per key, so the same quip never shows twice in a row. */
    fun lastMessageIndex(key: String): Int = prefs.getInt("msg_$key", -1)
    fun setLastMessageIndex(key: String, i: Int) = prefs.edit().putInt("msg_$key", i).apply()
}
