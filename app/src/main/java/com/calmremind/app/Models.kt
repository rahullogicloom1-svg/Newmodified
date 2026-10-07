package com.calmremind.app

import org.json.JSONArray
import org.json.JSONObject

enum class ReminderType(val label: String, val emoji: String) {
    WATER("Water", "💧"),
    MEDICINE("Medicine", "💊"),
    CUSTOM("Custom", "🔔");
}

/** INTERVAL = "every X hours between start and end", TIMES = "at these exact times". */
enum class ScheduleMode { INTERVAL, TIMES }

/** DAILY = every day, ALTERNATE = every other day, WEEKDAYS = only the chosen weekdays. */
enum class RepeatMode { DAILY, ALTERNATE, WEEKDAYS }

private inline fun <reified T : Enum<T>> enumOr(value: String, default: T): T =
    try {
        enumValueOf<T>(value)
    } catch (e: IllegalArgumentException) {
        default
    }

data class Reminder(
    val id: Int,
    val name: String,
    val type: ReminderType,
    val mode: ScheduleMode,
    /** Minutes between doses (INTERVAL mode). */
    val intervalMinutes: Int,
    /** First dose of the day, minutes after midnight (INTERVAL mode). */
    val windowStart: Int,
    /** Last allowed dose of the day, minutes after midnight (INTERVAL mode). */
    val windowEnd: Int,
    /** Exact times, minutes after midnight (TIMES mode). */
    val times: List<Int>,
    val repeat: RepeatMode,
    /** java.time.DayOfWeek values: 1 = Monday ... 7 = Sunday (WEEKDAYS repeat). */
    val weekdays: Set<Int>,
    /** Epoch day the "every other day" cycle counts from. */
    val anchorEpochDay: Long,
    val note: String,
    val enabled: Boolean,
    val createdAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("mode", mode.name)
        put("interval", intervalMinutes)
        put("wStart", windowStart)
        put("wEnd", windowEnd)
        put("times", JSONArray(times))
        put("repeat", repeat.name)
        put("weekdays", JSONArray(weekdays.toList()))
        put("anchor", anchorEpochDay)
        put("note", note)
        put("enabled", enabled)
        put("created", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject): Reminder {
            fun ints(key: String): List<Int> {
                val a = o.optJSONArray(key) ?: return emptyList()
                return (0 until a.length()).map { a.getInt(it) }
            }
            return Reminder(
                id = o.getInt("id"),
                name = o.optString("name", "Reminder"),
                type = enumOr(o.optString("type"), ReminderType.CUSTOM),
                mode = enumOr(o.optString("mode"), ScheduleMode.TIMES),
                intervalMinutes = o.optInt("interval", 120),
                windowStart = o.optInt("wStart", 8 * 60),
                windowEnd = o.optInt("wEnd", 22 * 60),
                times = ints("times"),
                repeat = enumOr(o.optString("repeat"), RepeatMode.DAILY),
                weekdays = ints("weekdays").toSet(),
                anchorEpochDay = o.optLong("anchor", 0L),
                note = o.optString("note", ""),
                enabled = o.optBoolean("enabled", true),
                createdAt = o.optLong("created", 0L)
            )
        }
    }
}

/** One "I did it" record. [slotMillis] is the scheduled time of the dose that was ticked off. */
data class LogEntry(
    val reminderId: Int,
    val name: String,
    val emoji: String,
    val slotMillis: Long,
    val doneMillis: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("rid", reminderId)
        put("name", name)
        put("emoji", emoji)
        put("slot", slotMillis)
        put("done", doneMillis)
    }

    companion object {
        fun fromJson(o: JSONObject) = LogEntry(
            reminderId = o.getInt("rid"),
            name = o.optString("name", ""),
            emoji = o.optString("emoji", "🔔"),
            slotMillis = o.optLong("slot", 0L),
            doneMillis = o.optLong("done", 0L)
        )
    }
}
