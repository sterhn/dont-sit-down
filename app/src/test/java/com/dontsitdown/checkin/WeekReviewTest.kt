package com.dontsitdown.checkin

import com.dontsitdown.chain.SummaryCalculator
import com.dontsitdown.service.WeeklySummaryWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekReviewTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private val sunday = monday.plusDays(6)

    @Test
    fun saysHowTheWeekFelt() {
        val moods = listOf(3, 6, 7, 9, 6, 6, 5)
        val d = CheckinData(
            days = moods.mapIndexed { i, m -> monday.plusDays(i.toLong()).toString() to Day(Morning(sleep = 6, mood = m)) }.toMap(),
            log = listOf(LogEntry("1", "new-food", "2026-11-05", "khachapuri")),
        )
        val lines = Checkin.weekReview(d, monday, sunday)
        assertTrue(lines.toString(), lines[0].startsWith("how it felt: mostly fine"))
        assertEquals("sleep: 6.0 on average", lines[1])
        assertEquals("best day: thursday · hardest: monday", lines[2])
        assertEquals("adventures: khachapuri", lines[3])
        assertTrue(lines.none { "streak" in it || "days written" in it })
    }

    @Test
    fun bothHalvesInOneNote() {
        val empty = SummaryCalculator.build(monday, emptyList(), emptyList(), emptyList(), emptyList(), emptyMap(), sunday)
        assertNull(WeeklySummaryWorker.reviewText(emptyList(), empty))
        assertEquals("how it felt: mostly good (mood 7.0)", WeeklySummaryWorker.reviewText(listOf("how it felt: mostly good (mood 7.0)"), empty))
    }
}
