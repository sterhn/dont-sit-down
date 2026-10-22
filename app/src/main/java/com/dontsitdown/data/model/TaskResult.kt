package com.dontsitdown.data.model

data class TaskResult(
    val taskId: String,
    val name: String,
    val estimate: Float,
    val actual: Float,
    val dreadRating: Int,
)
