package com.smagri.alarmclock

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.ringing.*
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import java.io.File
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveAlarmActivityTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun activeStateSurvivesRecreationAndReopeningThenDismissesOneByOne() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.smagri.alarmclock.stage4tests")
        val store = AlarmStore(File(context.filesDir, "alarms.json"))
        val original = store.load()
        val at = System.currentTimeMillis() - 1_000
        val first = AlarmListItem("active-a", LocalTime.of(20, 0), label = "Edited configuration",
            lastDeliveredAtMillis = at,
            activeOccurrences = listOf(AlarmOccurrence(at, LocalTime.of(7, 0), "First active")))
        val second = AlarmListItem("active-b", LocalTime.of(8, 0), lastDeliveredAtMillis = at + 1,
            activeOccurrences = listOf(AlarmOccurrence(at + 1, LocalTime.of(8, 0), "Second active")))
        var scenario: ActivityScenario<ActiveAlarmActivity>? = null
        fun waitFor(text: String) {
            compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        }
        try {
            store.save(listOf(first, second))
            scenario = ActivityScenario.launch(Intent(context, ActiveAlarmActivity::class.java))
            waitFor("First active")
            compose.onNodeWithText("Edited configuration").assertDoesNotExist()
            scenario.recreate()
            waitFor("First active")
            scenario.close()
            assertEquals(2, activeAlarms(store.load()).size)
            scenario = ActivityScenario.launch(Intent(context, ActiveAlarmActivity::class.java))
            waitFor("First active")
            compose.onNodeWithText("Dismiss").performScrollTo().performClick()
            waitFor("Second active")
            assertFalse(store.load().first().enabled)
            compose.onNodeWithText("Dismiss").performScrollTo().performClick()
            compose.waitUntil(5_000) { scenario?.state == Lifecycle.State.DESTROYED }
            assertTrue(activeAlarms(store.load()).isEmpty())
            assertTrue(store.load().all { !it.enabled })
        } finally {
            scenario?.close()
            store.save(original)
            AlarmNotifications.update(context, original)
        }
    }
}
