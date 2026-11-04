package com.dontsitdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.dontsitdown.MainActivity
import com.dontsitdown.R
import com.dontsitdown.app
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.Face
import com.dontsitdown.ui.components.drawable
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** What the widget shows, worked out without any Android in the way (so it can be tested). */
data class WidgetState(
    val face: Face,
    val caption: String,
    val title: String,
    val line: String,
    val button: String,
    val ticket: String,
) {
    companion object {
        fun of(data: CheckinData, now: LocalDateTime, away: Boolean, choresDone: Int, choreMinutes: Int, chainMinutes: Int?): WidgetState {
            val hour = now.hour
            val evening = hour < 5 || hour >= 17
            val date: LocalDate = if (hour < 5) now.toLocalDate().minusDays(1) else now.toLocalDate()
            val day = data.day(Checkin.key(date))
            val mood = if (evening) day.evening?.mood ?: day.morning?.mood else day.morning?.mood
            val small = hour < 7
            val face = when {
                mood != null -> Checkin.faceFor(mood)
                away -> Face.AWAY
                evening -> Checkin.faceFor(null, Face.COZY, small)
                else -> Checkin.faceFor(null, Face.COFFEE, small)
            }
            val filled = if (evening) Checkin.eveningFilled(day.evening) else Checkin.morningFilled(day.morning)
            return WidgetState(
                face = face,
                caption = mood?.let { Checkin.moodWord(it) } ?: Checkin.FACE_CAPTIONS[face] ?: "",
                title = when {
                    hour < 5 -> "Last night"
                    evening -> "Good evening"
                    else -> "Good morning"
                },
                line = when {
                    filled && mood != null -> "Mood $mood · ${Checkin.moodWord(mood)}"
                    filled -> "Written down"
                    else -> "Not checked in yet"
                },
                button = if (filled) "Open →" else "Check in →",
                ticket = when {
                    choresDone > 0 -> "CHORES · $choresDone DONE · $choreMinutes MIN ✓"
                    away -> "CHORES · NOT HOME TODAY"
                    chainMinutes != null && chainMinutes > 0 -> "DON'T SIT DOWN · $chainMinutes MIN TONIGHT  GO ›"
                    else -> "CHORES · NOTHING DUE"
                },
            )
        }
    }
}

class CheckinWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        context.app.scope.launch {
            try {
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Redraw every placed widget. Cheap when there are none. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, CheckinWidget::class.java))
            if (ids.isEmpty()) return
            context.app.scope.launch { render(context, manager, ids) }
        }

        suspend fun state(context: Context, now: LocalDateTime = LocalDateTime.now()): WidgetState {
            val app = context.app
            val today = TimeUtils.today()
            val start = TimeUtils.startOfDay(today)
            val chores = app.repository.completionsBetween(start, start + TimeUtils.DAY)
            val away = app.settings.current.isAway(today)
            val chain = if (chores.isEmpty() && !away) app.generateChain().sumOf { it.effectiveMinutes.toDouble() }.toFloat() else 0f
            return WidgetState.of(
                app.checkin.current, now, away,
                choresDone = chores.size,
                choreMinutes = TimeUtils.wholeMinutes(chores.sumOf { it.minutes.toDouble() }.toFloat()),
                chainMinutes = if (chain > 0f) TimeUtils.wholeMinutes(chain) else null,
            )
        }

        fun views(context: Context, s: WidgetState): RemoteViews = RemoteViews(context.packageName, R.layout.widget_checkin).apply {
            setImageViewResource(R.id.w_face, s.face.drawable())
            setTextViewText(R.id.w_caption, s.caption)
            setTextViewText(R.id.w_title, s.title)
            setTextViewText(R.id.w_line, s.line)
            setTextViewText(R.id.w_button, s.button)
            setTextViewText(R.id.w_ticket, s.ticket)
            setOnClickPendingIntent(R.id.w_root, open(context, MainActivity.OPEN_TODAY, 400))
            setOnClickPendingIntent(R.id.w_button, open(context, MainActivity.OPEN_TODAY, 401))
            setOnClickPendingIntent(R.id.w_ticket, open(context, MainActivity.OPEN_CHAIN, 402))
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val v = views(context, state(context))
            ids.forEach { manager.updateAppWidget(it, v) }
        }

        private fun open(context: Context, target: String, code: Int): PendingIntent = PendingIntent.getActivity(
            context, code,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
