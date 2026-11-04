package com.smagri.alarmclock

import android.app.PendingIntent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.scheduling.AndroidAlarmScheduler
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import com.smagri.alarmclock.ringing.AlarmNotifications
import java.io.File
import java.time.LocalTime
import java.util.UUID
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAlarmSchedulerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val scheduler = AndroidAlarmScheduler(context)

    @Before fun requireIsolatedTargetAndGrantAccess() {
        // Never grant permissions to or schedule test alarms in the personal app.
        check(context.packageName == "com.smagri.alarmclock.stage4tests") {
            "Run with -I tools/isolated-android-tests.init.gradle"
        }
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow",
        )).use { it.readBytes() }
        assertTrue(scheduler.canSchedule())
    }

    @Test fun occurrenceIntentsAreDistinctAndCancellationIsIndependent() {
        val first = "Aa-${UUID.randomUUID()}"
        val second = "BB-${UUID.randomUUID()}"
        val at = System.currentTimeMillis() + 120_000
        fun exists(id: String, time: Long) = PendingIntent.getBroadcast(context, 0,
            scheduler.intent(id, time), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) != null
        try {
            scheduler.schedule(first, at)
            scheduler.schedule(second, at)
            assertTrue(exists(first, at))
            assertTrue(exists(second, at))
            assertFalse(exists(first, at + 60_000))
            scheduler.cancel(first, at)
            assertFalse(exists(first, at))
            assertTrue(exists(second, at))
        } finally {
            scheduler.cancel(first, at)
            scheduler.cancel(second, at)
        }
    }

    @Test fun realAlarmManagerDeliveryRecordsOneTimeWithoutRearmingIt() {
        val store = AlarmStore(File(context.filesDir, "alarms.json"))
        val original = store.load()
        val at = System.currentTimeMillis() + 4_000
        val id = "delivery-test-${UUID.randomUUID()}"
        val alarm = AlarmListItem(id, LocalTime.of(7, 0), scheduledAtMillis = at)
        try {
            store.save(original + alarm)
            scheduler.schedule(id, at)
            val deadline = System.currentTimeMillis() + 12_000
            var delivered: AlarmListItem? = null
            while (System.currentTimeMillis() < deadline) {
                delivered = store.load().find { it.id == id }
                if (delivered?.lastDeliveredAtMillis == at) break
                Thread.sleep(50)
            }
            assertEquals(at, delivered?.lastDeliveredAtMillis)
            assertNull(delivered?.scheduledAtMillis)
            assertTrue(delivered!!.enabled)
            assertEquals(listOf(at), delivered.activeOccurrences.map { it.atMillis })
        } finally {
            scheduler.cancel(id, at)
            store.save(original)
            AlarmNotifications.update(context, original)
        }
    }
}
