package com.calmremind.app

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Pure schedule maths: which doses happen on a given day, and when the next one is due. */
object Schedule {

    fun occursOn(r: Reminder, date: LocalDate): Boolean = when (r.repeat) {
        RepeatMode.DAILY -> true
        RepeatMode.ALTERNATE -> Math.floorMod(date.toEpochDay() - r.anchorEpochDay, 2L) == 0L
        RepeatMode.WEEKDAYS -> date.dayOfWeek.value in r.weekdays
    }

    fun intervalDoses(start: Int, end: Int, step: Int): List<Int> {
        val s = step.coerceAtLeast(5)
        val out = ArrayList<Int>()
        var t = start
        while (t <= end && t < 24 * 60) {
            out.add(t)
            t += s
        }
        return out
    }

    /** Minutes-after-midnight of every dose on [date] (empty if the reminder skips that day). */
    fun dosesOn(r: Reminder, date: LocalDate): List<Int> {
        if (!occursOn(r, date)) return emptyList()
        return when (r.mode) {
            ScheduleMode.TIMES -> r.times.distinct().sorted()
            ScheduleMode.INTERVAL -> intervalDoses(r.windowStart, r.windowEnd, r.intervalMinutes)
        }
    }

    fun slotMillis(date: LocalDate, minute: Int, zone: ZoneId): Long =
        date.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()

    fun slotsOn(r: Reminder, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Long> =
        dosesOn(r, date).map { slotMillis(date, it, zone) }

    /** First dose strictly after [after], looking up to two weeks ahead. Null if none / disabled. */
    fun nextTrigger(r: Reminder, after: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!r.enabled) return null
        val start = Instant.ofEpochMilli(after).atZone(zone).toLocalDate()
        for (d in 0..14) {
            val date = start.plusDays(d.toLong())
            for (m in dosesOn(r, date)) {
                val millis = slotMillis(date, m, zone)
                if (millis > after) return millis
            }
        }
        return null
    }
}

// ---------- Display helpers ----------

fun fmtTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, if (h < 12) "AM" else "PM")
}

fun fmtInterval(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

fun defaultName(t: ReminderType): String = when (t) {
    ReminderType.WATER -> "Drink water"
    ReminderType.MEDICINE -> "Take medicine"
    ReminderType.CUSTOM -> "My reminder"
}

fun repeatText(r: Reminder): String = when (r.repeat) {
    RepeatMode.DAILY -> "Every day"
    RepeatMode.ALTERNATE -> "Every other day"
    RepeatMode.WEEKDAYS -> r.weekdays.sorted().joinToString(", ") {
        DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}

fun describe(r: Reminder): String {
    val whenText = when (r.mode) {
        ScheduleMode.INTERVAL ->
            "Every ${fmtInterval(r.intervalMinutes)} · ${fmtTime(r.windowStart)} – ${fmtTime(r.windowEnd)}"
        ScheduleMode.TIMES ->
            if (r.times.isEmpty()) "No times set" else "At " + r.times.sorted().joinToString(", ") { fmtTime(it) }
    }
    return "$whenText · ${repeatText(r)}"
}
