package com.dontsitdown.data

import com.dontsitdown.data.model.Frequency.AS_NEEDED
import com.dontsitdown.data.model.Frequency.BIWEEKLY
import com.dontsitdown.data.model.Frequency.DAILY
import com.dontsitdown.data.model.Frequency.EVERY_FEW_DAYS
import com.dontsitdown.data.model.Frequency.MONTHLY
import com.dontsitdown.data.model.Frequency.WEEKLY
import com.dontsitdown.data.model.Task

object DefaultTasks {
    fun create(now: Long = System.currentTimeMillis()): List<Task> = listOf(
        // quick
        "take out trash" to (3 to EVERY_FEW_DAYS),
        "wipe counters" to (4 to DAILY),
        "take out recycling" to (4 to EVERY_FEW_DAYS),
        "wipe stove" to (5 to EVERY_FEW_DAYS),
        "water plants" to (3 to WEEKLY),
        "put away clean laundry" to (5 to AS_NEEDED),
        // medium
        "dishes" to (8 to DAILY),
        "quick tidy" to (10 to DAILY),
        "sweep kitchen" to (6 to EVERY_FEW_DAYS),
        "clean bathroom sink" to (5 to WEEKLY),
        "vacuum" to (12 to WEEKLY),
        "change towels" to (5 to WEEKLY),
        "load/switch laundry" to (5 to AS_NEEDED),
        // longer
        "change sheets" to (15 to WEEKLY),
        "mop floors" to (20 to BIWEEKLY),
        "clean toilet" to (10 to WEEKLY),
        "clean shower/tub" to (15 to BIWEEKLY),
        "deep clean fridge" to (20 to MONTHLY),
        "organize a drawer/shelf" to (15 to MONTHLY),
    ).mapIndexed { i, (name, spec) ->
        Task(name = name, estimateMinutes = spec.first, frequency = spec.second, createdAt = now, sortOrder = i)
    }

    /** Group headings used on first launch, matching the default list order above. */
    val groups = listOf("quick" to 0..5, "medium" to 6..12, "longer" to 13..18)
    const val COUNT = 19
}
