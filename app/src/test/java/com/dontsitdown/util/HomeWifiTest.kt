package com.dontsitdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeWifiTest {
    @Test
    fun decides() {
        assertEquals(false, HomeWifi.decide(onWifi = false, ssid = "flat", home = "flat"))
        assertEquals(true, HomeWifi.decide(onWifi = true, ssid = "flat", home = "flat"))
        assertEquals(false, HomeWifi.decide(onWifi = true, ssid = "cafe", home = "flat"))
        // Can't read the name (no permission): any Wi-Fi counts.
        assertEquals(true, HomeWifi.decide(onWifi = true, ssid = null, home = "flat"))
        // No home network picked: any Wi-Fi counts.
        assertEquals(true, HomeWifi.decide(onWifi = true, ssid = "cafe", home = null))
    }

    @Test
    fun cleansNames() {
        assertEquals("flat 5G", HomeWifi.clean("\"flat 5G\""))
        assertNull(HomeWifi.clean("<unknown ssid>"))
        assertNull(HomeWifi.clean("\"\""))
    }
}
