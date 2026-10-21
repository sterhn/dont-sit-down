package com.dontsitdown.chain

import com.dontsitdown.util.Settings
import com.dontsitdown.util.TimeUtils
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Every line the app says lives here. Direct, a little irreverent, never a motivational poster.
 * Normal voice is warm-ish; spite voice is for days that need adversarial energy.
 */
object MessageGenerator {
    private var settings: Settings? = null
    private val lastIndex = mutableMapOf<String, Int>()

    fun init(settings: Settings) {
        this.settings = settings
    }

    /** Random pick that never repeats the previous pick for the same key. */
    private fun pick(key: String, options: List<String>): String {
        if (options.size == 1) return options[0]
        val last = lastIndex[key] ?: settings?.lastMessageIndex(key) ?: -1
        var i: Int
        do {
            i = options.indices.random()
        } while (i == last)
        lastIndex[key] = i
        settings?.setLastMessageIndex(key, i)
        return options[i]
    }

    private fun mins(m: Float) = TimeUtils.wholeMinutes(m)

    // --- the chain ---

    const val HEADLINE = "don't sit down."

    fun readySubline(spite: Boolean, minutes: Float, now: LocalDateTime = LocalDateTime.now()): String {
        val m = mins(minutes)
        if (spite) return pick(
            "ready_spite", listOf(
                "your past self left this for you again. classic her.",
                "the couch can wait $m minutes. it's not going anywhere. neither are the dishes.",
                "you know exactly what happens if you sit down first.",
                "$m minutes. you've spent longer choosing a show.",
            )
        )
        val contextual = buildList {
            if (now.hour >= 21) add("it's late. still counts. $m minutes.")
            if (now.dayOfWeek == DayOfWeek.FRIDAY) add("it's friday. $m minutes and the weekend starts clean.")
            if (now.dayOfWeek == DayOfWeek.MONDAY) add("monday. get it out of the way. $m minutes.")
        }
        return pick(
            "ready", contextual + listOf(
                "$m minutes. go.",
                "$m minutes. then the couch is all yours.",
                "shoes still on? good. $m minutes.",
                "$m minutes. you've done harder things before breakfast.",
            )
        )
    }

    fun takeoverTail(minutes: Float) = "${mins(minutes)} minutes.\nthat's it."

    fun starting(spite: Boolean): String = if (spite) pick(
        "start_spite", listOf(
            "every minute you stall, tomorrow-you gets madder.",
            "clock's running. so is your excuse budget.",
        )
    ) else pick(
        "start", listOf("go.", "clock's running.", "no thinking. just start.")
    )

    fun taskDone(spite: Boolean, actual: Float, estimate: Float): String {
        val fast = actual < estimate * 0.85f
        val slow = actual > estimate * 1.3f
        return when {
            spite && fast -> pick("done_fast_spite", listOf("see, that was nothing. you dramatic.", "that's it? that's what you were avoiding?"))
            spite && slow -> pick("done_slow_spite", listOf("slower than planned. still beat the couch.", "took a while. tomorrow-you still says thanks."))
            spite -> pick("done_spite", listOf("done. what was all the fuss about.", "right on schedule. annoyingly."))
            fast -> pick("done_fast", listOf("faster than you thought.", "under. obviously.", "quicker than the dread, as usual."))
            slow -> pick("done_slow", listOf("took a bit. still done.", "longer than planned. doesn't matter. done."))
            else -> pick("done", listOf("right on time.", "exactly as long as it takes.", "done. next."))
        }
    }

    fun chainComplete(spite: Boolean, actual: Float): String {
        val m = mins(actual)
        return if (spite) pick(
            "complete_spite", listOf(
                "you just did in $m minutes what you spent 2 hours dreading. let that sink in.",
                "$m minutes. the couch is disappointed in how little it got.",
                "done in $m. so what was the big deal, exactly?",
            )
        ) else pick(
            "complete", listOf(
                "told you. $m minutes.",
                "$m minutes. that's the whole thing.",
                "done. $m minutes. now sit down.",
                "$m minutes. you can sit down now. properly.",
            )
        )
    }

    fun singleComplete(spite: Boolean, name: String, actual: Float, estimate: Float) =
        "$name: done. ${TimeUtils.minutesLabel(actual)}.\n" + taskDone(spite, actual, estimate)

    fun emptyChain(spite: Boolean): String = if (spite) "nothing's due. suspicious, but fine. sit down."
    else "nothing's due. genuinely. sit down."

    fun oneMorePrompt() = pick("one_more", listOf("got one more in you?", "while you're up…", "one more. quick one."))

    // --- dread ---

    val dreadLabels = listOf("fine", "ugh", "i'd rather\nnot exist")

    fun dreadSkulls(level: Int) = "💀".repeat(level.coerceIn(1, 3))

    fun dreadLevelName(level: Int) = when (level) {
        1 -> "(no big deal)"
        2 -> "(moderate suffering)"
        else -> "(maximum suffering)"
    }

    fun realityCheck(dread: Int, actual: Float, estimate: Float): String = when {
        dread >= 3 && actual <= estimate * 1.1f -> pick("reality3", listOf("barely an inconvenience", "an inconvenience, at most", "less than a podcast episode"))
        dread >= 3 -> "took a bit. still not worth the dread"
        dread == 2 && actual <= estimate * 1.1f -> pick("reality2", listOf("mildly annoying, at worst", "fine, actually", "the 'ugh' was bigger than the task"))
        dread == 2 -> "annoying, and then over"
        else -> pick("reality1", listOf("fine, as predicted", "you called it", "no notes"))
    }

