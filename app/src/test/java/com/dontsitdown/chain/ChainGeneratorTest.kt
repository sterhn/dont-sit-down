package com.dontsitdown.chain

import com.dontsitdown.data.DefaultTasks
import com.dontsitdown.data.model.Frequency
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskSize
import com.dontsitdown.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChainGeneratorTest {
    private val now = 1_760_000_000_000L
    private fun daysAgo(d: Double) = now - (d * TimeUtils.DAY).toLong()

    @Test
    fun defaultListAlwaysYieldsAValidChain() {
        val tasks = DefaultTasks.create(now)
        repeat(300) { seed ->
            val chain = ChainGenerator.generate(tasks, now, 20, random = Random(seed))
            assertTrue("size ${chain.size}", chain.size in 2..4)
            assertTrue("total ${chain.sumOf { it.estimateMinutes }}", chain.sumOf { it.effectiveMinutes.toDouble() } <= 20.0)
            assertTrue("two long tasks", chain.count { it.size == TaskSize.LONG } <= 1)
            assertTrue("as-needed task auto-scheduled", chain.none { it.frequency == Frequency.AS_NEEDED })
        }
    }

    @Test
    fun overdueTasksComeFirst() {
        val tasks = DefaultTasks.create(now).map {
            // Everything freshly done, except one overdue daily task.
            if (it.name == "dishes") it.copy(lastDone = daysAgo(3.0)) else it.copy(lastDone = daysAgo(0.1))
        }
        repeat(100) { seed ->
            val chain = ChainGenerator.generate(tasks, now, 20, random = Random(seed))
            assertTrue(chain.any { it.name == "dishes" })
        }
    }

    @Test
    fun usesLearnedTimesOverEstimates() {
        val slow = Task(name = "dishes", estimateMinutes = 5, frequency = Frequency.DAILY, actualTimes = listOf(30f, 30f, 30f))
        assertEquals(30f, slow.effectiveMinutes)
        val tasks = listOf(slow, Task(name = "a", estimateMinutes = 3, frequency = Frequency.DAILY), Task(name = "b", estimateMinutes = 4, frequency = Frequency.DAILY))
        repeat(50) { seed ->
            val chain = ChainGenerator.generate(tasks, now, 20, random = Random(seed))
            assertTrue(chain.none { it.name == "dishes" })
        }
    }

    @Test
    fun shuffleAvoidsShownCombos() {
        val tasks = DefaultTasks.create(now)
        val first = ChainGenerator.generate(tasks, now, 20, random = Random(1))
        val shown = setOf(ChainGenerator.comboKey(first))
        repeat(50) { seed ->
            val next = ChainGenerator.generate(tasks, now, 20, exclude = shown, shuffle = true, random = Random(seed))
            assertTrue(ChainGenerator.comboKey(next) !in shown)
        }
    }

    @Test
    fun tooFewTasksDegradesGracefully() {
        assertEquals(emptyList<Task>(), ChainGenerator.generate(emptyList(), now, 20))
        val one = listOf(Task(name = "trash", estimateMinutes = 3, frequency = Frequency.DAILY))
        assertTrue(ChainGenerator.generate(one, now, 20).size <= 1)
    }
}
