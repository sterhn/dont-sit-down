package com.dontsitdown.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dontsitdown.app
import com.dontsitdown.data.DefaultTasks
import com.dontsitdown.data.model.Frequency
import com.dontsitdown.data.model.Task
import com.dontsitdown.service.ReminderScheduler
import com.dontsitdown.service.WeeklySummaryWorker
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.DayToggles
import com.dontsitdown.ui.components.OutlineButton
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.Rule
import com.dontsitdown.ui.components.SectionLabel
import com.dontsitdown.ui.settings.pickTime
import com.dontsitdown.ui.theme.Nums
import com.dontsitdown.util.NotificationHelper
import com.dontsitdown.util.Permissions
import com.dontsitdown.util.TimeUtils
import kotlinx.coroutines.launch

private const val STEPS = 7

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()

    var step by rememberSaveable { mutableIntStateOf(0) }
    val tasks = remember { mutableStateListOf<Task>().apply { addAll(DefaultTasks.create()) } }
    var hour by rememberSaveable { mutableIntStateOf(19) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    var days by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var spite by rememberSaveable { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { step++ }

    BackHandler(enabled = step > 0) { step-- }

    fun finish() = scope.launch {
        if (!app.repository.hasTasks()) {
            app.repository.saveAll(tasks.mapIndexed { i, t -> t.copy(sortOrder = i) })
        }
        app.settings.update { it.copy(onboarded = true, homeHour = hour, homeMinute = minute, reminderDays = days, spite = spite) }
        ReminderScheduler.scheduleNext(context, app.settings.current)
        WeeklySummaryWorker.schedule(context)
        onDone()
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp),
    ) {
        Text(
            "${step + 1} / $STEPS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp),
        )
        when (step) {
            0 -> Page {
                Spacer(Modifier.weight(1f))
                Text("this app has one job:", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("catch you before you sit down.", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.weight(1f))
                BigButton("ok", onClick = { step++ })
            }

            1 -> Page {
                Spacer(Modifier.height(16.dp))
                Text("what applies to you?", style = MaterialTheme.typography.displayMedium)
                Text("turn off anything you don't have. no plants? no plants.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TaskPicker(tasks, Modifier.weight(1f))
                Spacer(Modifier.height(12.dp))
                BigButton("next", onClick = { step++ })
            }

            2 -> Page {
                Spacer(Modifier.weight(1f))
                Text("when do you usually get home?", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(24.dp))
                Text(
                    TimeUtils.hhmm(hour, minute),
                    style = Nums.huge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { pickTime(context, hour, minute) { h, m -> hour = h; minute = m } },
                )
                Text("tap to change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                DayToggles(days, { days = it })
                Text(
                    "weekends are off by default. you can set a separate weekend time later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                BigButton("next", onClick = { step++ })
            }

            3 -> Page {
                Spacer(Modifier.weight(1f))
                Text("notifications.", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text("so i can catch you at the door.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                BigButton("allow", onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !NotificationHelper.canPost(context)) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else step++
                })
                if (!ReminderScheduler.canFullScreen(context)) {
                    QuietAction("also allow full-screen (so it can light up the lock screen)", onClick = {
                        Permissions.open(context, Permissions.fullScreenSettings(context))
                    }, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }

            4 -> Page {
                Spacer(Modifier.weight(1f))
                Text("alarms.", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "so i show up at ${TimeUtils.hhmm(hour, minute)}, not \"sometime around then\".",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (ReminderScheduler.canScheduleExact(context)) {
                    BigButton("next", onClick = { step++ })
                } else {
                    BigButton("allow exact alarms", onClick = { Permissions.open(context, Permissions.exactAlarmSettings(context)) })
                    QuietAction("done that, next", onClick = { step++ }, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }

            5 -> Page {
                Spacer(Modifier.weight(1f))
                Text("want the nice version\nor the honest version?", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(16.dp))
                Text(
                    "honest: \"your past self left this for you again. classic her.\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                OutlineButton("nice", onClick = { spite = false; step++ }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                BigButton("honest", onClick = { spite = true; step++ })
                Text(
                    "changeable any time in settings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                )
            }

            else -> Page {
                Spacer(Modifier.weight(1f))
                Text("done.", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(8.dp))
                Text("first chain's ready.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                BigButton("show me", onClick = { finish() })
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ColumnScope.Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth(), content = content)
}

@Composable
private fun TaskPicker(tasks: MutableList<Task>, modifier: Modifier) {
    var newName by remember { mutableStateOf("") }
    Column(modifier.verticalScroll(rememberScrollState())) {
        val defaults = DefaultTasks.groups
        defaults.forEach { (label, range) ->
            SectionLabel(label)
            range.forEach { i -> if (i < tasks.size) PickRow(tasks, i) }
        }
        if (tasks.size > DefaultTasks.COUNT) {
            SectionLabel("yours")
            (DefaultTasks.COUNT until tasks.size).forEach { PickRow(tasks, it) }
        }
        SectionLabel("add your own")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("e.g. feed the cat") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
            QuietAction("add", color = MaterialTheme.colorScheme.primary, onClick = {
                if (newName.isNotBlank()) {
                    tasks += Task(name = newName.trim(), estimateMinutes = 5, frequency = Frequency.WEEKLY)
                    newName = ""
                }
            })
        }
        Text(
            "new ones start at 5 min, weekly. tweak them later in tasks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun PickRow(tasks: MutableList<Task>, i: Int) {
    val t = tasks[i]
    Row(
        Modifier.fillMaxWidth().clickable { tasks[i] = t.copy(isActive = !t.isActive) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).alpha(if (t.isActive) 1f else 0.35f)) {
            Text(t.name, style = MaterialTheme.typography.bodyLarge)
            Text("${t.estimateMinutes}m · ${t.frequency.label}", style = Nums.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            if (t.isActive) "on" else "off",
            style = MaterialTheme.typography.labelMedium,
            color = if (t.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Rule()
}
