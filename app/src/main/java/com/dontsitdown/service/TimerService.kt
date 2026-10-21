package com.dontsitdown.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.dontsitdown.R
import com.dontsitdown.app
import com.dontsitdown.chain.Phase
import com.dontsitdown.chain.SessionMode
import com.dontsitdown.chain.SessionState
import com.dontsitdown.util.NotificationHelper
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/**
 * Keeps the timer alive while the app is backgrounded, with an ongoing notification
 * (a native chronometer, so no per-second updates) and DONE / KEEP MOVING actions.
 */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = app.session
        when (intent?.action) {
            ACTION_DONE -> session.done()
            ACTION_NEXT -> session.next()
        }
        val state = session.state.value
        if (state != null && state.isRunning) {
            goForeground(build(state))
            if (!observing) observe()
        } else if (!observing) {
            // Nothing to time (a stale notification action): honour the start contract, then go.
            goForeground(placeholder())
            stop(state)
        }
        // Otherwise the observer sees the chain end and stops the service itself.
        return START_NOT_STICKY
    }

    private var observing = false

    private fun observe() {
        observing = true
        scope.launch {
            app.session.state
                .distinctUntilChangedBy { it?.phase to it?.index }
                .collect { s ->
                    if (s == null || !s.isRunning) {
                        stop(s)
                    } else {
                        NotificationHelper.notify(this@TimerService, NotificationHelper.ID_TIMER, build(s))
                        if (s.phase == Phase.BETWEEN && !appInForeground()) announceDone(s)
                        if (s.phase == Phase.ACTIVE) NotificationHelper.cancel(this@TimerService, NotificationHelper.ID_PROGRESS)
                    }
                }
        }
    }

    private fun goForeground(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NotificationHelper.ID_TIMER, n, type)
    }

    private fun stop(last: SessionState?) {
        if (last?.phase == Phase.COMPLETE && !appInForeground()) {
            val title = if (last.mode == SessionMode.SINGLE) "${last.runs.lastOrNull()?.name ?: "task"}: done." else "chain done."
            NotificationHelper.simple(this, NotificationHelper.CH_PROGRESS, NotificationHelper.ID_PROGRESS, title, last.quip)
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** "dishes: done. 6 min. next: vacuum." */
    private fun announceDone(s: SessionState) {
        val run = s.runs.lastOrNull() ?: return
        val next = s.next?.task?.name ?: return
        val n = NotificationCompat.Builder(this, NotificationHelper.CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_chair)
            .setColor(0xFFD4920A.toInt())
            .setContentTitle("${run.name}: done. ${TimeUtils.minutesLabel(run.actual)}.")
            .setContentText("next: $next.")
            .setContentIntent(NotificationHelper.openAppIntent(this))
            .addAction(0, "keep moving", action(ACTION_NEXT))
            .setAutoCancel(true)
            .build()
        NotificationHelper.notify(this, NotificationHelper.ID_PROGRESS, n)
    }

    private fun base() = NotificationCompat.Builder(this, NotificationHelper.CH_TIMER)
        .setSmallIcon(R.drawable.ic_stat_chair)
        .setColor(0xFFD4920A.toInt())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(NotificationHelper.openAppIntent(this))

    private fun placeholder() = base().setContentTitle("don't sit down.").build()

    private fun build(s: SessionState): Notification = when (s.phase) {
        Phase.ACTIVE -> {
            val t = s.current!!
            val position = if (s.plan.size > 1) " · ${s.index + 1} of ${s.plan.size}" else ""
            base()
                .setContentTitle(t.task.name)
                .setContentText("est ${TimeUtils.wholeMinutes(t.estimate)} min$position")
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(s.currentStartedAt)
                .addAction(0, "done", action(ACTION_DONE))
                .build()
        }
        else -> {
            val run = s.runs.lastOrNull()
            base()
                .setContentTitle("done. ${run?.let { TimeUtils.minutesLabel(it.actual) } ?: ""}.")
                .setContentText("next: ${s.next?.task?.name ?: "—"}")
                .addAction(0, "keep moving", action(ACTION_NEXT))
                .build()
        }
    }

    private fun action(a: String): PendingIntent = PendingIntent.getService(
        this, a.hashCode(),
        Intent(this, TimerService::class.java).setAction(a),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun appInForeground() =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_DONE = "com.dontsitdown.timer.DONE"
        const val ACTION_NEXT = "com.dontsitdown.timer.NEXT"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
            } catch (e: Exception) {
                // Background-start restrictions: the in-app timer still works, just without the notification.
                Log.w("TimerService", "couldn't start foreground timer", e)
            }
        }
    }
}
