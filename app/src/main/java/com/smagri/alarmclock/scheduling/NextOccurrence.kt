package com.smagri.alarmclock.scheduling

import com.smagri.alarmclock.ui.alarms.AlarmListItem
import java.time.Instant
import java.time.ZoneId

object NextOccurrence {
    /** Strictly after now. DST gaps move forward; overlaps use the earlier offset once. */
    fun next(alarm: AlarmListItem, now: Instant, zone: ZoneId): Instant? {
        if (!alarm.enabled) return null
        val today = now.atZone(zone).toLocalDate()
        for (offset in 0L..8L) {
            val date = today.plusDays(offset)
            if (alarm.repeatDays.isNotEmpty() && date.dayOfWeek !in alarm.repeatDays) continue
            val candidate = date.atTime(alarm.time).atZone(zone)
            // A completely skipped calendar day must not move a weekly alarm to
            // a weekday the user did not select.
            if (candidate.toLocalDate() != date) continue
            if (candidate.toInstant().isAfter(now)) return candidate.toInstant()
        }
        error("No next occurrence found")
    }
}
