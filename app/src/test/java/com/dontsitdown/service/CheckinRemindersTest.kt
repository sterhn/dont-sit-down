package com.dontsitdown.service

import com.dontsitdown.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CheckinRemindersTest {
    private val day = LocalDate.of(2026, 11, 4)

    @Test
    fun laterTodayOrTomorrow() {
        val morning = 9 * 60
        assertEquals(TimeUtils.at(day, 9, 0), CheckinReminders.nextAt(morning, TimeUtils.at(day, 7, 30)))
        assertEquals(TimeUtils.at(day.plusDays(1), 9, 0), CheckinReminders.nextAt(morning, TimeUtils.at(day, 9, 0)))
        assertEquals(TimeUtils.at(day.plusDays(1), 22, 15), CheckinReminders.nextAt(22 * 60 + 15, TimeUtils.at(day, 23, 0)))
    }
}
