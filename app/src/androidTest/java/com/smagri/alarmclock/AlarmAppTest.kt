package com.smagri.alarmclock

import android.os.SystemClock
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Matcher
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import android.content.res.Resources
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.ui.alarms.AlarmApp
import android.util.AtomicFile
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmAppTest {
    @get:Rule val compose = createComposeRule()
    private val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        "alarm-app-test-${UUID.randomUUID()}.json")
    private val blockedWrite = File("${file.path}.new")
    @After fun cleanUp() {
        blockedWrite.deleteRecursively()
        AtomicFile(file).delete()
    }

    private fun waitForText(text: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun clickText(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
    }

    @Test fun createEditToggleReloadAndDelete() {
        val session = mutableStateOf(0)
        compose.setContent {
            SmagriAlarmClockTheme {
                key(session.value) { AlarmApp(AlarmStore(file)) }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Add Alarm").fetchSemanticsNodes()
                .any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        }
        compose.onNodeWithText("No alarms yet. Tap + to add your first alarm.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add Alarm").performClick()
        compose.onNodeWithText("Label (optional)").performTextInput("Morning")
        compose.onNode(hasText("Time:", substring = true)).performClick()
        // Use the native text fields and close the IME before confirming; it can
        // obscure the dialog button while Compose can still access content behind it.
        fun androidId(name: String) = Resources.getSystem().getIdentifier(name, "id", "android")
        onView(withId(androidId("toggle_mode"))).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
            override fun getDescription() = "Open text input only if the picker is in clock mode"
            override fun perform(uiController: UiController, view: View) {
                val input = view.rootView.findViewById<View>(androidId("input_hour"))
                if (!input.isShown) view.performClick()
                uiController.loopMainThreadUntilIdle()
            }
        })
        onView(withId(androidId("input_hour"))).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
            override fun getDescription() = "Wait for native time input layout"
            override fun perform(uiController: UiController, view: View) {
                val deadline = SystemClock.uptimeMillis() + 5_000
                while (view.width == 0 || view.height == 0 || view.isLayoutRequested) {
                    check(SystemClock.uptimeMillis() < deadline) { "Native time input did not finish layout" }
                    uiController.loopMainThreadForAtLeast(16)
                }
            }
        })
        onView(withId(androidId("input_hour"))).perform(replaceText("11"))
        onView(withId(androidId("input_minute"))).perform(replaceText("45"))
        closeSoftKeyboard()
        onView(withId(android.R.id.button1)).perform(click())
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Time:", substring = true) and hasText("11:45", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        clickText("Monday")
        clickText("Sunday")
        clickText("Save")
        waitForText("Alarms")
        waitForText("Morning")
        val saved = AlarmStore(file).load().single()
        assertEquals(LocalTime.of(11, 45), saved.time)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), saved.repeatDays)

        compose.onNodeWithText("Morning").performClick()
        compose.onNodeWithText("Label (optional)").performTextReplacement("Evening")
        clickText("Monday")
        clickText("Sunday")
        clickText("Save")
        waitForText("Alarms")
        waitForText("Evening")
        compose.onNodeWithText("One-time").assertIsDisplayed()
        assertEquals(saved.id, AlarmStore(file).load().single().id)
        compose.onNodeWithContentDescription("Alarm enabled state").performClick()
        waitForText("Disabled")
        // Rebuild all UI state and load the same file using a new store instance.
        compose.runOnIdle { session.value++ }
        waitForText("Evening")
        compose.onNodeWithText("Disabled").assertIsDisplayed()
        assertFalse(AlarmStore(file).load().single().enabled)
        compose.onNodeWithText("Evening").performClick()
        clickText("Delete Alarm")
        compose.onNodeWithText("Delete this alarm? This cannot be undone.").assertIsDisplayed()
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        clickText("Delete Alarm")
        compose.onNode(hasText("Delete Alarm") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        waitForText("No alarms yet. Tap + to add your first alarm.")
        assertTrue(AlarmStore(file).load().isEmpty())
    }

    @Test fun cancelNewAlarmDoesNotSave() {
        compose.setContent { SmagriAlarmClockTheme { AlarmApp(AlarmStore(file)) } }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Add Alarm").fetchSemanticsNodes()
                .any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        }
        compose.onNodeWithContentDescription("Add Alarm").performClick()
        compose.onNodeWithText("Label (optional)").performTextInput("Discard me")
        clickText("Cancel")
        waitForText("No alarms yet. Tap + to add your first alarm.")
        assertTrue(AlarmStore(file).load().isEmpty())
    }

    @Test fun failedLoadPreservesDataAndRetryLoadsRecoveredFile() {
        file.writeText("Malformed test fixture")
        val store = AlarmStore(file)
        compose.setContent { SmagriAlarmClockTheme { AlarmApp(store) } }
        waitForText("Alarm storage error")
        assertEquals("Malformed test fixture", file.readText())
        compose.onNodeWithContentDescription("Add Alarm").assertIsNotEnabled()
        // Repair only this test fixture, then exercise the user's Retry action.
        store.save(emptyList())
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Add Alarm").fetchSemanticsNodes()
                .any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        }
        compose.onNodeWithText("No alarms yet. Tap + to add your first alarm.").assertIsDisplayed()
    }

    @Test fun failedSaveKeepsDraftAndPreviousDataUntilRetrySucceeds() {
        val store = AlarmStore(file)
        store.save(emptyList())
        val original = file.readBytes()
        compose.setContent { SmagriAlarmClockTheme { AlarmApp(store) } }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Add Alarm").fetchSemanticsNodes()
                .any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        }
        // Prevent opening AtomicFile's temporary output without changing the
        // successfully saved file or requiring storage permissions.
        check(blockedWrite.mkdir())
        File(blockedWrite, "blocker").writeText("test fixture")
        compose.onNodeWithContentDescription("Add Alarm").performClick()
        compose.onNodeWithText("Label (optional)").performTextInput("Keep this draft")
        clickText("Save")
        waitForText("Alarm storage error")
        assertArrayEquals(original, file.readBytes())
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Label (optional)").performScrollTo().assertTextContains("Keep this draft")
        check(blockedWrite.deleteRecursively())
        clickText("Save")
        waitForText("Alarms")
        waitForText("Keep this draft")
        assertEquals("Keep this draft", AlarmStore(file).load().single().label)
    }
}
