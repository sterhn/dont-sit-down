package com.dontsitdown.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dontsitdown.app
import com.dontsitdown.service.ReminderScheduler
import com.dontsitdown.service.WeeklySummaryWorker
import com.dontsitdown.ui.components.DayToggles
import com.dontsitdown.ui.components.OutlineButton
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.SectionLabel
import com.dontsitdown.ui.components.Segmented
import com.dontsitdown.ui.components.Stepper
import com.dontsitdown.ui.components.ToggleRow
import com.dontsitdown.ui.components.TopBar
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.NotificationHelper
import com.dontsitdown.util.Permissions
import com.dontsitdown.util.SettingsState
import com.dontsitdown.util.ThemeMode
import com.dontsitdown.util.TimeUtils

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.app
    val s by app.settings.state.collectAsStateWithLifecycle()
    // Bumped on resume so permission rows re-check after a trip to system settings.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }

    fun update(reschedule: Boolean = false, change: (SettingsState) -> SettingsState) {
        app.settings.update(change)
        if (reschedule) ReminderScheduler.scheduleNext(context, app.settings.current)
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("settings")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            SectionLabel("the takeover")
            TimeRow("home time", "weekdays", s.homeHour, s.homeMinute) { h, m -> update(true) { it.copy(homeHour = h, homeMinute = m) } }
            TimeRow("home time", "weekends", s.weekendHour, s.weekendMinute) { h, m -> update(true) { it.copy(weekendHour = h, weekendMinute = m) } }
            Text("reminder days", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            DayToggles(s.reminderDays, { days -> update(true) { it.copy(reminderDays = days) } })
            Spacer(Modifier.height(4.dp))
            NextFire(s)
            LabeledStepper("snooze limit", s.snoozeLimit, 0..5) { v -> update { it.copy(snoozeLimit = v) } }

            key(resumes) { PermissionRows(context) }
            Spacer(Modifier.height(8.dp))
            OutlineButton("test the takeover (5 sec)", onClick = {
                ReminderScheduler.scheduleTest(context)
                Toast.makeText(context, "lock your phone. 5 seconds.", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.fillMaxWidth())

            SectionLabel("chains")
            LabeledStepper("chain max", s.chainMaxMinutes, 10..45, step = 5, suffix = " min") { v -> update { it.copy(chainMaxMinutes = v) } }
            ToggleRow("dread calculator", s.dread, { v -> update { it.copy(dread = v) } }, "rate the dread before, see the reality after")

            SectionLabel("voice")
            Text("motivation style", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
            Segmented(listOf(false to "normal", true to "spite"), s.spite, { v -> update { it.copy(spite = v) } }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            ToggleRow("the negotiator", s.negotiator, { v -> update { it.copy(negotiator = v) } }, "when you skip: \"what about just the counters?\"")
            ToggleRow("ghost of tomorrow-you", s.ghost, { v -> update { it.copy(ghost = v) } }, "a word from the person who wakes up to this")
            ToggleRow("scroll shame timer", s.scrollShame, { v -> update { it.copy(scrollShame = v) } }, "one honest nudge, 2 hours later. once.")

            SectionLabel("weekly summary")
            ToggleRow("weekly summary", s.weeklySummary, { v -> update { it.copy(weeklySummary = v) }; WeeklySummaryWorker.schedule(context) })
            if (s.weeklySummary) {
                DayToggles(setOf(s.summaryDay), { days ->
                    val d = (days - s.summaryDay).firstOrNull() ?: s.summaryDay
                    update { it.copy(summaryDay = d) }
                    WeeklySummaryWorker.schedule(context)
                })
                LabeledStepper("at", s.summaryHour, 0..23, suffix = ":00") { v ->
                    update { it.copy(summaryHour = v) }
                    WeeklySummaryWorker.schedule(context)
                }
            }

            SectionLabel("theme")
            Segmented(
                listOf(ThemeMode.SYSTEM to "system", ThemeMode.DARK to "dark", ThemeMode.LIGHT to "light"),
                s.theme, { v -> update { it.copy(theme = v) } }, Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(40.dp))
            Text(
                "everything stays on this phone. no accounts, no sync, no analytics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TimeRow(title: String, sub: String, hour: Int, minute: Int, onPick: (Int, Int) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable { pickTime(context, hour, minute, onPick) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(TimeUtils.hhmm(hour, minute), style = Nums.large, color = MaterialTheme.colorScheme.primary)
    }
}

fun pickTime(context: Context, hour: Int, minute: Int, onPick: (Int, Int) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPick(h, m) }, hour, minute, true).show()
}

@Composable
private fun NextFire(s: SettingsState) {
    val next = ReminderScheduler.nextFireTime(s)
    Text(
        next?.let {
            val day = TimeUtils.toDate(it).dayOfWeek
            val (h, m) = s.homeTimeFor(day.value)
            "next takeover: ${TimeUtils.dayName(day)} ${TimeUtils.hhmm(h, m)}"
        }
            ?: "no reminder days picked. the couch thanks you.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LabeledStepper(label: String, value: Int, range: IntRange, step: Int = 1, suffix: String = "", onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Stepper(value, onChange, range, step, suffix)
    }
}

@Composable
private fun PermissionRows(context: Context) {
    val rows = listOf(
        Triple("notifications", NotificationHelper.canPost(context), Permissions.notificationSettings(context)),
        Triple("exact alarms", ReminderScheduler.canScheduleExact(context), Permissions.exactAlarmSettings(context)),
        Triple("full-screen takeover", ReminderScheduler.canFullScreen(context), Permissions.fullScreenSettings(context)),
    )
    Spacer(Modifier.height(8.dp))
    Rule()
    rows.forEach { (label, ok, intent) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (ok) Text("ok", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else QuietAction("fix →", onClick = { Permissions.open(context, intent) }, color = MaterialTheme.colorScheme.primary)
        }
    }
    Rule()
}
