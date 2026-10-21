package com.dontsitdown.chain

import com.dontsitdown.data.model.ChainResult
import com.dontsitdown.data.model.Frequency
import com.dontsitdown.data.model.ReminderEvent
import com.dontsitdown.data.model.ReminderEventType
import com.dontsitdown.data.model.TaskCompletion
import com.dontsitdown.data.repository.TaskRepository
import com.dontsitdown.util.TimeUtils
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

data class GridRow(val name: String, val minutesByDay: List<Float?>)

data class Score(val couch: Int, val you: Int, val skipped: List<LocalDate>)

data class WeekSummary(
    val weekStart: LocalDate,
    val chains: Int,
    val totalMinutes: Float,
    val tasksDone: Int,
    val notable: List<String>,
    val score: Score,
    val fastest: Pair<Float, LocalDate>?,
    val avgDread: Double?,
    val avgReality: Double?,
    val conclusion: String,
    val grid: List<GridRow>,
) {
    val isEmpty: Boolean get() = tasksDone == 0 && score.couch == 0

    fun toText(): String = buildString {
        appendLine("this week:")
        appendLine("  $chains chain${if (chains == 1) "" else "s"} · ${totalMinutes.roundToInt()} minutes total")
        notable.forEach { appendLine("  $it ✓") }
        if (score.skipped.isNotEmpty()) {
            val days = score.skipped.joinToString(", ") { TimeUtils.dayName(it.dayOfWeek) }
            appendLine("  you skipped $days (the couch: ${score.couch}, you: ${score.you})")
        } else if (score.you > 0) {
            appendLine("  no skips. the couch: 0, you: ${score.you}")
        }
        fastest?.let { (m, d) -> appendLine("  fastest chain: ${TimeUtils.wholeMinutes(m)} min (${TimeUtils.dayName(d.dayOfWeek)})") }
        appendLine()
        avgDread?.let { appendLine("  avg dread: ${String.format(Locale.US, "%.1f", it)} 💀") }
        avgReality?.let { appendLine("  avg reality: ${TimeUtils.wholeMinutes(it.toFloat())} min") }
        append("  conclusion: $conclusion")
    }
}

object SummaryCalculator {

    /** A day goes to you if anything got done; to the couch if the reminder fired (or was refused) and nothing did. */
    fun score(completions: List<TaskCompletion>, events: List<ReminderEvent>, from: LocalDate, toExclusive: LocalDate, today: LocalDate): Score {
        val doneDays = completions.map { TimeUtils.toDate(it.date) }.toSet()
        val firedDays = events.filter { it.type == ReminderEventType.FIRED }.map { TimeUtils.toDate(it.timestamp) }.toSet()
        val refusedDays = events.filter { it.type == ReminderEventType.DISMISSED || it.type == ReminderEventType.REFUSED }
            .map { TimeUtils.toDate(it.timestamp) }.toSet()
        var couch = 0
        var you = 0
        val skipped = mutableListOf<LocalDate>()
        var d = from
        while (d.isBefore(toExclusive) && !d.isAfter(today)) {
            when {
                d in doneDays -> you++
                (d.isBefore(today) && d in firedDays) || d in refusedDays -> {
                    couch++
                    skipped += d
                }
            }
            d = d.plusDays(1)
        }
        return Score(couch, you, skipped)
    }

    suspend fun scoreBetween(repo: TaskRepository, from: LocalDate, toExclusive: LocalDate): Score {
        val a = TimeUtils.startOfDay(from)
        val b = TimeUtils.startOfDay(toExclusive)
        return score(repo.completionsBetween(a, b), repo.eventsBetween(a, b), from, toExclusive, TimeUtils.today())
    }

    suspend fun yearScore(repo: TaskRepository): Score {
        val today = TimeUtils.today()
        return scoreBetween(repo, today.withDayOfYear(1), today.plusDays(1))
    }

    suspend fun week(repo: TaskRepository, weekStart: LocalDate): WeekSummary {
        val from = TimeUtils.startOfDay(weekStart)
        val to = TimeUtils.startOfDay(weekStart.plusDays(7))
        val completions = repo.completionsBetween(from, to)
        val events = repo.eventsBetween(from, to)
        val results = repo.chainResultsBetween(from, to)
        val prev = repo.chainResultsBetween(TimeUtils.startOfDay(weekStart.minusDays(7)), from)
        val freq = repo.allTasks().associate { it.id to it.frequency }
        return build(weekStart, completions, events, results, prev, freq, TimeUtils.today())
    }

    fun build(
        weekStart: LocalDate,
        completions: List<TaskCompletion>,
        events: List<ReminderEvent>,
        results: List<ChainResult>,
        previousWeek: List<ChainResult>,
        frequencies: Map<String, Frequency>,
        today: LocalDate,
    ): WeekSummary {
        val chains = results.filter { !it.isSingle && it.tasks.isNotEmpty() }
        val rare = setOf(Frequency.WEEKLY, Frequency.BIWEEKLY, Frequency.MONTHLY)
        val notable = completions.filter { frequencies[it.taskId] in rare }.map { it.taskName }.distinct()

        val fastest = chains.filter { it.tasks.size >= 2 }.minByOrNull { it.totalActual }
            ?.let { it.totalActual to TimeUtils.toDate(it.date) }

        val avgDread = chains.filter { it.totalDreadBefore > 0 }.map { it.totalDreadBefore.toDouble() }.takeIf { it.isNotEmpty() }?.average()
        val prevDread = previousWeek.filter { !it.isSingle && it.totalDreadBefore > 0 }.map { it.totalDreadBefore.toDouble() }.takeIf { it.isNotEmpty() }?.average()
        val avgReality = chains.map { it.totalActual.toDouble() }.takeIf { it.isNotEmpty() }?.average()

        val grid = completions.groupBy { it.taskName }.map { (name, list) ->
            GridRow(name, (0..6).map { i ->
                val day = weekStart.plusDays(i.toLong())
                list.filter { TimeUtils.toDate(it.date) == day }.takeIf { it.isNotEmpty() }?.sumOf { it.minutes.toDouble() }?.toFloat()
            })
        }.sortedBy { row -> row.minutesByDay.indexOfFirst { it != null } }

        return WeekSummary(
            weekStart = weekStart,
            chains = chains.size,
            totalMinutes = completions.sumOf { it.minutes.toDouble() }.toFloat(),
            tasksDone = completions.size,
            notable = notable,
            score = score(completions, events, weekStart, weekStart.plusDays(7), today),
            fastest = fastest,
            avgDread = avgDread,
            avgReality = avgReality,
            conclusion = MessageGenerator.weeklyConclusion(avgDread, prevDread),
            grid = grid,
        )
    }
}
