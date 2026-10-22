package com.dontsitdown.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chain_results")
data class ChainResult(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val tasks: List<TaskResult>,
    val totalActual: Float,
    val totalEstimate: Float,
    /** Dread rated before the chain, 1..3. 0 = not rated. */
    val totalDreadBefore: Int,
    /** Longest run of tasks done back-to-back without a long pause. */
    val combo: Int = 0,
    /** Single-task runs ("just one") are stored too, so history is complete. */
    val isSingle: Boolean = false,
)
