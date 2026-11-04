package com.smagri.alarmclock

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smagri.alarmclock.ringing.ActiveAlarm
import com.smagri.alarmclock.ringing.ActiveAlarmScreen
import com.smagri.alarmclock.ringing.AlarmOccurrence
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveAlarmScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsLabelAndDismissWithoutSnoozeAndHandlesBlankLabel() {
        var dismissed = false
        compose.setContent {
            SmagriAlarmClockTheme {
                ActiveAlarmScreen(ActiveAlarm("a", AlarmOccurrence(1000, LocalTime.of(7, 15), "Wake up")),
                    waitingCount = 1, busy = false, error = false, onDismiss = { dismissed = true })
            }
        }
        compose.onNodeWithText("Wake up").assertIsDisplayed()
        compose.onNodeWithText("1 more alarm waiting").assertIsDisplayed()
        compose.onNodeWithText("Snooze").assertDoesNotExist()
        compose.onNodeWithText("Dismiss").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test fun failedDismissShowsErrorAndBusyPreventsDuplicateAction() {
        compose.setContent {
            SmagriAlarmClockTheme {
                ActiveAlarmScreen(ActiveAlarm("a", AlarmOccurrence(1000, LocalTime.MIDNIGHT, "")),
                    waitingCount = 0, busy = true, error = true, onDismiss = { error("Should be disabled") })
            }
        }
        compose.onNodeWithText("Could not dismiss the alarm. Please try again.").assertExists()
        compose.onNodeWithText("Dismiss").assertIsNotEnabled()
        compose.onNodeWithText("Alarm").assertIsDisplayed()
    }
}
