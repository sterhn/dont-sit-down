package com.dontsitdown.data.repository

import com.dontsitdown.data.db.AppDatabase
import com.dontsitdown.data.model.ChainResult
import com.dontsitdown.data.model.ReminderEvent
import com.dontsitdown.data.model.ReminderEventType
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskCompletion
import com.dontsitdown.util.TimeUtils

class TaskRepository(db: AppDatabase) {
    private val tasks = db.taskDao()
    private val chains = db.chainResultDao()

    fun observeTasks() = tasks.observeAll()
    fun observeTask(id: String) = tasks.observe(id)
    fun observeCompletions(taskId: String) = tasks.observeCompletions(taskId)
    fun observeCompletionsBetween(from: Long, to: Long) = tasks.observeCompletionsBetween(from, to)
    fun observeChainResults() = chains.observeAll()

    suspend fun allTasks() = tasks.getAll()
    suspend fun activeTasks() = tasks.getActive()
    suspend fun task(id: String) = tasks.get(id)

    /** Keeps the caller's order, drops ids that no longer exist or were switched off. */
    suspend fun tasksByIds(ids: List<String>): List<Task> {
        val byId = tasks.getByIds(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it] }.filter { it.isActive }
    }

    suspend fun hasTasks() = tasks.count() > 0

    suspend fun save(task: Task) {
        val withOrder = if (tasks.get(task.id) == null) task.copy(sortOrder = tasks.maxSortOrder() + 1) else task
        tasks.upsert(withOrder.copy(nextDue = TimeUtils.nextDue(withOrder.lastDone, withOrder.frequency)))
    }

    suspend fun saveAll(list: List<Task>) = tasks.upsertAll(list)
    suspend fun delete(task: Task) = tasks.delete(task)
    suspend fun reorder(ids: List<String>) = tasks.reorder(ids)

    suspend fun recordCompletion(taskId: String, minutes: Float, at: Long, chainId: String?) {
        val task = tasks.get(taskId) ?: return
        // Never move lastDone backwards (e.g. a late write for an earlier run).
        val lastDone = maxOf(task.lastDone ?: at, at)
        tasks.update(
            task.copy(
                actualTimes = (task.actualTimes + minutes).takeLast(Task.MAX_HISTORY),
                lastDone = lastDone,
                nextDue = TimeUtils.nextDue(lastDone, task.frequency),
            )
        )
        tasks.insertCompletion(TaskCompletion(taskId = taskId, taskName = task.name, date = at, minutes = minutes, chainId = chainId))
    }

    /** Daily housekeeping: keep nextDue consistent with frequency (e.g. after a frequency edit). */
    suspend fun refreshDueDates() {
        val all = tasks.getAll()
        val changed = all.mapNotNull { t ->
            val due = TimeUtils.nextDue(t.lastDone, t.frequency)
            if (due != t.nextDue) t.copy(nextDue = due) else null
        }
        if (changed.isNotEmpty()) tasks.upsertAll(changed)
    }

    suspend fun saveChainResult(result: ChainResult) = chains.upsert(result)
    suspend fun allChainResults() = chains.getAll()
    suspend fun chainResultsBetween(from: Long, to: Long) = chains.between(from, to)
    suspend fun completionsBetween(from: Long, to: Long) = tasks.completionsBetween(from, to)
    suspend fun lastCompletionAt() = tasks.lastCompletionAt()

    suspend fun logEvent(type: ReminderEventType, at: Long = System.currentTimeMillis()) =
        chains.insertEvent(ReminderEvent(timestamp = at, type = type))

    suspend fun eventsBetween(from: Long, to: Long) = chains.eventsBetween(from, to)
}
