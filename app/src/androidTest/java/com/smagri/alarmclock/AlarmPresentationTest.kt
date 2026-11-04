package com.smagri.alarmclock

import android.app.NotificationManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.ringing.*
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import com.smagri.alarmclock.scheduling.AndroidAlarmScheduler
import java.io.File
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmPresentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val store = AlarmStore(File(context.filesDir, "alarms.json"))
    private val manager = context.getSystemService(NotificationManager::class.java)

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }

    @Before fun isolatedPermissions() {
        check(context.packageName == "com.smagri.alarmclock.stage4tests")
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        // Exercise the fallback without opening a full-screen activity over other tests.
        if (Build.VERSION.SDK_INT >= 34) shell("appops set ${context.packageName} USE_FULL_SCREEN_INTENT deny")
    }

    @Test fun fallbackNotificationHasOpenAndDismissActionsAndDismissDisablesOneTime() {
        val original = store.load()
        val at = System.currentTimeMillis() - 1_000
        val alarm = AlarmListItem("notification-test", LocalTime.of(7, 0), label = "Configured label",
            lastDeliveredAtMillis = at,
            activeOccurrences = listOf(AlarmOccurrence(at, LocalTime.of(6, 30), "Captured label")))
        try {
            store.save(listOf(alarm))
            AlarmNotifications.update(context, listOf(alarm), alert = true)
            val postedDeadline = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < postedDeadline && manager.activeNotifications.none { it.id == AlarmNotifications.NOTIFICATION_ID }) Thread.sleep(25)
            val notification = manager.activeNotifications.single { it.id == AlarmNotifications.NOTIFICATION_ID }.notification
            assertEquals(android.app.Notification.CATEGORY_ALARM, notification.category)
            assertEquals("Captured label", notification.extras.getString(android.app.Notification.EXTRA_TITLE))
            assertNotNull(notification.contentIntent)
            if (Build.VERSION.SDK_INT >= 34) assertNull(notification.fullScreenIntent)
            assertEquals("Dismiss", notification.actions.single().title.toString())
            notification.actions.single().actionIntent.send()
            val deadline = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < deadline && store.load().single().activeOccurrences.isNotEmpty()) Thread.sleep(25)
            assertTrue(store.load().single().activeOccurrences.isEmpty())
            assertFalse(store.load().single().enabled)
            // Posting/cancelling can lag the completed file write slightly.
            while (System.currentTimeMillis() < deadline && manager.activeNotifications.any { it.id == AlarmNotifications.NOTIFICATION_ID }) Thread.sleep(25)
            assertFalse(manager.activeNotifications.any { it.id == AlarmNotifications.NOTIFICATION_ID })
        } finally {
            store.save(original)
            AlarmNotifications.update(context, original)
        }
    }

    @Test fun staleNotificationDismissCannotDiscardCurrentAlarm() {
        val original = store.load()
        val at = System.currentTimeMillis() - 1_000
        val occurrence = AlarmOccurrence(at, LocalTime.of(7, 0), "Current")
        val alarm = AlarmListItem("current-notification", occurrence.time, lastDeliveredAtMillis = at,
            activeOccurrences = listOf(occurrence))
        val updated = CountDownLatch(1)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { updated.countDown() }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(AndroidAlarmScheduler.ACTION_UPDATED),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        try {
            store.save(listOf(alarm))
            val stale = ActiveAlarm(alarm.id, occurrence.copy(atMillis = at - 60_000))
            // Explicit old action leaves the actual active occurrence untouched.
            context.sendBroadcast(AlarmNotifications.dismissIntent(context, stale))
            assertTrue(updated.await(5, TimeUnit.SECONDS))
            assertEquals(listOf(alarm), store.load())
        } finally {
            context.unregisterReceiver(receiver)
            store.save(original)
            AlarmNotifications.update(context, original)
        }
    }
}
