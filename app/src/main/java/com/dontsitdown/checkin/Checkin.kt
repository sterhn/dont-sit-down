package com.dontsitdown.checkin

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

/** The character's faces: one sticker each, in res/drawable-nodpi/char_*.webp. */
enum class Face {
    NEUTRAL, HAPPY, GREAT, LOW, ROUGH, SLEEPY, PROUD, COFFEE, THINKING, COZY,
    SIGH, NICE, ANNOYED, SURPRISED, FLUSTERED, AWAY;
}

/** One day's numbers for charts: evening wins over morning, like the web app. */
data class DayNumbers(val date: LocalDate, val sleep: Int?, val mood: Int?, val body: Int?, val boundaries: Map<String, String?>?)

/**
 * Everything the check-in knows how to work out, as plain functions over [CheckinData].
 * Ports of the web app's goal and story logic, so both agree on every number. No streaks: on purpose.
 */
object Checkin {
    val MOOD_WORDS = listOf("drained", "rough", "low", "meh", "okay", "fine", "good", "bright", "great", "radiant")

    fun key(d: LocalDate): String = d.toString()
    fun date(key: String): LocalDate = LocalDate.parse(key)
    fun uid(): String = java.lang.Long.toString(System.currentTimeMillis(), 36) + Random.nextInt(36 * 36 * 36 * 36, 36 * 36 * 36 * 36 * 36).toString(36)

    fun moodWord(v: Int?): String = if (v == null) "" else MOOD_WORDS[(v - 1).coerceIn(0, 9)]

    // ---- filled ----

    fun morningFilled(m: Morning?): Boolean =
        m != null && (m.sleep != null || m.mood != null || m.body != null || m.intention.isNotBlank())

    fun eveningFilled(e: Evening?): Boolean =
        e != null && (e.mood != null || e.body != null || e.gym != null || e.happened.isNotBlank() ||
            e.honest.isNotBlank() || e.spent.isNotBlank() || e.boundaries.values.any { it != null })

    /** A day is written down if either check-in has something in it. */
    fun dayFilled(data: CheckinData, d: LocalDate): Boolean {
        val day = data.days[key(d)] ?: return false
        return morningFilled(day.morning) || eveningFilled(day.evening)
    }

