package com.dontsitdown.data.db

import androidx.room.TypeConverter
import com.dontsitdown.data.model.TaskResult
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun floatsToString(list: List<Float>): String = list.joinToString(",")

    @TypeConverter
    fun stringToFloats(s: String): List<Float> =
        if (s.isBlank()) emptyList() else s.split(",").mapNotNull { it.toFloatOrNull() }

    @TypeConverter
    fun resultsToString(list: List<TaskResult>): String = JSONArray().apply {
        list.forEach {
            put(
                JSONObject()
                    .put("taskId", it.taskId)
                    .put("name", it.name)
                    .put("estimate", it.estimate.toDouble())
                    .put("actual", it.actual.toDouble())
                    .put("dread", it.dreadRating)
            )
        }
    }.toString()

    @TypeConverter
    fun stringToResults(s: String): List<TaskResult> {
        val arr = JSONArray(s)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TaskResult(
                taskId = o.optString("taskId"),
                name = o.getString("name"),
                estimate = o.getDouble("estimate").toFloat(),
                actual = o.getDouble("actual").toFloat(),
                dreadRating = o.optInt("dread"),
            )
        }
    }
}
