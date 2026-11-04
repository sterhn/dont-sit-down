package com.dontsitdown.checkin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CheckinTest {
    private val today = LocalDate.of(2026, 11, 4) // a wednesday

    @Test
    fun blankCheckinDoesNotCount() {
        val d = CheckinData(days = mapOf(today.toString() to Day(morning = Morning(intention = "  "), evening = Evening(boundaries = mapOf("x" to null)))))
        assertTrue(!Checkin.dayFilled(d, today))
        val e = d.copy(days = mapOf(today.toString() to Day(evening = Evening(boundaries = mapOf("x" to "held")))))
        assertTrue(Checkin.dayFilled(e, today))
    }

    @Test
    fun goalPeriods() {
        val g = Goal("g", "Go somewhere new", "week", 1, "place")
        val log = listOf(
            LogEntry("1", "g", "2026-11-02", "park"), // this week (mon nov 2)
            LogEntry("2", "g", "2026-11-01", "museum"), // last week (sunday)
        )
        val d = CheckinData(goals = listOf(g), log = log)
        assertEquals(1, Checkin.periodCount(d, g, Checkin.periodOf(g, today)))
        assertTrue(Checkin.pendingGoals(d, today).isEmpty())
        assertEquals("5 days left", Checkin.periodLeft(g, today))
        assertEquals("last day", Checkin.periodLeft(g, LocalDate.of(2026, 11, 8)))
        val book = Goal("b", "Read a book", "month", 1, "book")
        assertEquals("2026-11", Checkin.periodOf(book, today))
        assertEquals("27 days left", Checkin.periodLeft(book, today))
    }

    @Test
    fun importsTheWebAppsExport() {
        val web = """
            {
              "boundaries": ["22:00 cutoff", "no doomscrolling"],
              "goals": [
                {"id": "new-place", "title": "Go somewhere new", "period": "week", "target": 1, "icon": "place", "prompt": "Where did you go?"},
                {"id": "x1", "title": "Call grandma", "period": "month", "target": 2, "icon": "weird"},
                {"title": "no id, dropped"}
              ],
              "log": [
                {"id": "a", "goal": "new-place", "date": "2026-10-30", "text": "the old harbour"},
                {"goal": "x1", "date": "not a date", "text": "dropped"}
              ],
              "days": {
                "2026-11-03": {
                  "morning": {"sleep": 7, "mood": 6, "body": null, "intention": "ship it", "savedAt": "2026-11-03T08:00:00.000Z"},
                  "evening": {"mood": 8.0, "body": 5, "boundaries": {"22:00 cutoff": "held", "no doomscrolling": "maybe"}, "gym": "went", "happened": "a lot", "honest": "", "spent": "12"}
                },
                "garbage": {"morning": {"mood": 3}}
              }
            }
        """.trimIndent()
        val d = CheckinJson.decode(web)
        assertNotNull(d)
        d!!
        assertEquals(listOf("22:00 cutoff", "no doomscrolling"), d.boundaries)
        assertEquals(2, d.goals.size)
        assertEquals("star", d.goals[1].icon)
        assertEquals(2, d.goals[1].target)
        assertEquals(1, d.log.size)
        assertEquals(1, d.days.size)
        val day = d.days["2026-11-03"]!!
        assertEquals("ship it", day.morning!!.intention)
        assertNull(day.morning!!.body)
        assertEquals(8, day.evening!!.mood)
        assertEquals("held", day.evening!!.boundaries["22:00 cutoff"])
        assertNull(day.evening!!.boundaries["no doomscrolling"])
        assertEquals("went", day.evening!!.gym)

        // And it survives a round trip through our own file.
        assertEquals(d, CheckinJson.decode(CheckinJson.encode(d)))
    }

    @Test
    fun rejectsThingsThatArentCheckins() {
        assertNull(CheckinJson.decode("[]"))
        assertNull(CheckinJson.decode("{\"tasks\": []}"))
        assertNull(CheckinJson.decode("not json"))
        // No goals in an old export: defaults stay.
        assertEquals(3, CheckinJson.decode("{\"days\": {}}")!!.goals.size)
    }

    @Test
    fun storyAndMarkdown() {
        val d = CheckinData(days = mapOf(today.toString() to Day(
            morning = Morning(sleep = 4, mood = 5, body = 6, intention = "finish the deck"),
            evening = Evening(mood = 7, body = 6, boundaries = mapOf("a" to "held", "b" to "held", "c" to "slipped"), gym = "rest"),
        )))
        assertEquals(listOf("sleep 4", "body 6", "mood 5 → 7"), Checkin.scoreBits(d, today))
        val lines = Checkin.storyLines(d, today, full = true).toMap()
        assertEquals("held a and b; slipped on c", lines["boundaries"])
        assertEquals("rest day", lines["gym"])
        val md = Checkin.dayMarkdown(d, today, chores = listOf("dishes" to 6.2f))
        assertTrue(md, md.startsWith("# wednesday 4 november"))
        assertTrue(md, md.contains("- dishes: 6 min"))
    }

    @Test
    fun faces() {
        assertEquals(Face.COFFEE, Checkin.faceFor(null, Face.COFFEE))
        assertEquals(Face.SLEEPY, Checkin.faceFor(null, Face.COFFEE, tired = true))
        assertEquals(Face.ROUGH, Checkin.faceFor(1))
        assertEquals(Face.LOW, Checkin.faceFor(4))
        assertEquals(Face.NEUTRAL, Checkin.faceFor(5))
        assertEquals(Face.HAPPY, Checkin.faceFor(8))
        assertEquals(Face.GREAT, Checkin.faceFor(10))
        assertEquals("radiant", Checkin.moodWord(10))
    }
}