    fun weekStart(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

    // ---- the character ----

    /** Which face fits a 1–10 mood. [idle] before a mood is picked; [tired] turns the middle sleepy. */
    fun faceFor(mood: Int?, idle: Face = Face.NEUTRAL, tired: Boolean = false): Face = when {
        mood == null -> if (tired) Face.SLEEPY else idle
        mood <= 2 -> Face.ROUGH
        mood <= 4 -> Face.LOW
        mood <= 6 -> if (tired) Face.SLEEPY else Face.NEUTRAL
        mood <= 8 -> Face.HAPPY
        else -> Face.GREAT
    }

    val FACE_CAPTIONS = mapOf(
        Face.COFFEE to "coffee first", Face.COZY to "cozy hours", Face.SLEEPY to "so sleepy", Face.SIGH to "oh well",
        Face.NICE to "nice", Face.ANNOYED to "hey.", Face.SURPRISED to "whoa", Face.GREAT to "yay!", Face.PROUD to "did it",
        Face.AWAY to "not home",
    )

    val POKES = listOf(Face.NICE to "hey you", Face.SURPRISED to "boop!", Face.GREAT to "hi hi", Face.FLUSTERED to "stop it", Face.HAPPY to "hehe")

    // ---- goals ----

    /** The period a date falls in: "yyyy-MM" for monthly goals, the monday's key for weekly ones. */
    fun periodOf(goal: Goal, d: LocalDate): String = if (goal.monthly) key(d).take(7) else key(weekStart(d))

    fun entries(data: CheckinData, goal: Goal) = data.log.filter { it.goal == goal.id }

    fun periodCount(data: CheckinData, goal: Goal, period: String) =
        entries(data, goal).count { runCatching { periodOf(goal, date(it.date)) }.getOrNull() == period }

    fun daysLeft(goal: Goal, today: LocalDate): Int =
        if (goal.monthly) today.lengthOfMonth() - today.dayOfMonth + 1 else 8 - today.dayOfWeek.value

    fun periodLeft(goal: Goal, today: LocalDate): String = daysLeft(goal, today).let { if (it == 1) "last day" else "$it days left" }

    fun unit(goal: Goal, n: Int) = if (goal.monthly) (if (n == 1) "month" else "months") else (if (n == 1) "week" else "weeks")

    /** Goals not yet met this period, the one with the least time left first. */
    fun pendingGoals(data: CheckinData, today: LocalDate): List<Pair<Goal, Int>> =
        data.goals.map { it to periodCount(data, it, periodOf(it, today)) }
            .filter { (g, n) -> n < g.target }
            .sortedBy { (g, _) -> daysLeft(g, today) }

    /** Which doodle a goal gets: its own for the three classics, then a rotation. */
    fun doodleFor(data: CheckinData, goal: Goal): Doodle = when (goal.icon) {
        "place" -> Doodle.MAP
        "food" -> Doodle.RAMEN
        "book" -> Doodle.BOOK
        else -> {
            val i = data.goals.filter { it.icon !in setOf("place", "food", "book") }.indexOf(goal).coerceAtLeast(0)
            EXTRA_DOODLES[i % EXTRA_DOODLES.size]
        }
    }

    private val EXTRA_DOODLES = listOf(Doodle.STAR, Doodle.BEAR, Doodle.HEART, Doodle.CLOUD, Doodle.FLOWER)

    val GOAL_IDEAS = listOf(
        "Message someone you miss" to "week",
        "A walk with no phone" to "week",
        "One small kind thing for someone" to "week",
        "Try a new workout" to "week",
        "See a film you'd never pick" to "month",
        "A museum, gallery or show" to "month",
        "A day trip" to "month",
        "Learn one new skill" to "month",
    )

    fun collectionName(g: Goal) = when (g.id) {
        "new-place" -> "new places"
        "new-food" -> "new foods"
        "book" -> "books read"
        else -> g.title.lowercase()
    }

    // ---- numbers over time ----

    fun numbers(data: CheckinData, d: LocalDate): DayNumbers {
        val day = data.days[key(d)]
        return DayNumbers(
            d,
            sleep = day?.morning?.sleep,
            mood = day?.evening?.mood ?: day?.morning?.mood,
            body = day?.evening?.body ?: day?.morning?.body,
            boundaries = day?.evening?.boundaries,
        )
    }

    /** [n] days ending [shift] days before [today], oldest first. */
    fun window(data: CheckinData, today: LocalDate, n: Int, shift: Int = 0): List<DayNumbers> =
        (n - 1 downTo 0).map { numbers(data, today.minusDays((it + shift).toLong())) }

    fun avg(xs: List<Int>): Double? = if (xs.isEmpty()) null else xs.average()

    fun fmt1(x: Double?): String = if (x == null) "–" else String.format(Locale.US, "%.1f", (x * 10).roundToInt() / 10.0)

    // ---- reading back ----

    /** "sleep 4 · body 6 · mood 5 → 7": morning to evening when both are there. */
    fun scoreBits(data: CheckinData, d: LocalDate): List<String> {
        val day = data.days[key(d)]
        val m = day?.morning
        val e = day?.evening
        fun arc(a: Int?, b: Int?): String? = if (a != null && b != null && a != b) "$a → $b" else (b ?: a)?.toString()
        return buildList {
            m?.sleep?.let { add("sleep $it") }
            arc(m?.body, e?.body)?.let { add("body $it") }
            arc(m?.mood, e?.mood)?.let { add("mood $it") }
        }
    }

    private fun listJoin(xs: List<String>) = if (xs.size < 2) xs.joinToString("") else xs.dropLast(1).joinToString(", ") + " and " + xs.last()

    /** The day's words as label/text pairs; [full] adds boundaries and gym. */
    fun storyLines(data: CheckinData, d: LocalDate, full: Boolean): List<Pair<String, String>> {
        val day = data.days[key(d)]
        val m = day?.morning
        val e = day?.evening
        val out = mutableListOf<Pair<String, String>>()
        fun add(label: String, text: String?) {
            if (!text.isNullOrBlank()) out += label to text.trim()
        }
        add("intention", m?.intention)
        add("what happened", e?.happened)
        add("one honest thing", e?.honest)
        if (full && e != null) {
            val held = e.boundaries.filterValues { it == "held" }.keys.toList()
            val slipped = e.boundaries.filterValues { it == "slipped" }.keys.toList()
            val parts = buildList {
                if (held.isNotEmpty()) add("held ${listJoin(held)}")
                if (slipped.isNotEmpty()) add("slipped on ${listJoin(slipped)}")
            }
            if (parts.isNotEmpty()) out += "boundaries" to parts.joinToString("; ")
            e.gym?.let { out += "gym" to (GYM_TEXT[it] ?: it) }
        }
        add("spent", e?.spent)
        return out
    }

    val GYM_TEXT = mapOf("went" to "went", "rest" to "rest day", "skipped" to "skipped")

    fun journalDates(data: CheckinData): List<LocalDate> = data.days.entries
        .filter { (_, d) -> !d.morning?.intention.isNullOrBlank() || !d.evening?.happened.isNullOrBlank() || !d.evening?.honest.isNullOrBlank() || !d.evening?.spent.isNullOrBlank() }
        .mapNotNull { runCatching { date(it.key) }.getOrNull() }
        .sortedDescending()

    // ---- dates as words ----

    private val LONG = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)
    private val SHORT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
    private val SHORT_YEAR = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
    private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK)

    fun pretty(d: LocalDate): String = d.format(LONG)
    fun short(d: LocalDate, today: LocalDate): String = d.format(if (d.year == today.year) SHORT else SHORT_YEAR)
    fun dayMonth(d: LocalDate): String = d.format(DAY_MONTH).lowercase()
    fun monthYear(d: LocalDate): String = d.format(MONTH_YEAR)
    fun narrow(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.UK)

    // ---- markdown ----

    fun dayMarkdown(data: CheckinData, d: LocalDate, chores: List<Pair<String, Float>> = emptyList()): String {
        val day = data.day(key(d))
        val m = day.morning
        val e = day.evening
        val lines = mutableListOf("# ${pretty(d)}", "")
        if (m != null) {
            lines += "## morning"
            lines += "- sleep: ${m.sleep ?: "–"}/10"
            lines += "- body: ${m.body ?: "–"}/10"
            lines += "- mood: ${m.mood ?: "–"}/10" + (m.mood?.let { " (${moodWord(it)})" } ?: "")
            if (m.intention.isNotBlank()) lines += "- intention: ${m.intention}"
            lines += ""
        }
        if (e != null) {
            lines += "## evening"
            lines += "- body: ${e.body ?: "–"}/10"
            lines += "- mood: ${e.mood ?: "–"}/10" + (e.mood?.let { " (${moodWord(it)})" } ?: "")
            val bounds = e.boundaries.filterValues { it != null }
            if (bounds.isNotEmpty()) {
                lines += "- boundaries:"
                bounds.forEach { (name, s) -> lines += "  - $name: $s" }
            }
            e.gym?.let { lines += "- gym: $it" }
            if (e.spent.isNotBlank()) lines += "- spent: ${e.spent}"
            if (e.happened.isNotBlank()) lines += "- what happened: ${e.happened}"
            if (e.honest.isNotBlank()) lines += "- one honest thing: ${e.honest}"
            lines += ""
        }
        if (chores.isNotEmpty()) {
            lines += "## chores"
            chores.forEach { (name, min) -> lines += "- $name: ${min.roundToInt().coerceAtLeast(1)} min" }
            lines += ""
        }
        val logged = data.log.filter { it.date == key(d) }
        if (logged.isNotEmpty()) {
            lines += "## goals"
            logged.forEach { l ->
                val g = data.goals.find { it.id == l.goal }
                lines += "- ${g?.title?.lowercase() ?: "goal"}: ${l.text}" + (l.tag?.let { " ($it)" } ?: "")
            }
        }
        return lines.joinToString("\n").trim() + "\n"
    }

    fun weekMarkdown(data: CheckinData, start: LocalDate, today: LocalDate, chores: Map<LocalDate, List<Pair<String, Float>>> = emptyMap()): String {
        val end = start.plusDays(6)
        val lines = mutableListOf("# week of ${start.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)).lowercase()}", "")
        for (i in 0..6) {
            val d = start.plusDays(i.toLong())
            if (d.isAfter(today)) break
            val bits = scoreBits(data, d)
            val story = storyLines(data, d, true)
            val ch = chores[d].orEmpty()
            if (bits.isEmpty() && story.isEmpty() && ch.isEmpty()) continue
            lines += "## ${pretty(d).lowercase()}"
            if (bits.isNotEmpty()) lines += "- ${bits.joinToString(" · ")}"
            story.forEach { (label, text) -> lines += "- $label: $text" }
            if (ch.isNotEmpty()) lines += "- chores: " + ch.joinToString(", ") { (n, m) -> "$n ${m.roundToInt().coerceAtLeast(1)}m" }
            lines += ""
        }
        val logged = data.log.filter { it.date >= key(start) && it.date <= key(end) }.sortedBy { it.date }
        if (logged.isNotEmpty()) {
            lines += "## goals"
            logged.forEach { l ->
                val g = data.goals.find { it.id == l.goal }
                lines += "- ${g?.title?.lowercase() ?: "goal"}: ${l.text}" + (l.tag?.let { " ($it)" } ?: "")
            }
        }
        return lines.joinToString("\n").trim() + "\n"
    }
}

enum class Doodle { MAP, RAMEN, BOOK, STAR, HEART, CLOUD, FLOWER, BEAR }
