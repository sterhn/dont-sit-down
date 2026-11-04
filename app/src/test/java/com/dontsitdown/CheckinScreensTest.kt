package com.dontsitdown

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.Day
import com.dontsitdown.checkin.Evening
import com.dontsitdown.checkin.LogEntry
import com.dontsitdown.checkin.Morning
import com.dontsitdown.data.DefaultTasks
import com.dontsitdown.ui.AppNav
import com.dontsitdown.ui.checkin.AddGoalScreen
import com.dontsitdown.ui.checkin.GoalDetailScreen
import com.dontsitdown.ui.checkin.GoalsScreen
import com.dontsitdown.ui.checkin.JournalScreen
import com.dontsitdown.ui.checkin.StatsScreen
import com.dontsitdown.ui.checkin.TodayScreen
import com.dontsitdown.ui.checkin.WeekStoryScreen
import com.dontsitdown.ui.chain.ChainScreen
import com.dontsitdown.ui.components.LocalToaster
import com.dontsitdown.ui.components.Toaster
import com.dontsitdown.ui.settings.SettingsScreen
import com.dontsitdown.ui.theme.AppTheme
import com.dontsitdown.util.ThemeMode
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalTime

/** The check-in half and the "not home" day, rendered for real and saved as screenshots. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi", application = TestApp::class)
class CheckinScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<DontSitDownApp>()
    private val outDir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
    private val today = TimeUtils.today()

    private fun shot(name: String) {
        compose.mainClock.advanceTimeBy(2_500)
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitForText(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasText(text, substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty()
    }

    /** Three weeks of believable check-ins, a few goals logged, and some chores done today. */
    private fun seed() {
        val notes = listOf(
            "finish the deck" to "long day, deck's done. ate standing up.",
            "be kind to myself" to "slept in, walked to the river",
            "call mum" to "called mum. she's fine, I'm fine.",
            "gym, then groceries" to "",
            "" to "nothing much. rain.",
        )
        val days = (1..20).filter { it != 6 && it != 13 }.associate { i ->
            val d = today.minusDays(i.toLong())
            val (intent, happened) = notes[i % notes.size]
            Checkin.key(d) to Day(
                morning = Morning(sleep = 4 + (i * 3) % 6, mood = 3 + (i * 5) % 7, body = 5 + i % 4, intention = intent, savedAt = "x"),
                evening = Evening(
                    mood = 4 + (i * 7) % 6, body = 5 + (i * 2) % 5,
                    boundaries = mapOf("22:00 cutoff" to if (i % 3 == 0) "slipped" else "held", "no solo takeout" to if (i % 4 == 0) "slipped" else "held", "bedtime before midnight" to null),
                    gym = listOf("went", "rest", "skipped", null)[i % 4], happened = happened, honest = if (i % 5 == 2) "I miss having a routine" else "", spent = if (i % 2 == 0) "${12 + i}" else "",
                    savedAt = "x",
                ),
            )
        } + (Checkin.key(today) to Day(morning = Morning(sleep = 6, mood = 7, body = 6, intention = "ship the app, then rest", savedAt = "x")))
        val log = listOf(
            LogEntry("1", "new-place", today.minusDays(9).toString(), "the old harbour market"),
            LogEntry("2", "new-place", today.minusDays(16).toString(), "that tiny bookshop"),
            LogEntry("3", "new-food", today.minusDays(1).toString(), "khachapuri", "ordered"),
            LogEntry("4", "new-food", today.minusDays(10).toString(), "miso aubergine", "cooked"),
            LogEntry("5", "book", today.minusDays(25).toString(), "piranesi"),
        )
        app.checkin.replaceAll(CheckinData(log = log, days = days).let { d ->
            d.copy(goals = d.goals.map { if (it.id == "book") it.copy(current = "the left hand of darkness") else it })
        })
        runBlocking {
            val tasks = DefaultTasks.create(System.currentTimeMillis())
            app.repository.saveAll(tasks)
            val t = TimeUtils.at(today, 8, 10)
            app.repository.recordCompletion(tasks.first { it.name == "dishes" }.id, 6.2f, t, "c")
            app.repository.recordCompletion(tasks.first { it.name == "wipe counters" }.id, 3.1f, t + 7 * 60_000, "c")
        }
        app.settings.update { it.copy(onboarded = true) }
    }

    @androidx.compose.runtime.Composable
    private fun Wrap(content: @androidx.compose.runtime.Composable () -> Unit) {
        AppTheme(app.settings.current.theme) {
            androidx.compose.runtime.CompositionLocalProvider(LocalToaster provides Toaster()) { content() }
        }
    }

    /** The check-in screens in their own world, like AppNav puts them. */
    @androidx.compose.runtime.Composable
    private fun Notebook(dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) =
        Wrap { com.dontsitdown.ui.theme.CheckinWorld(dark = dark) { content() } }

    @Test
    fun todayMorningAndEvening() {
        seed()
        compose.setContent { Notebook { TodayScreen({}, {}, {}, now = { LocalTime.of(8, 30) }) } }
        waitForText("Good morning")
        shot("20-today-morning")
        compose.onNodeWithText("Evening").performClick()
        waitForText("Good evening")
        shot("21-today-evening")

        // Tapping "held" records it, and the character reacts.
        compose.onAllNodesWithText("Held").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("held", app.checkin.current.day(Checkin.key(today)).evening!!.boundaries["22:00 cutoff"])
    }

    @Test
    fun goalsAndStats() {
        seed()
        var screen by mutableStateOf("goals")
        compose.setContent {
            Wrap {
                if (screen == "chores") com.dontsitdown.ui.summary.WeeklySummaryScreen()
                else com.dontsitdown.ui.theme.CheckinWorld(dark = false) { when (screen) {
                    "goals" -> GoalsScreen({}, {})
                    "goal" -> GoalDetailScreen("new-food", onBack = {})
                    "add" -> AddGoalScreen(onBack = {})
                    "stats" -> StatsScreen({}, {})
                    "journal" -> JournalScreen(onBack = {})
                    else -> WeekStoryScreen(onBack = {})
                } }
            }
        }
        shot("22-goals")
        screen = "goal"; shot("23-goal-detail")
        screen = "add"; shot("24-add-goal")
        screen = "stats"; shot("25-stats")
        screen = "chores"; waitForText("the couch"); shot("26-stats-chores")
        screen = "journal"; shot("27-journal")
        screen = "story"; shot("28-week-story")
    }

    @Test
    fun notHomeToday() {
        seed()
        compose.setContent { Wrap { ChainScreen(onJustOne = {}) } }
        waitForText("START")
        compose.onNodeWithText("not home today").performClick()
        waitForText("just today")
        compose.onNodeWithText("JUST TODAY").performClick()
        waitForText("not home today.")
        assertTrue(app.settings.current.isAway(today))
        shot("29-away")
        compose.onNodeWithText("i'm home after all").performClick()
        waitForText("START")
        assertFalse(app.settings.current.isAway(today))
    }

    @Test
    fun darkNotebookAndSettings() {
        seed()
        app.settings.update { it.copy(theme = ThemeMode.LIGHT, morningNudge = 9 * 60) }
        var screen by mutableStateOf("today")
        compose.setContent {
            Wrap {
                when (screen) {
                    "today" -> com.dontsitdown.ui.theme.CheckinWorld(dark = true) { TodayScreen({}, {}, {}, now = { LocalTime.of(8, 30) }) }
                    "goals" -> com.dontsitdown.ui.theme.CheckinWorld(dark = true) { GoalsScreen({}, {}) }
                    else -> SettingsScreen()
                }
            }
        }
        shot("30-dark-today")
        screen = "goals"; shot("31-dark-goals")
        screen = "settings"; shot("32-settings")
    }

    @Test
    fun freshInstallOffersToImportFromTheBrowser() {
        app.settings.update { it.copy(onboarded = true) }
        val web = """{"boundaries":["no phone in bed"],"goals":[],"log":[],"days":{"${today.minusDays(1)}":{"morning":{"sleep":7,"mood":6,"body":5,"intention":"from the browser"},"evening":null}}}"""
        val cm = app.getSystemService(android.content.ClipboardManager::class.java)
        cm.setPrimaryClip(android.content.ClipData.newPlainText("checkin", web))
        compose.setContent { Notebook { TodayScreen({}, {}, {}, now = { LocalTime.of(8, 30) }) } }
        waitForText("Coming from the browser app?")
        shot("34-import-card")
        compose.onNodeWithText("Paste").performClick()
        waitForText("import this?")
        compose.onNodeWithText("Import").performClick()
        compose.waitForIdle()
        val d = app.checkin.current
        assertEquals(listOf("no phone in bed"), d.boundaries)
        assertEquals("from the browser", d.day(Checkin.key(today.minusDays(1))).morning!!.intention)
        // Once there's history, the card goes away.
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Coming from the browser app?")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun wholeAppStartsOnToday() {
        seed()
        compose.setContent { AppTheme { AppNav(onboarded = true, openTarget = null, onTargetConsumed = {}) } }
        waitForText("morning")
        shot("33-app-today")
    }
}
