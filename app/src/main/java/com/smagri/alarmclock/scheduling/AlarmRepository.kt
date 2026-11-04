package com.smagri.alarmclock.scheduling

import com.smagri.alarmclock.data.AlarmStorage
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

data class ScheduleResult(
    val alarms: List<AlarmListItem>,
    val permissionGranted: Boolean,
    val schedulingFailed: Boolean = false,
)

/** Serializes UI and receiver updates. Call from an IO thread. */
class AlarmRepository(
    private val store: AlarmStorage,
    private val scheduler: AlarmScheduler,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    companion object { private val lock = Any() }

    fun reconcile(): ScheduleResult = synchronized(lock) {
        val saved = store.load()
        val planned = saved.map { alarm ->
            when {
                !alarm.enabled -> alarm.copy(scheduledAtMillis = null)
                alarm.scheduledAtMillis != null -> alarm
                alarm.repeatDays.isEmpty() && alarm.lastDeliveredAtMillis != null -> alarm
                else -> alarm.copy(scheduledAtMillis = next(alarm))
            }
        }
        commit(saved, planned, rescheduleAll = true)
    }

    fun saveConfigurations(desired: List<AlarmListItem>): ScheduleResult = synchronized(lock) {
        val saved = store.load()
        val byId = saved.associateBy { it.id }
        val planned = desired.map { requested ->
            val old = byId[requested.id]
            if (old != null && old.time == requested.time && old.repeatDays == requested.repeatDays &&
                old.enabled == requested.enabled) {
                // UI snapshots must not overwrite occurrence state advanced by a receiver.
                old.copy(label = requested.label)
            } else {
                requested.copy(scheduledAtMillis = next(requested), lastDeliveredAtMillis = null)
            }
        }
        commit(saved, planned)
    }

    fun delivered(id: String, triggerAtMillis: Long): ScheduleResult = synchronized(lock) {
        val saved = store.load()
        val alarm = saved.find { it.id == id }
        if (alarm == null || !alarm.enabled || alarm.scheduledAtMillis != triggerAtMillis ||
            clock.millis() < triggerAtMillis) {
            return@synchronized ScheduleResult(saved, scheduler.canSchedule())
        }
        val changed = alarm.copy(
            lastDeliveredAtMillis = triggerAtMillis,
            scheduledAtMillis = if (alarm.repeatDays.isEmpty()) null else next(alarm),
        )
        // One-time alarms remain enabled pending Stage 5 dismissal; they are not
        // silently re-armed for tomorrow by reopening the app.
        commit(saved, saved.map { if (it.id == id) changed else it })
    }

    private fun next(alarm: AlarmListItem): Long? =
        NextOccurrence.next(alarm, Instant.now(clock), zone())?.toEpochMilli()

    private fun commit(
        old: List<AlarmListItem>, planned: List<AlarmListItem>, rescheduleAll: Boolean = false,
    ): ScheduleResult {
        // If persistence fails, leave the OS schedules unchanged. If scheduling
        // fails afterwards, retain the intended configuration and report it so
        // reconcile can retry. Stale deliveries are rejected against this state.
        if (old != planned) store.save(planned)
        var failed = false
        val byId = planned.associateBy { it.id }
        old.forEach { alarm ->
            val at = alarm.scheduledAtMillis ?: return@forEach
            if (byId[alarm.id]?.scheduledAtMillis != at) {
                try { scheduler.cancel(alarm.id, at) } catch (_: RuntimeException) { failed = true }
            }
        }
        var allowed = scheduler.canSchedule()
        val oldById = old.associateBy { it.id }
        if (allowed) planned.forEach { alarm ->
            if (alarm.enabled && alarm.scheduledAtMillis != null &&
                (rescheduleAll || oldById[alarm.id]?.scheduledAtMillis != alarm.scheduledAtMillis)) {
                try {
                    scheduler.schedule(alarm.id, alarm.scheduledAtMillis)
                } catch (_: SecurityException) {
                    allowed = false
                } catch (_: RuntimeException) {
                    failed = true
                }
            }
        }
        return ScheduleResult(planned, allowed, failed)
    }
}
