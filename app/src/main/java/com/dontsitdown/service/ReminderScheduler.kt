package com.dontsitdown.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.dontsitdown.MainActivity
import com.dontsitdown.util.SettingsState
import com.dontsitdown.util.TimeUtils

/** Alarm mechanics only. What happens when an alarm goes off lives in [Reminders]. */
object ReminderScheduler {
    private const val REQ_MAIN = 100
    private const val REQ_SNOOZE = 101
    private const val REQ_SHAME = 102
    private const val REQ_TEST = 103

    const val SNOOZE_MINUTES = 10
    const val SHAME_AFTER_MS = 2 * TimeUtils.HOUR

    fun nextFireTime(s: SettingsState, now: Long = System.currentTimeMillis()): Long? {
        if (s.reminderDays.isEmpty()) return null
        val today = TimeUtils.toDate(now)
        for (d in 0..7L) {
            val date = today.plusDays(d)
            val dow = date.dayOfWeek.value
            if (dow !in s.reminderDays) continue
            val (h, m) = s.homeTimeFor(dow)
            val t = TimeUtils.at(date, h, m)
            if (t > now + 1_000) return t
        }
        return null
    }

    fun scheduleNext(context: Context, s: SettingsState) {
        val pi = fireIntent(context, REQ_MAIN, snoozed = false, test = false)
        alarmManager(context).cancel(pi)
        val at = nextFireTime(s) ?: return
        setExact(context, at, pi)
    }

    fun scheduleSnooze(context: Context) =
        setExact(context, System.currentTimeMillis() + SNOOZE_MINUTES * TimeUtils.MINUTE, fireIntent(context, REQ_SNOOZE, snoozed = true, test = false))

    fun cancelSnooze(context: Context) =
        alarmManager(context).cancel(fireIntent(context, REQ_SNOOZE, snoozed = true, test = false))

    fun scheduleShame(context: Context, at: Long) {
        val pi = PendingIntent.getBroadcast(
            context, REQ_SHAME,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_SHAME),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Not alarm-clock critical: an inexact-but-idle-safe alarm is plenty for one gentle nudge.
        alarmManager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Settings → "test the takeover": fires in a few seconds, ignores "already done today". */
    fun scheduleTest(context: Context, delayMs: Long = 5_000) =
        setExact(context, System.currentTimeMillis() + delayMs, fireIntent(context, REQ_TEST, snoozed = false, test = true))

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || alarmManager(context).canScheduleExactAlarms()

    fun canFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    private fun setExact(context: Context, at: Long, pi: PendingIntent) {
        val am = alarmManager(context)
        try {
            if (canScheduleExact(context)) {
                val show = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
                return
            }
        } catch (e: SecurityException) {
            Log.w("ReminderScheduler", "exact alarm refused, falling back", e)
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun fireIntent(context: Context, requestCode: Int, snoozed: Boolean, test: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_FIRE)
                .putExtra(ReminderReceiver.EXTRA_SNOOZED, snoozed)
                .putExtra(ReminderReceiver.EXTRA_TEST, test),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)
}
