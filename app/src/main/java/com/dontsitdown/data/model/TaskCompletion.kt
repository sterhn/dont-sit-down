package com.dontsitdown.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One finished run of one task. The factual log behind the weekly grid and task stats. */
@Entity(tableName = "completions", indices = [Index("taskId"), Index("date")])
data class TaskCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val taskName: String,
    val date: Long,
    val minutes: Float,
    val chainId: String?,
)
