package com.dontsitdown.checkin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.math.roundToInt

/*
 * The daily check-in, carried over from the web app. Same shape as the web app's
 * export file, so its JSON imports straight in and an export here opens there too.
 * Dates are "yyyy-MM-dd" keys, like LocalDate.toString().
 */

@Serializable
data class Morning(
    val sleep: Int? = null,
    val mood: Int? = null,
    val body: Int? = null,
    val intention: String = "",
    val savedAt: String? = null,
)

@Serializable
data class Evening(
    val mood: Int? = null,
    val body: Int? = null,
    /** boundary name → "held" | "slipped" | null (asked, not answered). */
    val boundaries: Map<String, String?> = emptyMap(),
    /** "went" | "rest" | "skipped" | null */
    val gym: String? = null,
    val happened: String = "",
    val honest: String = "",
    val spent: String = "",
    val savedAt: String? = null,
)

@Serializable
data class Day(val morning: Morning? = null, val evening: Evening? = null)

@Serializable
data class Goal(
    val id: String,
    val title: String,
    /** "week" | "month" */
    val period: String = "week",
    val target: Int = 1,
    /** "place" | "food" | "book" | "star" */
    val icon: String = "star",
    val prompt: String? = null,
    val tags: List<String>? = null,
    val currentLabel: String? = null,
    val current: String? = null,
) {
    val monthly: Boolean get() = period == "month"
}

@Serializable
data class LogEntry(
    val id: String,
    val goal: String,
    val date: String,
    val text: String,
    val tag: String? = null,
)

@Serializable
data class CheckinData(
    val boundaries: List<String> = DEFAULT_BOUNDARIES,
    val goals: List<Goal> = defaultGoals(),
    val log: List<LogEntry> = emptyList(),
    val days: Map<String, Day> = emptyMap(),
) {
    fun day(key: String): Day = days[key] ?: Day()

    companion object {
        val DEFAULT_BOUNDARIES = listOf("22:00 cutoff", "no solo takeout", "bedtime before midnight")

        fun defaultGoals() = listOf(
            Goal("new-place", "Go somewhere new", "week", 1, "place", prompt = "Where did you go?"),
            Goal("new-food", "Eat something new", "week", 1, "food", prompt = "What did you eat?", tags = listOf("ordered", "cooked")),
            Goal("book", "Read a book", "month", 1, "book", prompt = "Which book did you finish?", currentLabel = "Currently reading", current = ""),
        )
    }
}

object CheckinJson {
    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val DATE_RE = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val ICONS = setOf("place", "food", "book", "star")

    fun encode(data: CheckinData): String = json.encodeToString(CheckinData.serializer(), data)

    /** Lenient read of our own file or the web app's export. Null if it doesn't look like check-in data at all. */
    fun decode(text: String): CheckinData? = runCatching { normalize(json.parseToJsonElement(text)) }.getOrNull()

    /** A port of the web app's normalizeImportedState: keep what's recognisable, drop the rest, never throw. */
    fun normalize(raw: JsonElement): CheckinData? {
        val top = raw as? JsonObject ?: return null
        // A full app backup: take its check-in half.
        val o = if ((top["kind"] as? JsonPrimitive)?.content == "still-going-backup") top["checkin"] as? JsonObject ?: return null else top
        val rawDays = o["days"] as? JsonObject ?: return null

        val boundaries = (o["boundaries"] as? JsonArray)?.mapNotNull { it.str() } ?: CheckinData.DEFAULT_BOUNDARIES

        val days = buildMap {
            for ((key, rec) in rawDays) {
                if (!DATE_RE.matches(key) || rec !is JsonObject) continue
                val m = (rec["morning"] as? JsonObject)?.let { m ->
                    Morning(
                        sleep = m["sleep"].score(),
                        mood = m["mood"].score(),
                        body = m["body"].score(),
                        intention = m["intention"].str() ?: "",
                        savedAt = m["savedAt"].str(),
                    )
                }
                val e = (rec["evening"] as? JsonObject)?.let { e ->
                    val bounds = (e["boundaries"] as? JsonObject)?.mapValues { (_, v) -> v.str()?.takeIf { it == "held" || it == "slipped" } } ?: emptyMap()
                    Evening(
                        mood = e["mood"].score(),
                        body = e["body"].score(),
                        boundaries = bounds,
                        gym = e["gym"].str()?.takeIf { it in setOf("went", "rest", "skipped") },
                        happened = e["happened"].str() ?: "",
                        honest = e["honest"].str() ?: "",
                        spent = e["spent"].str() ?: "",
                        savedAt = e["savedAt"].str(),
                    )
                }
                put(key, Day(m, e))
            }
        }

        // Older exports have no goals: keep the defaults.
        val goals = (o["goals"] as? JsonArray)?.mapNotNull { g ->
            g as? JsonObject ?: return@mapNotNull null
            val id = g["id"].str() ?: return@mapNotNull null
            val title = g["title"].str() ?: return@mapNotNull null
            val target = (g["target"] as? JsonPrimitive)?.intOrNull?.takeIf { it in 1..31 } ?: 1
            val label = g["currentLabel"].str()
            Goal(
                id = id,
                title = title,
                period = if (g["period"].str() == "month") "month" else "week",
                target = target,
                icon = g["icon"].str()?.takeIf { it in ICONS } ?: "star",
                prompt = g["prompt"].str(),
                tags = (g["tags"] as? JsonArray)?.mapNotNull { it.str() },
                currentLabel = label,
                current = if (label != null) g["current"].str() ?: "" else null,
            )
        } ?: CheckinData.defaultGoals()

        val log = (o["log"] as? JsonArray)?.mapNotNull { e ->
            e as? JsonObject ?: return@mapNotNull null
            val goal = e["goal"].str() ?: return@mapNotNull null
            val text = e["text"].str() ?: return@mapNotNull null
            val date = e["date"].str()?.takeIf { DATE_RE.matches(it) } ?: return@mapNotNull null
            LogEntry(e["id"].str() ?: Checkin.uid(), goal, date, text, e["tag"].str())
        } ?: emptyList()

        return CheckinData(boundaries, goals, log, days)
    }

    private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** 1..10, or null. The web app only ever stored whole numbers, but be kind to hand-edited files. */
    private fun JsonElement?.score(): Int? {
        if (this == null || this is JsonNull) return null
        val d = (this as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull ?: return null
        if (!d.isFinite()) return null
        return d.roundToInt().coerceIn(1, 10)
    }
}
