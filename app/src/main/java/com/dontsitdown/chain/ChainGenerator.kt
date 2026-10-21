package com.dontsitdown.chain

import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskSize
import com.dontsitdown.util.TimeUtils
import kotlin.random.Random

/**
 * Picks 2–4 tasks that fit in [maxMinutes].
 *
 * Priority: overdue first, then stalest relative to its frequency, with some randomness.
 * Shape: never two long tasks, prefers one medium plus one or two quick ones.
 */
object ChainGenerator {
    const val MIN_TASKS = 2
    const val MAX_TASKS = 4
    private const val ATTEMPTS = 80

    data class Scored(val task: Task, val minutes: Float, val score: Double) {
        val size: TaskSize get() = TaskSize.of(minutes)
    }

    fun comboKey(tasks: List<Task>): String = tasks.map { it.id }.sorted().joinToString("|")

    /**
     * @param exclude combo keys already shown (shuffle never repeats one while alternatives exist)
     * @param shuffle whether AS_NEEDED tasks may appear; they are never auto-scheduled otherwise
     */
    fun generate(
        tasks: List<Task>,
        now: Long,
        maxMinutes: Int,
        exclude: Set<String> = emptySet(),
        shuffle: Boolean = false,
        random: Random = Random.Default,
    ): List<Task> {
        val active = tasks.filter { it.isActive }
        if (active.isEmpty()) return emptyList()

        val scored = active.mapIndexedNotNull { i, t -> score(t, i, active.size, now, shuffle) }
        // Anything due-ish is preferred; fresh tasks only pad a chain that would otherwise be too short.
        val (due, fresh) = scored.partition { it.score > 0.0 }

        val chains = mutableMapOf<String, Pair<List<Scored>, Double>>()
        repeat(ATTEMPTS) {
            val chain = build(due, fresh, maxMinutes.toFloat(), random)
            if (chain.isNotEmpty()) {
                val key = comboKey(chain.map { it.task })
                if (key !in chains) chains[key] = chain to rate(chain, maxMinutes.toFloat())
            }
        }
        if (chains.isEmpty()) return emptyList()

        val fresh2 = chains.filterKeys { it !in exclude }.values.ifEmpty { chains.values }
        // Pick among the best few, so the top-ranked chain doesn't come back every single time.
        val top = fresh2.sortedByDescending { it.second }.take(3)
        return top[random.nextInt(top.size)].first.map { it.task }
    }

    /** Null = never put this in a chain right now. Score <= 0 = only as filler. */
    internal fun score(task: Task, index: Int, count: Int, now: Long, shuffle: Boolean): Scored? {
        val minutes = task.effectiveMinutes
        val preference = (1.0 - index.toDouble() / count.coerceAtLeast(1)) * 0.3
        val interval = task.frequency.intervalDays
        if (interval == null) {
            return if (shuffle) Scored(task, minutes, 0.4 + preference) else null
        }
        val daysSince = task.lastDone?.let { (now - it).toDouble() / TimeUtils.DAY } ?: (interval * 1.5)
        val ratio = daysSince / interval
        val base = when {
            ratio >= 1.0 -> 3.0 + (ratio - 1.0) * 2.0 // overdue (or due): dominates everything else
            ratio >= 0.5 -> ratio                     // getting stale
            else -> ratio - 1.0                       // just done: filler only
        }
        return Scored(task, minutes, base + preference)
    }

    private fun build(due: List<Scored>, fresh: List<Scored>, max: Float, random: Random): List<Scored> {
        val jittered = due.map { it to it.score + random.nextDouble() * 1.2 }
            .sortedByDescending { it.second }
            .map { it.first }

        val pool = due + fresh
        val chain = mutableListOf<Scored>()
        var total = 0f
        fun tryAdd(c: Scored): Boolean {
            if (chain.size >= MAX_TASKS || c in chain) return false
            if (total + c.minutes > max) return false
            if (c.size == TaskSize.LONG && chain.any { it.size == TaskSize.LONG }) return false
            // A first task must leave room for a second one: a lone task filling the budget isn't a chain.
            if (chain.isEmpty() && pool.none { it !== c && it.minutes <= max - c.minutes }) return false
            chain += c
            total += c.minutes
            return true
        }

        for (c in jittered) {
            // Once we have the classic shape (one medium/long + quick ones), stop chasing more mediums.
            val hasAnchor = chain.any { it.size != TaskSize.QUICK }
            if (hasAnchor && c.size != TaskSize.QUICK && chain.size >= 2 && random.nextDouble() < 0.7) continue
            tryAdd(c)
            if (chain.size >= MAX_TASKS) break
        }

        if (chain.size < MIN_TASKS) {
            // Pad with the smallest things that fit, fresh or not.
            pool.filter { it !in chain }.sortedBy { it.minutes + random.nextFloat() }.forEach {
                if (chain.size < MIN_TASKS) tryAdd(it)
            }
        }
        // Order: anchor (biggest) first while energy is high, quick ones after.
        return chain.sortedByDescending { it.minutes }
    }

    private fun rate(chain: List<Scored>, max: Float): Double {
        val sizes = chain.map { it.size }
        val quick = sizes.count { it == TaskSize.QUICK }
        val medium = sizes.count { it == TaskSize.MEDIUM }
        val long = sizes.count { it == TaskSize.LONG }
        var r = chain.sumOf { it.score }
        if (chain.size < MIN_TASKS) r -= 5.0
        if (medium + long == 1 && quick in 1..2) r += 1.5
        if (long == 0 && medium == 0) r -= 0.5
        r += (chain.sumOf { it.minutes.toDouble() } / max) * 0.8
        return r
    }
}
