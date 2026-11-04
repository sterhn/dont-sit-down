package com.dontsitdown.checkin

import org.junit.Test
import java.io.File

/** Writes an export for the web-app compatibility check (only when asked to). */
class ExportFixtureTest {
    @Test
    fun writeFixture() {
        val out = System.getProperty("export.fixture") ?: return
        val d = CheckinData(
            goals = CheckinData.defaultGoals() + Goal("x", "Call grandma", "month", 2),
            log = listOf(LogEntry("a", "new-food", "2026-11-01", "khachapuri", "ordered")),
            days = mapOf("2026-11-03" to Day(Morning(sleep = 6, mood = null, body = 5, intention = "ship"), Evening(mood = 7, boundaries = mapOf("22:00 cutoff" to "held", "no solo takeout" to null), gym = "rest", happened = "a lot"))),
        )
        File(out).writeText(CheckinJson.encode(d))
    }
}
