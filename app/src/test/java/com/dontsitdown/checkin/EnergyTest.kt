package com.dontsitdown.checkin

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EnergyTest {
    private val today = LocalDate.of(2026, 11, 4)
    private fun on(m: Morning? = null, e: Evening? = null) = CheckinData(days = mapOf(today.toString() to Day(m, e)))

    @Test
    fun readsTheDay() {
        assertEquals(Energy.OK, Checkin.energy(CheckinData(), today))
        assertEquals(Energy.OK, Checkin.energy(on(Morning(sleep = 7, mood = 6, body = 5)), today))
        assertEquals(Energy.LOW, Checkin.energy(on(Morning(sleep = 4, mood = 6)), today))
        assertEquals(Energy.ROUGH, Checkin.energy(on(Morning(sleep = 8, mood = 2)), today))
        // The evening's mood replaces the morning's once it's in.
        assertEquals(Energy.OK, Checkin.energy(on(Morning(sleep = 7, mood = 2), Evening(mood = 7)), today))
    }

    @Test
    fun capsTheChain() {
        assertEquals(8, Checkin.chainCap(Energy.ROUGH, 20))
        assertEquals(12, Checkin.chainCap(Energy.LOW, 20))
        assertEquals(20, Checkin.chainCap(Energy.OK, 20))
        assertEquals(10, Checkin.chainCap(Energy.LOW, 10))
    }
}
