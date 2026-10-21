package com.dontsitdown.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dontsitdown.app

/** Alarms don't survive reboots or clock changes; put tonight's back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = context.app.settings.current
        if (settings.onboarded) ReminderScheduler.scheduleNext(context, settings)
        WeeklySummaryWorker.schedule(context)
    }
}
