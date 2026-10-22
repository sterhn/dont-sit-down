package com.dontsitdown.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReminderEventType { FIRED, SNOOZED, STARTED, DISMISSED, NEGOTIATED, REFUSED }

@Entity(tableName = "reminder_events", indices = [Index("timestamp")])
data class ReminderEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: ReminderEventType,
)
