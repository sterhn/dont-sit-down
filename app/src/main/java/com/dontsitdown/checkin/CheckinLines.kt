package com.dontsitdown.checkin

/** The check-in's voice: dry, warm one-liners. Same lines as the web app. */
object CheckinLines {
    private var last: String? = null

    /** Random line, never the same one twice in a row. */
    fun pick(list: List<String>): String {
        var s = list.random()
        if (list.size > 1 && s == last) s = list[(list.indexOf(s) + 1) % list.size]
        last = s
        return s
    }

    val MORNING = listOf(
        "noted. go be a person now",
        "morning logged. don't forget water",
        "got it. the day is yours",
        "saved. you're already ahead of yesterday-you",
        "cool. now close this app and do the thing",
        "morning check — done. coffee check — pending?",
        "okay, you're awake. that's step one",
    )
    val EVENING = listOf(
        "day's on record. rest now",
        "saved. tomorrow's a reset",
        "logged. you showed up today, that counts",
        "done. close your eyes before your phone closes them for you",
        "the day happened. you wrote it down. that's enough",
        "evening saved. the bed is right there",
    )
    val EMPTY = listOf(
        "blank check-in is just opening the app with extra steps",
        "you opened it, might as well fill something in",
        "the form is right there, just saying",
    )
    val STREAK = mapOf(
        7 to "a whole week. that's not nothing",
        14 to "two weeks. you're building something here",
        30 to "a month. i'm genuinely impressed",
        100 to "one hundred days. who even are you",
    )

    fun streakStarted(n: Int): String = when {
        STREAK[n] != null -> STREAK[n]!!
        n in Checkin.STREAK_MILESTONES -> "$n days in a row. look at you"
        n > 1 -> "day $n. still going"
        else -> "day one. streak started"
    }

    fun morningDone(hour: Int) = if (hour < 6) "you're either up early or haven't slept. either way — noted" else pick(MORNING)

    fun eveningDone(hour: Int, yesterday: Boolean) = when {
        hour < 2 -> "it's tomorrow already. save and sleep"
        hour < 6 -> "you're up late. this counts as dedication or denial"
        yesterday -> "yesterday's on record. better late than never"
        else -> pick(EVENING)
    }

    /** When the chores and the check-in meet: a line for the evening card. */
    fun choresLine(tasks: Int, minutes: Int, away: Boolean): String = when {
        away && tasks == 0 -> "not home today. the house can wait."
        tasks == 0 -> "nothing on the chore log."
        tasks == 1 -> "one thing done. $minutes min. it counts."
        else -> "$tasks things done in $minutes min. the couch lost today."
    }
}
