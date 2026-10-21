package com.dontsitdown.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dontsitdown.app
import com.dontsitdown.data.model.ReminderEventType
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        context.app.scope.launch {
            try {
                when (intent.action) {
                    ACTION_FIRE -> Reminders.fire(
                        context,
                        snoozed = intent.getBooleanExtra(EXTRA_SNOOZED, false),
                        test = intent.getBooleanExtra(EXTRA_TEST, false),
                    )
                    ACTION_SNOOZE -> Reminders.snooze(context)
                    // Swiped the notification away. Noted, no guilt trip right now.
                    ACTION_DISMISSED -> Reminders.refused(context, ReminderEventType.DISMISSED)
                    ACTION_SHAME -> Reminders.scrollShame(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.dontsitdown.reminder.FIRE"
        const val ACTION_SNOOZE = "com.dontsitdown.reminder.SNOOZE"
        const val ACTION_DISMISSED = "com.dontsitdown.reminder.DISMISSED"
        const val ACTION_SHAME = "com.dontsitdown.reminder.SHAME"
        const val EXTRA_SNOOZED = "snoozed"
        const val EXTRA_TEST = "test"
    }
}
