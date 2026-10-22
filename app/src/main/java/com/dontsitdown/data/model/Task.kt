package com.dontsitdown.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val estimateMinutes: Int,
    /** Actual durations in minutes, oldest first, capped at [MAX_HISTORY]. */
    val actualTimes: List<Float> = emptyList(),
    val lastDone: Long? = null,
    val frequency: Frequency,
    val nextDue: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
) {
    /** Rolling average of the last 5 real runs, or the estimate until there are any. */
    val effectiveMinutes: Float
        get() = if (actualTimes.isEmpty()) estimateMinutes.toFloat()
        else actualTimes.takeLast(5).average().toFloat()

    val size: TaskSize get() = TaskSize.of(effectiveMinutes)

    companion object {
        const val MAX_HISTORY = 30
    }
}

enum class TaskSize(val label: String) {
    QUICK("quick"), MEDIUM("medium"), LONG("longer");

    companion object {
        fun of(minutes: Float) = when {
            minutes <= 5.5f -> QUICK
            minutes < 15f -> MEDIUM
            else -> LONG
        }
    }
}
