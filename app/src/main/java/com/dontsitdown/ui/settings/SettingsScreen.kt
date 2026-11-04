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
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
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
fun SettingsScreen(onTasks: () -> Unit = {}) {
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
            Row(Modifier.fillMaxWidth().clickable(onClick = onTasks).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("your tasks", style = MaterialTheme.typography.bodyLarge)
                    Text("what the chains are made of", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("→", style = Nums.medium, color = MaterialTheme.colorScheme.primary)
            }
            Rule()

            AwaySection(s)

            SectionLabel("the takeover")
            TimeRow("home time", "weekdays", s.homeHour, s.homeMinute) { h, m -> update(true) { it.copy(homeHour = h, homeMinute = m) } }
            TimeRow("home time", "weekends", s.weekendHour, s.weekendMinute) { h, m -> update(true) { it.copy(weekendHour = h, weekendMinute = m) } }
            Text("reminder days", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            DayToggles(s.reminderDays, { days -> update(true) { it.copy(reminderDays = days) } })
            Spacer(Modifier.height(4.dp))
            NextFire(s)
            LabeledStepper("snooze limit", s.snoozeLimit, 0..5) { v -> update { it.copy(snoozeLimit = v) } }
            key(resumes) { HomeWifiRows(s) }

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

            SectionLabel("weekly review")
            ToggleRow("sunday review", s.weeklySummary, { v -> update { it.copy(weeklySummary = v) }; WeeklySummaryWorker.schedule(context) }, "how the week felt, and what got done. one notification.")
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

            CheckinSection(s)

            BackupSection()

            SectionLabel("theme")
            Segmented(
                listOf(ThemeMode.SYSTEM to "system", ThemeMode.DARK to "dark", ThemeMode.LIGHT to "light"),
                s.theme, { v -> update { it.copy(theme = v) } }, Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(40.dp))
            Text(
                "still going: the chores and the check-in, in one place.\neverything stays on this phone. no accounts, no sync, no analytics.",
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

@Composable
private fun AwaySection(s: SettingsState) {
    val context = LocalContext.current
    val app = context.app
    val today = TimeUtils.today()
    val through = s.awayThrough(today)
    SectionLabel("not home")
    if (through != null) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (through == today) "away today" else "away through ${TimeUtils.dayName(through.dayOfWeek)}", style = MaterialTheme.typography.bodyLarge)
                Text("no takeover, and the couch doesn't score", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            QuietAction("i'm home →", onClick = { app.settings.clearAway(today) }, color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Text("going somewhere? skip the takeover while you're out.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        Segmented(
            listOf(1 to "today", 2 to "+ tmrw", 3 to "3 days", 7 to "a week"),
            0,
            { n -> scope.launch { com.dontsitdown.service.Reminders.skip(context, n) } },
            Modifier.fillMaxWidth(),
        )
    }
    val upcoming = s.awayDates.filter { it.isAfter(through ?: today) }.sorted()
    if (upcoming.isNotEmpty()) {
        Text(
            "also away: " + upcoming.take(6).joinToString(", ") { TimeUtils.dateLabel(it) },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun CheckinSection(s: SettingsState) {
    val context = LocalContext.current
    val app = context.app
    val store = app.checkin
    val data by store.state.collectAsStateWithLifecycle()
    val exporter = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(store.export().toByteArray()) } }
            .onSuccess { Toast.makeText(context, "exported", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(context, "couldn't write that file", Toast.LENGTH_SHORT).show() }
    }

    SectionLabel("check-in")
    NudgeRow("morning reminder", s.morningNudge, 9 * 60) { v ->
        app.settings.update { it.copy(morningNudge = v) }
        com.dontsitdown.service.CheckinReminders.schedule(context, com.dontsitdown.service.CheckinReminders.MORNING, v)
    }
    NudgeRow("evening reminder", s.eveningNudge, 22 * 60) { v ->
        app.settings.update { it.copy(eveningNudge = v) }
        com.dontsitdown.service.CheckinReminders.schedule(context, com.dontsitdown.service.CheckinReminders.EVENING, v)
    }
    Text(
        "quiet, and only if you haven't checked in yet. boundaries are edited on the evening page.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Text(
        "${data.days.size} days · ${data.goals.size} goals · ${data.log.size} adventures logged",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(14.dp))
    Text("bring it over from the browser app", style = MaterialTheme.typography.bodyLarge)
    Text(
        "your history, goals and boundaries. in the browser app: progress → export data (then open file), or copy data (then paste).",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    com.dontsitdown.ui.checkin.CheckinImportButtons()
    Spacer(Modifier.height(14.dp))
    Text("take it with you", style = MaterialTheme.typography.bodyLarge)
    Text(
        "the same format the browser app reads.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        OutlineButton("export file", onClick = { exporter.launch("daily-checkin-${TimeUtils.today()}.json") }, modifier = Modifier.weight(1f))
        OutlineButton("copy data", onClick = {
            clipboard.setText(androidx.compose.ui.text.AnnotatedString(store.export()))
            Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NudgeRow(title: String, minuteOfDay: Int, default: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    val on = minuteOfDay >= 0
    ToggleRow(title, on, { v -> onChange(if (v) default else -1) }, if (on) "at ${TimeUtils.hhmm(minuteOfDay / 60, minuteOfDay % 60)} · tap the time to change" else "off")
    if (on) {
        Row(Modifier.fillMaxWidth().clickable { pickTime(context, minuteOfDay / 60, minuteOfDay % 60) { h, m -> onChange(h * 60 + m) } }.padding(bottom = 8.dp)) {
            Text(TimeUtils.hhmm(minuteOfDay / 60, minuteOfDay % 60), style = Nums.large, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Everything in one file: for a new phone, a reinstall, or just peace of mind. */
@Composable
private fun BackupSection() {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pending by remember { mutableStateOf<com.dontsitdown.data.Backup?>(null) }

    val saver = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = com.dontsitdown.data.BackupJson.encode(com.dontsitdown.data.BackupJson.create(context))
            runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }
                .onSuccess { Toast.makeText(context, "backup saved", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "couldn't write that file", Toast.LENGTH_SHORT).show() }
        }
    }
    val opener = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val text = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()
        val b = text?.let { com.dontsitdown.data.BackupJson.decode(it) }
        when {
            b != null -> pending = b
            text != null && com.dontsitdown.checkin.CheckinJson.decode(text) != null ->
                Toast.makeText(context, "that's a check-in file, not a full backup. use the check-in import above.", Toast.LENGTH_LONG).show()
            else -> Toast.makeText(context, "that file isn't a backup", Toast.LENGTH_LONG).show()
        }
    }

    SectionLabel("backup")
    Text(
        "everything in one file: tasks, the chore log, settings, days away, and the check-in.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        OutlineButton("save backup", onClick = { saver.launch("still-going-backup-${TimeUtils.today()}.json") }, modifier = Modifier.weight(1f))
        OutlineButton("restore", onClick = { opener.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) }, modifier = Modifier.weight(1f))
    }

    pending?.let { b ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("restore this backup?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    b.summary() + (if (b.exportedAt.isNotBlank()) ", saved ${b.exportedAt.take(10)}" else "") +
                        ".\n\nthis replaces everything on this phone: tasks, history, settings and check-ins.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pending = null
                    scope.launch {
                        com.dontsitdown.data.BackupJson.restore(context, b)
                        Toast.makeText(context, "restored", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("restore", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pending = null }) { Text("cancel", style = MaterialTheme.typography.labelLarge) } },
            containerColor = com.dontsitdown.ui.theme.LocalPaper.current.card,
        )
    }
}

/** "Only when I'm home": the takeover waits for home Wi-Fi. */
@Composable
private fun HomeWifiRows(s: SettingsState) {
    val context = LocalContext.current
    val app = context.app
    fun useCurrent() {
        val ssid = com.dontsitdown.util.HomeWifi.currentSsid(context)
        when {
            !com.dontsitdown.util.HomeWifi.onWifi(context) -> Toast.makeText(context, "you're not on wi-fi right now. try again from home.", Toast.LENGTH_LONG).show()
            ssid == null -> Toast.makeText(context, "couldn't read the network name, so any wi-fi will count as home.", Toast.LENGTH_LONG).show()
            else -> {
                app.settings.update { it.copy(homeSsid = ssid) }
                Toast.makeText(context, "home is \"$ssid\"", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val askLocation = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
    ) { useCurrent() }

    ToggleRow(
        "only when i'm home",
        s.homeWifiOnly,
        { v -> app.settings.update { it.copy(homeWifiOnly = v) } },
        "waits until your phone's on home wi-fi. up to 4 hours after home time.",
    )
    if (!s.homeWifiOnly) return
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("home wi-fi", style = MaterialTheme.typography.bodyLarge)
            Text(s.homeSsid ?: "any wi-fi counts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        QuietAction("use this one →", color = MaterialTheme.colorScheme.primary, onClick = {
            val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (fine) useCurrent()
            else askLocation.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        })
    }
    if (s.homeSsid != null) {
        val background = android.os.Build.VERSION.SDK_INT < 29 || androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (background) "checks the network name at home time" else "to check the name at home time, location needs \"allow all the time\". until then, any wi-fi counts.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!background) QuietAction("fix →", color = MaterialTheme.colorScheme.primary, onClick = {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            })
        }
        QuietAction("forget it (any wi-fi)", onClick = { app.settings.update { it.copy(homeSsid = null) } })
    }
}
