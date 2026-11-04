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
                    ACTION_SKIP -> Reminders.skip(context)
                    ACTION_RATE -> CheckinReminders.rate(context, intent.getStringExtra(EXTRA_WHICH) ?: CheckinReminders.MORNING, intent.getIntExtra(EXTRA_VALUE, 5))
                    ACTION_CHECKIN -> CheckinReminders.fire(context, intent.getStringExtra(EXTRA_WHICH) ?: CheckinReminders.MORNING)
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
        const val ACTION_CHECKIN = "com.dontsitdown.reminder.CHECKIN"
        const val EXTRA_WHICH = "which"
        const val ACTION_RATE = "com.dontsitdown.reminder.RATE"
        const val EXTRA_VALUE = "value"
        const val ACTION_SKIP = "com.dontsitdown.reminder.SKIP"
        const val ACTION_DISMISSED = "com.dontsitdown.reminder.DISMISSED"
        const val ACTION_SHAME = "com.dontsitdown.reminder.SHAME"
        const val EXTRA_SNOOZED = "snoozed"
        const val EXTRA_TEST = "test"
    }
}
