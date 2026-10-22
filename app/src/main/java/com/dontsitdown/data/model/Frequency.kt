package com.dontsitdown.data.model

enum class Frequency(val label: String, val intervalDays: Int?) {
    DAILY("daily", 1),
    EVERY_FEW_DAYS("every few days", 3),
    WEEKLY("weekly", 7),
    BIWEEKLY("every 2 weeks", 14),
    MONTHLY("monthly", 30),
    AS_NEEDED("as needed", null),
}
