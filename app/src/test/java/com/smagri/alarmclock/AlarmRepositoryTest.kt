package com.smagri.alarmclock

import com.smagri.alarmclock.data.AlarmStorage
import com.smagri.alarmclock.scheduling.AlarmRepository
import com.smagri.alarmclock.scheduling.AlarmScheduler
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import com.smagri.alarmclock.ringing.AlarmOccurrence
import com.smagri.alarmclock.ringing.activeAlarms
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class AlarmRepositoryTest {
    private class MemoryStore : AlarmStorage {
        var alarms = emptyList<AlarmListItem>()
        var fail = false
        override fun load() = alarms
        override fun save(alarms: List<AlarmListItem>) {
            if (fail) error("Storage unavailable")
            this.alarms = alarms
        }
    }
    private class Scheduler : AlarmScheduler {
        var allowed = true
        var fail = false
        val pending = mutableMapOf<String, Long>()
        val calls = mutableListOf<String>()
        override fun canSchedule() = allowed
        override fun schedule(alarmId: String, triggerAtMillis: Long) {
            if (fail) error("Scheduling unavailable")
            pending[alarmId] = triggerAtMillis
            calls += "schedule:$alarmId"
        }
        override fun cancel(alarmId: String, triggerAtMillis: Long) {
            if (pending[alarmId] == triggerAtMillis) pending.remove(alarmId)
            calls += "cancel:$alarmId"
        }
    }
    private class TestClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
        override fun instant(): Instant = now
    }
    private val store = MemoryStore()
    private val scheduler = Scheduler()
    private val clock = TestClock(Instant.parse("2026-11-04T06:00:00Z"))
    private val repo = AlarmRepository(store, scheduler, clock) { ZoneOffset.UTC }
    private val alarm = AlarmListItem("a", LocalTime.of(7, 0))

    @Test fun creatingAndReopeningKeepsSameOneTimeOccurrence() {
        val saved = repo.saveConfigurations(listOf(alarm)).alarms.single()
        assertEquals(Instant.parse("2026-11-04T07:00:00Z").toEpochMilli(), saved.scheduledAtMillis)
        clock.now = Instant.parse("2026-11-04T08:00:00Z")
        assertEquals(saved, repo.reconcile().alarms.single())
        assertEquals(saved.scheduledAtMillis, scheduler.pending[alarm.id])
    }

    @Test fun editingTimeReplacesOnlyThatAlarmAndLabelDoesNotReschedule() {
        val second = alarm.copy(id = "b", time = LocalTime.of(8, 0))
        val saved = repo.saveConfigurations(listOf(alarm, second)).alarms
        scheduler.calls.clear()
        val changed = repo.saveConfigurations(listOf(saved[0].copy(time = LocalTime.of(9, 0)), saved[1])).alarms
        assertEquals(listOf("cancel:a", "schedule:a"), scheduler.calls)
        assertEquals(saved[1], changed[1])
        scheduler.calls.clear()
        repo.saveConfigurations(changed.map { it.copy(label = "New label") })
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test fun disablingAndDeletionCancelPendingOccurrences() {
        var saved = repo.saveConfigurations(listOf(alarm)).alarms.single()
        saved = repo.saveConfigurations(listOf(saved.copy(enabled = false))).alarms.single()
        assertNull(saved.scheduledAtMillis)
        assertTrue(scheduler.pending.isEmpty())
        repo.saveConfigurations(listOf(saved.copy(enabled = true)))
        assertEquals(1, scheduler.pending.size)
        repo.saveConfigurations(emptyList())
        assertTrue(scheduler.pending.isEmpty())
        assertTrue(store.alarms.isEmpty())
    }

    @Test fun selectedWeekdayChangesReplaceSchedule() {
        val saved = repo.saveConfigurations(listOf(alarm)).alarms.single()
        val edited = repo.saveConfigurations(listOf(saved.copy(repeatDays = setOf(DayOfWeek.SUNDAY)))).alarms.single()
        assertEquals(Instant.parse("2026-11-08T07:00:00Z").toEpochMilli(), edited.scheduledAtMillis)
    }

    @Test fun receiverAdvancesRepeatingAlarmAndRejectsDuplicateDelivery() {
        val saved = repo.saveConfigurations(listOf(alarm.copy(repeatDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)))).alarms.single()
        clock.now = Instant.ofEpochMilli(saved.scheduledAtMillis!!)
        val next = repo.delivered(saved.id, saved.scheduledAtMillis).alarms.single()
        assertEquals(Instant.parse("2026-11-06T07:00:00Z").toEpochMilli(), next.scheduledAtMillis)
        assertEquals(saved.scheduledAtMillis, next.lastDeliveredAtMillis)
        scheduler.calls.clear()
        assertEquals(next, repo.delivered(saved.id, saved.scheduledAtMillis).alarms.single())
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test fun deliveredOneTimeDoesNotRepeatAndReenablingSchedulesNextOccurrence() {
        val saved = repo.saveConfigurations(listOf(alarm)).alarms.single()
        clock.now = Instant.ofEpochMilli(saved.scheduledAtMillis!!)
        val delivered = repo.delivered(saved.id, saved.scheduledAtMillis).alarms.single()
        assertTrue(delivered.enabled) // Dismissal, not delivery, disables it in Stage 5.
        assertNull(delivered.scheduledAtMillis)
        assertEquals(delivered, repo.reconcile().alarms.single())
        repo.saveConfigurations(listOf(delivered.copy(enabled = false)))
        val enabled = repo.saveConfigurations(listOf(delivered.copy(enabled = true))).alarms.single()
        assertEquals(Instant.parse("2026-11-05T07:00:00Z").toEpochMilli(), enabled.scheduledAtMillis)
        assertNull(enabled.lastDeliveredAtMillis)
    }

    @Test fun staleEarlyDisabledAndDeletedDeliveriesAreIgnored() {
        val saved = repo.saveConfigurations(listOf(alarm)).alarms.single()
        assertEquals(saved, repo.delivered(saved.id, saved.scheduledAtMillis!!).alarms.single())
        clock.now = Instant.parse("2026-11-04T10:00:00Z")
        val edited = repo.saveConfigurations(listOf(saved.copy(time = LocalTime.of(11, 0)))).alarms.single()
        assertEquals(edited, repo.delivered(saved.id, saved.scheduledAtMillis).alarms.single())
        val disabled = repo.saveConfigurations(listOf(edited.copy(enabled = false))).alarms.single()
        assertEquals(disabled, repo.delivered(edited.id, edited.scheduledAtMillis!!).alarms.single())
        repo.saveConfigurations(emptyList())
        assertTrue(repo.delivered(edited.id, edited.scheduledAtMillis).alarms.isEmpty())
    }

    @Test fun permissionDenialPreservesConfigurationAndGrantRestoresScheduling() {
        scheduler.allowed = false
        val result = repo.saveConfigurations(listOf(alarm))
        assertFalse(result.permissionGranted)
        assertEquals(1, store.alarms.size)
        assertTrue(scheduler.pending.isEmpty())
        scheduler.allowed = true
        assertTrue(repo.reconcile().permissionGranted)
        assertEquals(result.alarms.single().scheduledAtMillis, scheduler.pending[alarm.id])
    }

    @Test fun failedStorageLeavesSchedulesUntouched() {
        val saved = repo.saveConfigurations(listOf(alarm)).alarms
        scheduler.calls.clear()
        store.fail = true
        assertThrows(IllegalStateException::class.java) { repo.saveConfigurations(emptyList()) }
        assertEquals(saved, store.alarms)
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test fun schedulingFailureIsReportedAndRetriedWithoutLosingData() {
        scheduler.fail = true
        val result = repo.saveConfigurations(listOf(alarm))
        assertTrue(result.schedulingFailed)
        assertEquals(result.alarms, store.alarms)
        scheduler.fail = false
        assertFalse(repo.reconcile().schedulingFailed)
        assertEquals(1, scheduler.pending.size)
    }

    @Test fun staleUiSnapshotCannotUndoReceiverProgress() {
        val snapshot = repo.saveConfigurations(listOf(alarm)).alarms.single()
        clock.now = Instant.ofEpochMilli(snapshot.scheduledAtMillis!!)
        repo.delivered(snapshot.id, snapshot.scheduledAtMillis)
        val edited = repo.saveConfigurations(listOf(snapshot.copy(label = "Edited label"))).alarms.single()
        assertNull(edited.scheduledAtMillis)
        assertEquals(snapshot.scheduledAtMillis, edited.lastDeliveredAtMillis)
        assertEquals("Edited label", edited.label)
    }

    @Test fun legacyConfigurationsAreScheduledOnFirstReconcile() {
        store.alarms = listOf(alarm, alarm.copy(id = "disabled", enabled = false))
        val result = repo.reconcile()
        assertNotNull(result.alarms.first().scheduledAtMillis)
        assertNull(result.alarms.last().scheduledAtMillis)
        assertEquals(setOf("a"), scheduler.pending.keys)
    }

    private fun deliver(alarm: AlarmListItem): AlarmListItem {
        val at = requireNotNull(alarm.scheduledAtMillis)
        clock.now = Instant.ofEpochMilli(at)
        return repo.delivered(alarm.id, at).alarms.single { it.id == alarm.id }
    }

    @Test fun deliveryCapturesTimeAndLabelWithoutDuplicatingActiveOccurrence() {
        val pending = repo.saveConfigurations(listOf(alarm.copy(label = "Captured"))).alarms.single()
        val delivered = deliver(pending)
        assertEquals(listOf(AlarmOccurrence(pending.scheduledAtMillis!!, alarm.time, "Captured")), delivered.activeOccurrences)
        assertTrue(repo.delivered(alarm.id, pending.scheduledAtMillis).alarms.single().activeOccurrences.size == 1)
    }

    @Test fun dismissOneTimeDisablesButKeepsConfiguration() {
        val active = deliver(repo.saveConfigurations(listOf(alarm)).alarms.single())
        val result = repo.dismiss(active.id, active.activeOccurrences.single().atMillis).alarms.single()
        assertFalse(result.enabled)
        assertNull(result.scheduledAtMillis)
        assertTrue(result.activeOccurrences.isEmpty())
        assertEquals(alarm.id, result.id)
        assertEquals(result, repo.reconcile().alarms.single())
    }

    @Test fun dismissRepeatingKeepsEnabledAndNextSelectedOccurrence() {
        val active = deliver(repo.saveConfigurations(listOf(alarm.copy(repeatDays = setOf(DayOfWeek.WEDNESDAY)))).alarms.single())
        val next = active.scheduledAtMillis
        val dismissed = repo.dismiss(active.id, active.activeOccurrences.single().atMillis).alarms.single()
        assertTrue(dismissed.enabled)
        assertTrue(dismissed.activeOccurrences.isEmpty())
        assertEquals(next, dismissed.scheduledAtMillis)
        assertEquals(next, scheduler.pending[alarm.id])
    }

    @Test fun staleDismissDoesNotDismissAnotherOccurrence() {
        val active = deliver(repo.saveConfigurations(listOf(alarm)).alarms.single())
        assertEquals(active, repo.dismiss(active.id, active.lastDeliveredAtMillis!! - 1).alarms.single())
        assertEquals(active, repo.dismiss("different", active.lastDeliveredAtMillis).alarms.single())
    }

    @Test fun failedDismissKeepsActiveStateAndEnabledFlag() {
        val active = deliver(repo.saveConfigurations(listOf(alarm)).alarms.single())
        store.fail = true
        assertThrows(IllegalStateException::class.java) { repo.dismiss(active.id, active.lastDeliveredAtMillis!!) }
        assertEquals(active, store.alarms.single())
    }

    @Test fun disablingOrDeletingAnActiveAlarmRemovesItsActiveState() {
        val active = deliver(repo.saveConfigurations(listOf(alarm)).alarms.single())
        val disabled = repo.saveConfigurations(listOf(active.copy(enabled = false))).alarms.single()
        assertTrue(disabled.activeOccurrences.isEmpty())
        repo.saveConfigurations(emptyList())
        assertTrue(activeAlarms(store.alarms).isEmpty())
    }

    @Test fun editingActiveConfigurationKeepsSnapshotAndDismissDoesNotCancelNewSchedule() {
        val active = deliver(repo.saveConfigurations(listOf(alarm.copy(label = "Original"))).alarms.single())
        val edited = repo.saveConfigurations(listOf(active.copy(time = LocalTime.of(8, 30), label = "Edited"))).alarms.single()
        assertEquals("Original", edited.activeOccurrences.single().label)
        assertEquals(LocalTime.of(7, 0), edited.activeOccurrences.single().time)
        val dismissed = repo.dismiss(edited.id, active.lastDeliveredAtMillis!!).alarms.single()
        assertTrue(dismissed.enabled)
        assertEquals(edited.scheduledAtMillis, dismissed.scheduledAtMillis)
    }

    @Test fun multipleDueAlarmsAreRetainedAndDismissedIndependently() {
        val saved = repo.saveConfigurations(listOf(alarm, alarm.copy(id = "b"))).alarms
        deliver(saved[0]); deliver(saved[1])
        val queue = activeAlarms(store.alarms)
        assertEquals(listOf("a", "b"), queue.map { it.alarmId })
        repo.dismiss(queue[0].alarmId, queue[0].occurrence.atMillis)
        assertEquals(listOf("b"), activeAlarms(store.alarms).map { it.alarmId })
        assertFalse(store.alarms.first().enabled)
        assertTrue(store.alarms.last().enabled)
    }

    @Test fun repeatedOccurrencesOfSameAlarmAreNotLostWhileWaiting() {
        val first = deliver(repo.saveConfigurations(listOf(alarm.copy(repeatDays = DayOfWeek.entries.toSet()))).alarms.single())
        val second = deliver(first)
        assertEquals(2, second.activeOccurrences.size)
        repo.dismiss(alarm.id, first.activeOccurrences.single().atMillis)
        val remaining = store.alarms.single()
        assertEquals(listOf(second.lastDeliveredAtMillis), remaining.activeOccurrences.map { it.atMillis })
        assertTrue(remaining.enabled)
    }
}
