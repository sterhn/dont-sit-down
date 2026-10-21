package com.dontsitdown

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dontsitdown.chain.Phase
import com.dontsitdown.chain.PlannedTask
import com.dontsitdown.chain.SessionMode
import com.dontsitdown.chain.SessionState
import com.dontsitdown.chain.TaskRun
import com.dontsitdown.data.DefaultTasks
import com.dontsitdown.data.model.ChainResult
import com.dontsitdown.data.model.ReminderEventType
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskResult
import com.dontsitdown.service.FullScreenReminderActivity
import com.dontsitdown.ui.AppNav
import com.dontsitdown.ui.chain.ChainViewModel
import com.dontsitdown.ui.chain.DoneScreen
import com.dontsitdown.ui.chain.TimerScreen
import com.dontsitdown.ui.onboarding.OnboardingScreen
import com.dontsitdown.ui.settings.SettingsScreen
import com.dontsitdown.ui.summary.WeeklySummaryScreen
import com.dontsitdown.ui.tasks.TaskDetailScreen
import com.dontsitdown.ui.tasks.TaskListScreen
import com.dontsitdown.ui.theme.AppTheme
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real screens on the JVM (Robolectric native graphics), drives a full chain
 * through the UI, and saves screenshots so the look can be checked without a device.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi", application = TestApp::class)
class ScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<DontSitDownApp>()
    private val now = System.currentTimeMillis()
    private val outDir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }

    /** Software-draws the window. (captureToImage waits on a hardware redraw Robolectric never does.) */
    private fun shot(name: String, view: View = compose.activity.window.decorView) {
        compose.waitForIdle()
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitForText(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasText(text, substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun daysAgo(d: Double) = now - (d * TimeUtils.DAY).toLong()

    /** Defaults with a believable history: some overdue, some fresh, real times learned. */
    private fun seed(): List<Task> = runBlocking {
        val tasks = DefaultTasks.create(now).map {
            when (it.name) {
                "dishes" -> it.copy(lastDone = daysAgo(2.2), actualTimes = listOf(6f, 7f, 5.5f, 6.5f, 6f))
                "vacuum" -> it.copy(lastDone = daysAgo(9.0), actualTimes = listOf(10f, 11f))
                "wipe counters" -> it.copy(lastDone = daysAgo(1.4), actualTimes = listOf(3f, 3.5f))
                "take out trash" -> it.copy(lastDone = daysAgo(4.0))
                "water plants" -> it.copy(lastDone = daysAgo(2.0))
                "change sheets" -> it.copy(lastDone = daysAgo(3.0))
                "deep clean fridge" -> it.copy(lastDone = daysAgo(10.0))
                "mop floors" -> it.copy(lastDone = daysAgo(5.0))
                "clean shower/tub" -> it.copy(lastDone = daysAgo(6.0))
                else -> it
            }
        }
        app.repository.saveAll(tasks)
        app.settings.update { it.copy(onboarded = true) }
        tasks
    }

    @Test
    fun onboarding() {
        compose.setContent { AppTheme { OnboardingScreen(onDone = {}) } }
        shot("01-welcome")
        compose.onNodeWithText("OK").performClick()
        shot("02-pick-tasks")
        compose.onNodeWithText("NEXT").performClick()
        shot("03-home-time")
    }

    @Test
    fun fullChainThroughTheUi() {
        seed()
        compose.setContent { AppTheme { AppNav(onboarded = true, openTarget = null, onTargetConsumed = {}) } }
        waitForText("START")
        shot("04-chain-ready")
        val planned = app.session.state.value!!
        assertTrue("chain of ${planned.plan.size}", planned.plan.size in 2..4)
        assertTrue(planned.plannedMinutes <= 20.5f)

        compose.onNodeWithText("START").performClick()
        waitForText("dreading")
        shot("05-dread")
        compose.onNodeWithText("\"ugh\"").performClick()

        repeat(planned.plan.size) { i ->
            assertEquals(Phase.ACTIVE, app.session.state.value!!.phase)
            compose.onNodeWithText("DONE").performClick()
            if (i < planned.plan.size - 1) {
                waitForText("KEEP MOVING")
                if (i == 0) shot("07-between")
                compose.onNodeWithText("KEEP MOVING").performClick()
            }
        }
        waitForText("SIT DOWN")
        val done = app.session.state.value!!
        assertEquals(Phase.COMPLETE, done.phase)
        assertEquals(planned.plan.size, done.runs.size)
        assertEquals(planned.plan.size, done.combo)
        assertEquals(2, done.dread)

        compose.onNodeWithText("SIT DOWN").performClick()
        waitForText("now sit down.")
        shot("09-resting")

        runBlocking {
            val results = app.repository.allChainResults()
            assertEquals(1, results.size)
            assertEquals(planned.plan.size, results.single().tasks.size)
            val first = app.repository.task(planned.plan.first().task.id)!!
            assertNotNull(first.lastDone)
            assertEquals(TimeUtils.dueInDays(first)!! > 0 || first.frequency.intervalDays == null, true)
        }
    }

    @Test
    fun timerAndDoneScreens() {
        val tasks = seed()
        val dishes = tasks.first { it.name == "dishes" }
        val vacuum = tasks.first { it.name == "vacuum" }
        val counters = tasks.first { it.name == "wipe counters" }
        val min = 60_000L
        val start = now - 30 * min
        val runs = listOf(
            TaskRun(dishes.id, dishes.name, 6.2f, start, start + 6 * min),
            TaskRun(vacuum.id, vacuum.name, 10.5f, start + 6 * min + 20_000, start + 16 * min),
            TaskRun(counters.id, counters.name, 3.2f, start + 16 * min + 30_000, start + 19 * min),
        )
        runBlocking {
            // A little history so the dread averages have something to say.
            listOf(3, 3, 2, 3).forEachIndexed { i, d ->
                app.repository.saveChainResult(
                    ChainResult(date = daysAgo(i + 1.0), tasks = listOf(TaskResult(dishes.id, "dishes", 6f, 6f, d)), totalActual = 14f + i, totalEstimate = 17f, totalDreadBefore = d)
                )
            }
        }
        val plan = listOf(dishes, vacuum, counters).map { PlannedTask(it, it.effectiveMinutes) }

        var state by mutableStateOf(
            SessionState(mode = SessionMode.CHAIN, plan = plan, phase = Phase.ACTIVE, index = 1, currentStartedAt = now - (8 * min + 12_000), runs = runs.take(1), dread = 3, quip = "clock's running.")
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AppTheme {
                val s = state
                if (s.phase == Phase.ACTIVE) TimerScreen(s, onDone = {}, onBail = {})
                else DoneScreen(s, ChainViewModel(app))
            }
        }
        compose.mainClock.advanceTimeBy(600)
        shot("06-timer")

        state = SessionState(mode = SessionMode.CHAIN, plan = plan, phase = Phase.COMPLETE, index = 2, runs = runs, dread = 3, quip = "told you. 19 minutes.")
        compose.mainClock.advanceTimeBy(2_000)
        shot("08-done")
    }

    @Test
    fun takeover() {
        val tasks = seed()
        app.settings.pendingChain = listOf("dishes", "vacuum", "wipe counters").map { n -> tasks.first { it.name == n }.id }
        ActivityScenario.launch(FullScreenReminderActivity::class.java).use { scenario ->
            waitForText("START")
            var root: View? = null
            scenario.onActivity { root = it.window.decorView }
            shot("10-takeover", root!!)
        }
    }

    @Test
    fun taskScreensWeekAndSettings() {
        val tasks = seed()
        val dishes = tasks.first { it.name == "dishes" }
        val weekStart = TimeUtils.weekStart(TimeUtils.today())
        runBlocking {
            val days = (0 until TimeUtils.today().dayOfWeek.value).map { weekStart.plusDays(it.toLong()) }
            days.forEachIndexed { i, d ->
                if (i == 2) {
                    app.repository.logEvent(ReminderEventType.FIRED, TimeUtils.at(d, 19, 0))
                    return@forEachIndexed
                }
                val t = TimeUtils.at(d, 19, 5)
                app.repository.recordCompletion(dishes.id, 5f + i, t, "c$i")
                app.repository.recordCompletion(tasks.first { it.name == "wipe counters" }.id, 3f, t + 7 * 60_000, "c$i")
                if (i == 0) app.repository.recordCompletion(tasks.first { it.name == "change sheets" }.id, 14f, t + 10 * 60_000, "c$i")
                app.repository.saveChainResult(
                    ChainResult(date = t, tasks = emptyList(), totalActual = 8f + i, totalEstimate = 12f, totalDreadBefore = if (i % 2 == 0) 3 else 2)
                        .copy(tasks = listOf(TaskResult(dishes.id, "dishes", 6f, 5f + i, 2), TaskResult("x", "wipe counters", 3f, 3f, 2)))
                )
            }
            // Older dishes history for the "actually how long" card.
            listOf(4.2f, 11f, 6f, 7.5f, 5f, 6.4f).forEachIndexed { i, m -> app.repository.recordCompletion(dishes.id, m, daysAgo(10.0 + i), null) }
        }

        var screen by mutableStateOf("list")
        compose.setContent {
            AppTheme {
                when (screen) {
                    "list" -> TaskListScreen(onOpen = {}, onAdd = {})
                    "detail" -> TaskDetailScreen(dishes.id, onBack = {}, onEdit = {}, onStart = {})
                    "week" -> WeeklySummaryScreen()
                    else -> SettingsScreen()
                }
            }
        }
        waitForText("dishes")
        shot("11-tasks")
        screen = "detail"
        waitForText("actually, how long")
        shot("12-task-detail")
        screen = "week"
        waitForText("the couch")
        shot("13-week")
        screen = "settings"
        waitForText("home time")
        shot("14-settings")
    }
}
