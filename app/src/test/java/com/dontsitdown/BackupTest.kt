package com.dontsitdown

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.CheckinJson
import com.dontsitdown.checkin.Day
import com.dontsitdown.checkin.Morning
import com.dontsitdown.data.BackupJson
import com.dontsitdown.data.DefaultTasks
import com.dontsitdown.data.model.ReminderEventType
import com.dontsitdown.util.ThemeMode
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A full backup round-trips everything, and the two import paths don't get confused. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestApp::class)
class BackupTest {
    private val app get() = ApplicationProvider.getApplicationContext<DontSitDownApp>()

    @Test
    fun roundTrip() = runBlocking {
        val tasks = DefaultTasks.create(System.currentTimeMillis())
        app.repository.saveAll(tasks)
        app.repository.recordCompletion(tasks.first().id, 6f, System.currentTimeMillis(), "c1")
        app.repository.logEvent(ReminderEventType.FIRED)
        app.settings.update { it.copy(onboarded = true, theme = ThemeMode.LIGHT, homeHour = 18, awayDays = setOf(TimeUtils.today().toEpochDay())) }
        app.checkin.replaceAll(CheckinData(days = mapOf("2026-11-01" to Day(Morning(mood = 7, intention = "keep this")))))

        val text = BackupJson.encode(BackupJson.create(app))

        // Wipe, then restore.
        val db = app.db
        db.taskDao().clearCompletions(); db.taskDao().clearTasks(); db.chainResultDao().clearEvents()
        app.settings.update { it.copy(theme = ThemeMode.DARK, homeHour = 19, awayDays = emptySet()) }
        app.checkin.replaceAll(CheckinData())

        val b = BackupJson.decode(text)
        assertNotNull(b)
        BackupJson.restore(app, b!!)

        assertEquals(tasks.size, app.repository.allTasks().size)
        assertEquals(1, db.taskDao().allCompletions().size)
        assertEquals(1, db.chainResultDao().allEvents().size)
        assertEquals(ThemeMode.LIGHT, app.settings.current.theme)
        assertEquals(18, app.settings.current.homeHour)
        assertTrue(app.settings.current.isAway(TimeUtils.today()))
        assertEquals("keep this", app.checkin.current.days["2026-11-01"]!!.morning!!.intention)
    }

    @Test
    fun importPathsDontMixUp() = runBlocking {
        app.checkin.replaceAll(CheckinData(days = mapOf("2026-11-01" to Day(Morning(mood = 3)))))
        val backup = BackupJson.encode(BackupJson.create(app))
        val checkinOnly = app.checkin.export()
        // A check-in file is not a full backup...
        assertNull(BackupJson.decode(checkinOnly))
        // ...but the check-in importer can take a full backup's check-in half.
        assertEquals(3, CheckinJson.decode(backup)!!.days["2026-11-01"]!!.morning!!.mood)
    }
}
