package com.dontsitdown.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dontsitdown.MainActivity
import com.dontsitdown.app
import com.dontsitdown.chain.SummaryCalculator
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.util.NotificationHelper
import com.dontsitdown.util.TimeUtils
import java.util.concurrent.TimeUnit

/** Fires once at the configured day/hour (default sunday 20:00), then books the next one. */
class WeeklySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext.app
        val settings = app.settings.current
        if (settings.weeklySummary && settings.onboarded) {
            val start = TimeUtils.weekStart(TimeUtils.today())
            val summary = SummaryCalculator.week(app.repository, start, settings.awayDates)
            val text = reviewText(Checkin.weekReview(app.checkin.current, start, TimeUtils.today()), summary)
            if (text != null) {
                NotificationHelper.simple(
                    applicationContext, NotificationHelper.CH_SUMMARY, NotificationHelper.ID_SUMMARY,
                    "your week", text, MainActivity.OPEN_STORY,
                )
            }
        }
        schedule(applicationContext, afterRun = true)
        return Result.success()
    }

    companion object {
        private const val NAME = "weekly-summary"

        /** Both halves in one note: how the week felt first, then what got done. Null if there's nothing to say. */
        fun reviewText(checkin: List<String>, chores: com.dontsitdown.chain.WeekSummary): String? {
            if (checkin.isEmpty() && chores.isEmpty) return null
            return buildString {
                checkin.forEach { appendLine(it) }
                if (checkin.isNotEmpty() && !chores.isEmpty) appendLine()
                if (!chores.isEmpty) append(chores.toText())
            }.trim()
        }

        fun nextRun(day: Int, hour: Int, now: Long = System.currentTimeMillis(), afterRun: Boolean = false): Long {
            val today = TimeUtils.toDate(now)
            for (d in 0..7L) {
                val date = today.plusDays(d)
                if (date.dayOfWeek.value != day) continue
                val t = TimeUtils.at(date, hour, 0)
                // Right after a run, skip today's slot even if WorkManager ran us a little early.
                if (t > now + if (afterRun) TimeUtils.HOUR else 0) return t
            }
            return TimeUtils.at(today.plusDays(7), hour, 0)
        }

        fun schedule(context: Context, afterRun: Boolean = false) {
            val s = context.app.settings.current
            val delay = nextRun(s.summaryDay, s.summaryHour, afterRun = afterRun) - System.currentTimeMillis()
            WorkManager.getInstance(context).enqueueUniqueWork(
                NAME,
                if (afterRun) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WeeklySummaryWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build(),
            )
        }
    }
}
