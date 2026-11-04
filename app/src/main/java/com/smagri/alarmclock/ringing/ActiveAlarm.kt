package com.smagri.alarmclock.ringing

import com.smagri.alarmclock.ui.alarms.AlarmListItem
import java.time.LocalTime

/** Snapshot the delivered occurrence so later edits do not change its display. */
data class AlarmOccurrence(val atMillis: Long, val time: LocalTime, val label: String)

data class ActiveAlarm(val alarmId: String, val occurrence: AlarmOccurrence)

fun activeAlarms(alarms: List<AlarmListItem>): List<ActiveAlarm> = alarms
    .flatMap { alarm -> alarm.activeOccurrences.map { ActiveAlarm(alarm.id, it) } }
    .sortedWith(compareBy<ActiveAlarm> { it.occurrence.atMillis }.thenBy { it.alarmId })
