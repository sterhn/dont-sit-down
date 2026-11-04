package com.dontsitdown.data

import android.content.Context
import androidx.room.withTransaction
import com.dontsitdown.app
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.CheckinJson
import com.dontsitdown.data.model.ChainResult
import com.dontsitdown.data.model.ReminderEvent
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskCompletion
import com.dontsitdown.service.CheckinReminders
import com.dontsitdown.service.ReminderScheduler
import com.dontsitdown.service.WeeklySummaryWorker
import com.dontsitdown.util.SettingsState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant

/** Everything the app knows, in one file: tasks, the chore log, settings, days away, and the check-in. */
@Serializable
data class Backup(
    val kind: String = KIND,
    val version: Int = 1,
    val exportedAt: String = "",
    val settings: SettingsState = SettingsState(),
    val tasks: List<Task> = emptyList(),
    val completions: List<TaskCompletion> = emptyList(),
    val chainResults: List<ChainResult> = emptyList(),
    val events: List<ReminderEvent> = emptyList(),
    val checkin: CheckinData = CheckinData(),
) {
    fun summary(): String =
        "${tasks.size} tasks, ${completions.size} chores done, ${checkin.days.size} check-in days, ${checkin.goals.size} goals"

    companion object {
        const val KIND = "still-going-backup"
    }
}

object BackupJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
        prettyPrint = true
    }

    fun encode(b: Backup): String = json.encodeToString(Backup.serializer(), b)

    /** A backup made by this app, or null if [text] is something else (a check-in export, garbage). */
    fun decode(text: String): Backup? = runCatching {
        val el = json.parseToJsonElement(text) as? JsonObject ?: return null
        if ((el["kind"] as? JsonPrimitive)?.content != Backup.KIND) return null
        val b = json.decodeFromJsonElement(Backup.serializer(), JsonObject(el - "checkin"))
        // The check-in half goes through the same forgiving reader as any other check-in file.
        val checkin = (el["checkin"])?.let { CheckinJson.normalize(it) } ?: CheckinData()
        b.copy(checkin = checkin)
    }.getOrNull()

    suspend fun create(context: Context): Backup {
        val app = context.app
        val db = app.db
        return Backup(
            exportedAt = Instant.now().toString(),
            settings = app.settings.current,
            tasks = db.taskDao().getAll(),
            completions = db.taskDao().allCompletions(),
            chainResults = db.chainResultDao().getAll(),
            events = db.chainResultDao().allEvents(),
            checkin = app.checkin.also { it.flush() }.current,
        )
    }

    /** Replace everything with [b]. The chain in progress (if any) is dropped; alarms are rebooked. */
    suspend fun restore(context: Context, b: Backup) {
        val app = context.app
        val db = app.db
        db.withTransaction {
            db.taskDao().clearCompletions()
            db.taskDao().clearTasks()
            db.chainResultDao().clearResults()
            db.chainResultDao().clearEvents()
            db.taskDao().upsertAll(b.tasks)
            db.taskDao().insertCompletions(b.completions)
            db.chainResultDao().upsertAll(b.chainResults)
            db.chainResultDao().insertEvents(b.events)
        }
        app.settings.update { b.settings.copy(onboarded = true) }
        app.checkin.replaceAll(b.checkin)
        app.session.clear()
        ReminderScheduler.scheduleNext(context, app.settings.current)
        CheckinReminders.scheduleAll(context)
        WeeklySummaryWorker.schedule(context)
    }
}
