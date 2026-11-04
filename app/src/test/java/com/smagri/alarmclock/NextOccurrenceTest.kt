package com.smagri.alarmclock

import com.smagri.alarmclock.scheduling.NextOccurrence
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class NextOccurrenceTest {
    private val utc = ZoneId.of("UTC")
    private val alarm = AlarmListItem("a", LocalTime.of(7, 15))

    @Test fun disabledHasNoOccurrence() {
        assertNull(NextOccurrence.next(alarm.copy(enabled = false), Instant.EPOCH, utc))
    }

    @Test fun oneTimeUsesTodayWhenFutureAndTomorrowWhenPassedOrEqual() {
        assertEquals(Instant.parse("2026-11-04T07:15:00Z"),
            NextOccurrence.next(alarm, Instant.parse("2026-11-04T07:14:59Z"), utc))
        listOf("2026-11-04T07:15:00Z", "2026-11-04T20:00:00Z").forEach {
            assertEquals(Instant.parse("2026-11-05T07:15:00Z"), NextOccurrence.next(alarm, Instant.parse(it), utc))
        }
    }

    @Test fun independentlySelectedDaysAndWeekWrap() {
        val weekly = alarm.copy(repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY))
        assertEquals(Instant.parse("2026-11-05T07:15:00Z"),
            NextOccurrence.next(weekly, Instant.parse("2026-11-04T08:00:00Z"), utc))
        assertEquals(Instant.parse("2026-11-08T07:15:00Z"),
            NextOccurrence.next(weekly, Instant.parse("2026-11-05T07:15:00Z"), utc))
        assertEquals(Instant.parse("2026-11-09T07:15:00Z"),
            NextOccurrence.next(weekly, Instant.parse("2026-11-08T08:00:00Z"), utc))
    }

    @Test fun singleWeekdayWrapsSevenDays() {
        assertEquals(Instant.parse("2026-11-11T07:15:00Z"), NextOccurrence.next(
            alarm.copy(repeatDays = setOf(DayOfWeek.WEDNESDAY)), Instant.parse("2026-11-04T07:15:00Z"), utc))
    }

    @Test fun midnightCrossesYearBoundary() {
        assertEquals(Instant.parse("2027-01-01T00:00:00Z"), NextOccurrence.next(
            alarm.copy(time = LocalTime.MIDNIGHT), Instant.parse("2026-12-31T23:59:59Z"), utc))
    }

    @Test fun localWallClockTimeUsesCurrentZone() {
        val now = Instant.parse("2026-11-03T18:00:00Z")
        for (name in listOf("Australia/Sydney", "Australia/Perth")) {
            val zone = ZoneId.of(name)
            val result = NextOccurrence.next(alarm, now, zone)!!.atZone(zone)
            assertEquals(alarm.time, result.toLocalTime())
            assertEquals(LocalDate.of(2026, 11, 4), result.toLocalDate())
        }
    }

    @Test fun daylightSavingGapMovesForwardByGapLength() {
        val zone = ZoneId.of("Australia/Sydney")
        val now = ZonedDateTime.of(2026, 10, 4, 0, 0, 0, 0, zone).toInstant()
        val result = NextOccurrence.next(alarm.copy(time = LocalTime.of(2, 30)), now, zone)!!.atZone(zone)
        assertEquals(LocalTime.of(3, 30), result.toLocalTime())
        assertEquals(LocalDate.of(2026, 10, 4), result.toLocalDate())
    }

    @Test fun daylightSavingOverlapUsesEarlierOffsetOnlyOnce() {
        val zone = ZoneId.of("Australia/Sydney")
        val sunday = alarm.copy(time = LocalTime.of(2, 30), repeatDays = setOf(DayOfWeek.SUNDAY))
        val now = ZonedDateTime.of(2026, 4, 5, 0, 0, 0, 0, zone).toInstant()
        val first = NextOccurrence.next(sunday, now, zone)!!
        assertEquals(ZoneOffset.ofHours(11), first.atZone(zone).offset)
        assertEquals(LocalDate.of(2026, 4, 12),
            NextOccurrence.next(sunday, first.plusSeconds(1), zone)!!.atZone(zone).toLocalDate())
    }

    @Test fun skippedCalendarDayDoesNotRunOnUnselectedWeekday() {
        val zone = ZoneId.of("Pacific/Apia")
        val now = ZonedDateTime.of(2011, 12, 29, 12, 0, 0, 0, zone).toInstant()
        val friday = alarm.copy(repeatDays = setOf(DayOfWeek.FRIDAY))
        assertEquals(LocalDate.of(2012, 1, 6), NextOccurrence.next(friday, now, zone)!!.atZone(zone).toLocalDate())
    }
}
