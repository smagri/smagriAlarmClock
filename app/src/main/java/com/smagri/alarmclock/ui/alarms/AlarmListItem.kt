package com.smagri.alarmclock.ui.alarms

import java.time.DayOfWeek
import java.time.LocalTime

/** Display data only; these samples do not schedule alarms. */
data class AlarmListItem(
    val id: Long,
    val time: LocalTime,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    val enabled: Boolean = true,
)

val sampleAlarms = listOf(
    AlarmListItem(1, LocalTime.of(7, 0), DayOfWeek.entries.toSet(), "Wake up"),
    AlarmListItem(2, LocalTime.of(7, 30), DayOfWeek.entries.take(5).toSet(), "Exercise"),
    AlarmListItem(3, LocalTime.of(12, 0), enabled = false),
    AlarmListItem(4, LocalTime.of(18, 15), setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), "Evening walk"),
    AlarmListItem(5, LocalTime.of(21, 0), DayOfWeek.entries.toSet(), "Medication"),
)
