package com.dontsitdown.util

import com.dontsitdown.data.model.Frequency
import com.dontsitdown.data.model.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

object TimeUtils {
    const val MINUTE = 60_000L
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun toDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun today(): LocalDate = LocalDate.now(zone)
    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
    fun at(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    fun nextDue(lastDone: Long?, frequency: Frequency): Long? {
        val interval = frequency.intervalDays ?: return null
        lastDone ?: return null
        return startOfDay(toDate(lastDone).plusDays(interval.toLong()))
    }

    /**
     * Days until due: negative = overdue, 0 = due today, null = as needed.
     * A recurring task that has never been done counts as due today.
     */
    fun dueInDays(task: Task, now: Long = System.currentTimeMillis()): Int? {
        task.frequency.intervalDays ?: return null
        val due = nextDue(task.lastDone, task.frequency) ?: return 0
        return ChronoUnit.DAYS.between(toDate(now), toDate(due)).toInt()
    }

    fun dueLabel(days: Int?): String = when {
        days == null -> "as needed"
        days < -1 -> "overdue by ${-days} days"
        days == -1 -> "overdue by 1 day"
        days == 0 -> "due today"
        days == 1 -> "due tomorrow"
        else -> "due in $days days"
    }

    fun lastDoneLabel(lastDone: Long?, now: Long = System.currentTimeMillis()): String {
        lastDone ?: return "never"
        val days = ChronoUnit.DAYS.between(toDate(lastDone), toDate(now)).toInt()
        return when {
            days <= 0 -> "today"
            days == 1 -> "yesterday"
            else -> "$days days ago"
        }
    }

    /** "06:12", or "1:02:03" past the hour. */
    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%02d:%02d", m, sec)
    }

    /** Whole minutes for display; never shows 0 for something that was actually done. */
    fun wholeMinutes(minutes: Float): Int = minutes.roundToInt().coerceAtLeast(1)

    fun minutesLabel(minutes: Float): String =
        if (minutes < 1f) "${(minutes * 60).roundToInt().coerceAtLeast(1)} sec" else "${wholeMinutes(minutes)} min"

    fun durationLabel(minutes: Float): String {
        val total = minutes.roundToInt()
        val h = total / 60
        val m = total % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> if (h == 1) "1 hr" else "$h hrs"
            else -> "${h} hr${if (h > 1) "s" else ""} $m min"
        }
    }

    fun hhmm(hour: Int, minute: Int): String = String.format(Locale.US, "%02d:%02d", hour, minute)

    fun dayName(day: DayOfWeek): String = day.name.lowercase()
    fun dayInitial(day: DayOfWeek): String = day.name.first().lowercase()

    private val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    fun dateLabel(ms: Long): String = toDate(ms).format(monthDay).lowercase()
    fun dateLabel(date: LocalDate): String = date.format(monthDay).lowercase()
}
