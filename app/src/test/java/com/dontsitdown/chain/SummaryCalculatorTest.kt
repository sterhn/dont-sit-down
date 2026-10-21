package com.dontsitdown.chain

import com.dontsitdown.data.model.ReminderEvent
import com.dontsitdown.data.model.ReminderEventType
import com.dontsitdown.data.model.TaskCompletion
import com.dontsitdown.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SummaryCalculatorTest {
    private val monday = LocalDate.of(2026, 10, 12)
    private fun at(day: Long, hour: Int) = TimeUtils.at(monday.plusDays(day), hour, 0)

    @Test
    fun couchVersusYou() {
        val completions = listOf(0L, 1L, 3L, 4L).map {
            TaskCompletion(taskId = "t", taskName = "dishes", date = at(it, 19), minutes = 6f, chainId = "c$it")
        }
        val events = listOf(
            ReminderEvent(timestamp = at(2, 19), type = ReminderEventType.FIRED), // ignored -> couch
            ReminderEvent(timestamp = at(3, 19), type = ReminderEventType.FIRED), // did it anyway -> you
        )
        val score = SummaryCalculator.score(completions, events, monday, monday.plusDays(7), monday.plusDays(6))
        assertEquals(4, score.you)
        assertEquals(1, score.couch)
        assertEquals(listOf(monday.plusDays(2)), score.skipped)
    }

    @Test
    fun todayOnlyCountsForTheCouchOnceRefused() {
        val today = monday.plusDays(1)
        val fired = listOf(ReminderEvent(timestamp = at(1, 19), type = ReminderEventType.FIRED))
        assertEquals(0, SummaryCalculator.score(emptyList(), fired, monday, monday.plusDays(7), today).couch)
        val refused = fired + ReminderEvent(timestamp = at(1, 20), type = ReminderEventType.REFUSED)
        assertEquals(1, SummaryCalculator.score(emptyList(), refused, monday, monday.plusDays(7), today).couch)
    }
}