    fun dreadConclusion(avgDread: Double, avgMinutes: Double): String = when {
        avgDread >= 2.3 && avgMinutes <= 22 -> "you're dramatic"
        avgDread >= 1.7 -> "a little dramatic"
        else -> "suspiciously reasonable"
    }

    fun weeklyConclusion(avgDread: Double?, prevAvgDread: Double?): String {
        avgDread ?: return "not enough data to judge you. yet."
        val improving = prevAvgDread != null && avgDread < prevAvgDread - 0.15
        return when {
            avgDread >= 2.0 && improving -> "still dramatic but improving"
            avgDread >= 2.0 -> "you're dramatic"
            improving -> "dreading less. the data is getting through to you"
            else -> "suspiciously reasonable"
        }
    }

    // --- combo ---

    fun comboLabel(n: Int) = when {
        n <= 2 -> "double down"
        n == 3 -> "on a roll"
        n == 4 -> "unstoppable"
        else -> "who are you and what did you do with me"
    }

    // --- reminder, negotiator, ghost ---

    fun snoozeLimit(count: Int, firstTask: String?): String {
        val thing = firstTask?.let { nounPlural(it) } ?: "the dishes"
        return "you've snoozed $count times. $thing ${if (thing.endsWith("s")) "aren't" else "isn't"} getting cleaner."
    }

    fun skipped(spite: Boolean, couch: Int, you: Int): String =
        if (spite) "the couch won today. it's $couch-$you for the year, by the way."
        else "skipped today. noted."

    data class Offer(val text: String, val yes: String, val no: String)

    fun negotiatorFirst(task: String, minutes: Float): Offer {
        val m = mins(minutes)
        return Offer(
            "okay. not the full chain.\nbut what about just ${noun(task)}?\n$m minutes. you can do $m minutes.",
            "fine, $m minutes", "no",
        )
    }

    fun negotiatorSecond(task: String, minutes: Float): Offer {
        val m = mins(minutes)
        return Offer(
            "${noun(task)} then. $m minutes.\nthat's less time than this conversation.",
            "okay fine", "leave me alone",
        )
    }

    const val NOTED = "alright. noted."

    fun ghost(task: String?): Offer = Offer(
        "tomorrow-you is going to wake up\nto ${consequence(task)}.\n\nshe's not going to be happy about it.",
        "ugh, fine", "she can deal",
    )

    fun scrollShame(hoursSinceHome: Int, chainMinutes: Float): Offer {
        val times = (hoursSinceHome * 60 / mins(chainMinutes).coerceAtLeast(1))
        val hrs = if (hoursSinceHome == 1) "an hour" else "$hoursSinceHome hours"
        return Offer(
            "it's been $hrs since you got home.\nthe tasks are still there.\nyou could have done them $times times by now.",
            "", "",
        )
    }

    fun notificationText(taskNames: List<String>, minutes: Float) =
        taskNames.joinToString(" · ") + " — ${mins(minutes)} minutes. that's it."

    // --- task stats ---

    fun lifetimeKicker(count: Int, totalMinutes: Float): String = when {
        count == 0 -> "no history yet. go find out how long it really takes."
        totalMinutes >= 60 -> "which sounds like a lot until you think about\nhow many hours you've spent thinking about\ndoing them."
        else -> "less time than one episode of anything."
    }

    fun slowestNote(slowest: Float, average: Float): String? =
        if (slowest > average * 1.6f) pick("slow_note", listOf("(you were on the phone)", "(something distracted you)", "(we don't talk about that one)"))
        else null

    // --- wording helpers ---

    private val nouns = mapOf(
        "take out trash" to "the trash",
        "take out recycling" to "the recycling",
        "wipe counters" to "the counters",
        "wipe stove" to "the stove",
        "water plants" to "the plants",
        "put away clean laundry" to "the clean laundry",
        "dishes" to "the dishes",
        "quick tidy" to "a quick tidy",
        "sweep kitchen" to "sweeping the kitchen",
        "clean bathroom sink" to "the bathroom sink",
        "vacuum" to "vacuuming",
        "change towels" to "the towels",
        "load/switch laundry" to "the laundry",
        "change sheets" to "the sheets",
        "mop floors" to "the floors",
        "clean toilet" to "the toilet",
        "clean shower/tub" to "the shower",
        "deep clean fridge" to "the fridge",
        "organize a drawer/shelf" to "one drawer",
    )

    fun noun(task: String): String = nouns[task.lowercase()] ?: task.lowercase()

    private fun nounPlural(task: String): String = noun(task).let { if (it.startsWith("the ")) it else "the $it" }

    private val consequences = mapOf(
        "dishes" to "a sink full of dishes",
        "take out trash" to "a full trash can",
        "take out recycling" to "an avalanche of recycling",
        "wipe counters" to "sticky counters",
        "wipe stove" to "yesterday's stove",
        "quick tidy" to "all of today's stuff, everywhere",
        "load/switch laundry" to "laundry that smells like a decision",
        "put away clean laundry" to "the chair of clean clothes",
        "sweep kitchen" to "crunchy kitchen floors",
        "change sheets" to "the same sheets, again",
        "vacuum" to "the crumbs",
    )

    private fun consequence(task: String?): String =
        task?.let { consequences[it.lowercase()] } ?: task?.let { "${noun(it)}, still not done" } ?: "everything, exactly where you left it"

    fun percentFaster(actual: Float, estimate: Float): Int =
        if (estimate <= 0f) 0 else ((1f - actual / estimate) * 100).roundToInt()
}
