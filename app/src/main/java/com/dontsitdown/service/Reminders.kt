package com.dontsitdown.service

import android.content.Context
import com.dontsitdown.app
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.chain.Phase
import com.dontsitdown.data.model.ReminderEventType
import com.dontsitdown.data.model.Task
import com.dontsitdown.util.NotificationHelper
import com.dontsitdown.util.TimeUtils

/** What the takeover does: fire, snooze, get dismissed, and (optionally) one honest nudge later. */
object Reminders {
    /** Did something in the last few hours? Then don't nag. */
    private const val RECENTLY_DONE_MS = 3 * TimeUtils.HOUR

    suspend fun fire(context: Context, snoozed: Boolean, test: Boolean) {
        val app = context.app
        val settings = app.settings
        val repo = app.repository
        if (!settings.current.onboarded) return
        if (!snoozed && !test) {
            ReminderScheduler.scheduleNext(context, settings.current)
            settings.resetSnoozes()
        }
        val phase = app.session.state.value?.phase
        if (phase == Phase.DREAD || phase == Phase.ACTIVE || phase == Phase.BETWEEN) return // already moving

        val now = System.currentTimeMillis()
        if (!test) {
            val last = repo.lastCompletionAt()
            if (last != null && now - last < RECENTLY_DONE_MS) return
        }

        val chain = (if (snoozed) repo.tasksByIds(settings.pendingChain) else emptyList()).ifEmpty { app.generateChain() }
        if (chain.isEmpty()) return
        settings.pendingChain = chain.map { it.id }
        if (!snoozed) settings.lastFiredAt = now
        repo.logEvent(ReminderEventType.FIRED, now)

        val snoozeAllowed = settings.snoozesToday() < settings.current.snoozeLimit
        NotificationHelper.showTakeover(context, chain.map { it.name }, chain.sumOf { it.effectiveMinutes.toDouble() }.toFloat(), snoozeAllowed)

        if (!snoozed && !test && settings.current.scrollShame) {
            ReminderScheduler.scheduleShame(context, now + ReminderScheduler.SHAME_AFTER_MS)
        }
    }

    suspend fun snooze(context: Context) {
        val app = context.app
        app.settings.addSnooze()
        NotificationHelper.cancel(context, NotificationHelper.ID_TAKEOVER)
        app.repository.logEvent(ReminderEventType.SNOOZED)
        ReminderScheduler.scheduleSnooze(context)
    }

    suspend fun refused(context: Context, type: ReminderEventType) {
        NotificationHelper.cancel(context, NotificationHelper.ID_TAKEOVER)
        ReminderScheduler.cancelSnooze(context)
        context.app.repository.logEvent(type)
    }

    suspend fun started(context: Context, type: ReminderEventType = ReminderEventType.STARTED) {
        NotificationHelper.cancel(context, NotificationHelper.ID_TAKEOVER)
        ReminderScheduler.cancelSnooze(context)
        context.app.repository.logEvent(type)
    }

    /** One observation, once, only if nothing happened and the app wasn't even opened. */
    suspend fun scrollShame(context: Context) {
        val app = context.app
        val settings = app.settings
        if (!settings.current.scrollShame) return
        val today = TimeUtils.today()
        if (settings.shameNudgedDay == today.toEpochDay()) return
        val fired = settings.lastFiredAt
        if (fired == 0L || TimeUtils.toDate(fired) != today) return
        val last = app.repository.lastCompletionAt()
        if (last != null && last >= fired) return
        if (settings.lastOpenedAt >= fired) return
        if (app.session.state.value?.isRunning == true) return

        val hours = ((System.currentTimeMillis() - fired) / TimeUtils.HOUR).toInt().coerceAtLeast(1)
        val minutes = app.repository.tasksByIds(settings.pendingChain).sumOf { it.effectiveMinutes.toDouble() }.toFloat()
            .takeIf { it > 0f } ?: settings.current.chainMaxMinutes.toFloat()
        NotificationHelper.simple(
            context, NotificationHelper.CH_NUDGE, NotificationHelper.ID_NUDGE,
            "still there?", MessageGenerator.scrollShame(hours, minutes).text,
        )
        settings.shameNudgedDay = today.toEpochDay()
    }

    /**
     * Two small offers for the negotiator: the second-quickest due task, then the quickest.
     * ("just the counters? 4 minutes." → "the trash then. 3 minutes.")
     */
    fun negotiationOffers(tasks: List<Task>): List<Task> {
        val active = tasks.filter { it.isActive }
        val due = active.filter { (TimeUtils.dueInDays(it) ?: 1) <= 0 }
        val pool = (if (due.size >= 2) due else active).sortedBy { it.effectiveMinutes }
        return when {
            pool.size >= 2 -> listOf(pool[1], pool[0])
            else -> pool
        }
    }
}
