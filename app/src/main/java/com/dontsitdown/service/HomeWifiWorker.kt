package com.dontsitdown.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dontsitdown.app
import com.dontsitdown.util.HomeWifi
import com.dontsitdown.util.TimeUtils
import java.util.concurrent.TimeUnit

/**
 * Home time came and the phone wasn't on home Wi-Fi: wait for Wi-Fi, check it's home, then do the
 * takeover. Gives up after [WINDOW_MS]; if you never got home, the evening just goes by.
 */
class HomeWifiWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val since = inputData.getLong(KEY_SINCE, 0L)
        val now = System.currentTimeMillis()
        if (now - since > WINDOW_MS) return Result.success()
        val app = applicationContext.app
        if (!app.settings.current.homeWifiOnly) return Result.success()
        if (HomeWifi.atHome(applicationContext, app.settings.current)) {
            Reminders.fire(applicationContext, snoozed = false, test = false, skipHomeCheck = true)
        } else {
            // On some other Wi-Fi (a café, work). Look again in a bit.
            enqueue(applicationContext, since, delayMinutes = 10)
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "home-wifi"
        private const val KEY_SINCE = "since"
        const val WINDOW_MS = 4 * TimeUtils.HOUR

        fun enqueue(context: Context, since: Long, delayMinutes: Long = 0) {
            val req = OneTimeWorkRequestBuilder<HomeWifiWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_SINCE to since))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, req)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
