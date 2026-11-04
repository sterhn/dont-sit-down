package com.dontsitdown.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dontsitdown.MainActivity
import com.dontsitdown.app
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.util.NotificationHelper
import com.dontsitdown.util.TimeUtils

/**
 * Two optional, quiet reminders for the check-in: a morning one and an evening one.
 * Inexact on purpose (a few minutes late is fine), and silent if you've already checked in.
 */
object CheckinReminders {
    const val MORNING = "morning"
    const val EVENING = "evening"
    private const val REQ_MORNING = 200
    private const val REQ_EVENING = 201

    fun scheduleAll(context: Context) {
        val s = context.app.settings.current
        schedule(context, MORNING, s.morningNudge)
        schedule(context, EVENING, s.eveningNudge)
    }

    /** [minuteOfDay] < 0 cancels. */
    fun schedule(context: Context, which: String, minuteOfDay: Int, now: Long = System.currentTimeMillis()) {
        val pi = intent(context, which)
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pi)
        if (minuteOfDay < 0) return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt(minuteOfDay, now), pi)
    }

    fun nextAt(minuteOfDay: Int, now: Long): Long {
        val today = TimeUtils.toDate(now)
        val t = TimeUtils.at(today, minuteOfDay / 60, minuteOfDay % 60)
        return if (t > now + 1_000) t else TimeUtils.at(today.plusDays(1), minuteOfDay / 60, minuteOfDay % 60)
    }

    fun fire(context: Context, which: String) {
        val app = context.app
        val s = app.settings.current
        schedule(context, which, if (which == MORNING) s.morningNudge else s.eveningNudge)
        val day = app.checkin.current.day(Checkin.key(TimeUtils.today()))
        val (title, text) = if (which == MORNING) {
            if (Checkin.morningFilled(day.morning)) return
            "morning check-in" to "how are you, honestly? tap a number, or open it for more."
        } else {
            if (Checkin.eveningFilled(day.evening)) return
            "close the day" to "how was it? tap a number, or open it for the rest."
        }
        showRatable(context, which, title, text)
    }

    /** The reminder with a 1–10 row. Tapping a number records the mood right there. */
    fun showRatable(context: Context, which: String, title: String, text: String) {
        val views = android.widget.RemoteViews(context.packageName, com.dontsitdown.R.layout.notif_rate).apply {
            setTextViewText(com.dontsitdown.R.id.rate_text, text)
            for (v in 1..10) setOnClickPendingIntent(BOXES[v - 1], rateIntent(context, which, v))
        }
        val n = androidx.core.app.NotificationCompat.Builder(context, NotificationHelper.CH_CHECKIN)
            .setSmallIcon(com.dontsitdown.R.drawable.ic_shortcut_checkin_small)
            .setColor(0xFF2B3550.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(androidx.core.app.NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setContentIntent(NotificationHelper.openAppIntent(context, MainActivity.OPEN_TODAY, NotificationHelper.ID_CHECKIN))
            .setAutoCancel(true)
            .build()
        NotificationHelper.notify(context, NotificationHelper.ID_CHECKIN, n)
    }

    /** A tap on a number in the reminder. Before 5am the evening one still means last night. */
    fun rate(context: Context, which: String, value: Int, now: java.time.LocalDateTime = java.time.LocalDateTime.now()) {
        val store = context.app.checkin
        val v = value.coerceIn(1, 10)
        if (which == MORNING) store.setMorning(now.toLocalDate()) { it.copy(mood = v) }
        else store.setEvening(if (now.hour < 5) now.toLocalDate().minusDays(1) else now.toLocalDate()) { it.copy(mood = v) }
        store.flush()
        val n = androidx.core.app.NotificationCompat.Builder(context, NotificationHelper.CH_CHECKIN)
            .setSmallIcon(com.dontsitdown.R.drawable.ic_shortcut_checkin_small)
            .setColor(0xFF2B3550.toInt())
            .setContentTitle("mood $v · ${Checkin.moodWord(v)}")
            .setContentText(if (which == MORNING) "noted. tap to add sleep and an intention." else "noted. tap to add the rest of the day.")
            .setContentIntent(NotificationHelper.openAppIntent(context, MainActivity.OPEN_TODAY, NotificationHelper.ID_CHECKIN))
            .setAutoCancel(true)
            .setSilent(true)
            .setTimeoutAfter(8_000)
            .build()
        NotificationHelper.notify(context, NotificationHelper.ID_CHECKIN, n)
    }

    private val BOXES = intArrayOf(
        com.dontsitdown.R.id.rate_1, com.dontsitdown.R.id.rate_2, com.dontsitdown.R.id.rate_3, com.dontsitdown.R.id.rate_4, com.dontsitdown.R.id.rate_5,
        com.dontsitdown.R.id.rate_6, com.dontsitdown.R.id.rate_7, com.dontsitdown.R.id.rate_8, com.dontsitdown.R.id.rate_9, com.dontsitdown.R.id.rate_10,
    )

    private fun rateIntent(context: Context, which: String, value: Int): PendingIntent = PendingIntent.getBroadcast(
        context, 300 + (if (which == MORNING) 0 else 20) + value,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_RATE)
            .putExtra(ReminderReceiver.EXTRA_WHICH, which).putExtra(ReminderReceiver.EXTRA_VALUE, value),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun intent(context: Context, which: String): PendingIntent = PendingIntent.getBroadcast(
        context, if (which == MORNING) REQ_MORNING else REQ_EVENING,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_CHECKIN).putExtra(ReminderReceiver.EXTRA_WHICH, which),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
