package com.dontsitdown.data.model

@kotlinx.serialization.Serializable
data class TaskResult(
    val taskId: String,
    val name: String,
    val estimate: Float,
    val actual: Float,
    val dreadRating: Int,
)
