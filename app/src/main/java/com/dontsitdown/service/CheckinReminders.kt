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
            "morning check-in" to "how'd you sleep? ten seconds, then go be a person."
        } else {
            if (Checkin.eveningFilled(day.evening)) return
            "close the day" to "how was it, honestly? two minutes, then bed."
        }
        NotificationHelper.simple(context, NotificationHelper.CH_CHECKIN, NotificationHelper.ID_CHECKIN, title, text, MainActivity.OPEN_TODAY)
    }

    private fun intent(context: Context, which: String): PendingIntent = PendingIntent.getBroadcast(
        context, if (which == MORNING) REQ_MORNING else REQ_EVENING,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_CHECKIN).putExtra(ReminderReceiver.EXTRA_WHICH, which),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
