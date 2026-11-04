package com.dontsitdown

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.Day
import com.dontsitdown.checkin.Energy
import com.dontsitdown.checkin.Face
import com.dontsitdown.checkin.Morning
import com.dontsitdown.data.DefaultTasks
import com.dontsitdown.service.CheckinReminders
import com.dontsitdown.util.TimeUtils
import com.dontsitdown.widget.CheckinWidget
import com.dontsitdown.widget.WidgetState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi", application = TestApp::class)
class WidgetAndRatingTest {
    private val app get() = ApplicationProvider.getApplicationContext<DontSitDownApp>()
    private val outDir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
    private val today = TimeUtils.today()

    private fun snap(v: View, w: Int, h: Int, name: String) {
        val wSpec = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        v.measure(wSpec, hSpec)
        v.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply { drawColor(0xFFDDDDDD.toInt()) }
        v.draw(Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun widgetState() {
        val morning = LocalDateTime.of(today, java.time.LocalTime.of(8, 0))
        val empty = WidgetState.of(CheckinData(), morning, away = false, choresDone = 0, choreMinutes = 0, chainMinutes = 19)
        assertEquals(Face.COFFEE, empty.face)
        assertEquals("Check in →", empty.button)
        assertTrue(empty.ticket, empty.ticket.contains("19 MIN"))
        val d = CheckinData(days = mapOf(Checkin.key(today) to Day(Morning(mood = 9))))
        val done = WidgetState.of(d, morning, away = false, choresDone = 2, choreMinutes = 9, chainMinutes = null)
        assertEquals(Face.GREAT, done.face)
        assertEquals("Mood 9 · great", done.line)
        assertTrue(done.ticket.contains("2 DONE"))
        assertEquals(Face.AWAY, WidgetState.of(CheckinData(), morning, away = true, 0, 0, null).face)
    }

    @Test
    fun widgetRenders() = runBlocking {
        app.repository.saveAll(DefaultTasks.create(System.currentTimeMillis()))
        app.checkin.replaceAll(CheckinData(days = mapOf(Checkin.key(today) to Day(Morning(mood = 7, sleep = 6)))))
        val s = CheckinWidget.state(app, LocalDateTime.of(today, java.time.LocalTime.of(9, 0)))
        val parent = FrameLayout(app)
        val v = CheckinWidget.views(app, s).apply(app, parent)
        snap(v, 1000, 460, "40-widget")
        assertEquals("Mood 7 · good", v.findViewById<TextView>(R.id.w_line).text.toString())
    }

    @Test
    fun ratingFromTheNotification() {
        app.checkin.replaceAll(CheckinData())
        CheckinReminders.rate(app, CheckinReminders.MORNING, 3, LocalDateTime.of(today, java.time.LocalTime.of(9, 5)))
        assertEquals(3, app.checkin.current.day(Checkin.key(today)).morning!!.mood)
        // ...which makes it a rough day: the chain gets smaller and spite stands down.
        assertEquals(Energy.ROUGH, app.energyToday())
        app.settings.update { it.copy(spite = true, chainMaxMinutes = 20) }
        assertTrue(!app.spiteToday())
        val chain = runBlocking { app.repository.saveAll(DefaultTasks.create(System.currentTimeMillis())); app.generateChain() }
        assertTrue("chain ${chain.sumOf { it.effectiveMinutes.toDouble() }}", chain.sumOf { it.effectiveMinutes.toDouble() } <= 8.5)
        // After midnight the evening reminder still means last night.
        CheckinReminders.rate(app, CheckinReminders.EVENING, 8, LocalDateTime.of(today, java.time.LocalTime.of(1, 0)))
        assertEquals(8, app.checkin.current.day(Checkin.key(today.minusDays(1))).evening!!.mood)
    }

    @Test
    fun notificationLayoutRenders() {
        val v = android.view.LayoutInflater.from(app).inflate(R.layout.notif_rate, FrameLayout(app), false)
        v.findViewById<TextView>(R.id.rate_text).text = "how are you, honestly? tap a number, or open it for more."
        snap(v, 1000, 300, "41-notification-row")
    }
}
