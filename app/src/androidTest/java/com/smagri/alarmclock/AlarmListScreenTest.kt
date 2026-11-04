package com.smagri.alarmclock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import com.smagri.alarmclock.ui.alarms.AlarmListScreen
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun controlsAreEnabledAndAlarmsAreSorted() {
        compose.setContent {
            SmagriAlarmClockTheme {
                AlarmListScreen(listOf(
                    AlarmListItem("2", LocalTime.of(9, 0), label = "Later"),
                    AlarmListItem("1", LocalTime.of(7, 0), label = "Earlier", enabled = false),
                ))
            }
        }
        compose.onNodeWithContentDescription("Add Alarm").assertIsEnabled()
        val switches = compose.onAllNodesWithContentDescription("Alarm enabled state")
        switches[0].assertIsEnabled()
        switches[1].assertIsEnabled()
        val earlier = compose.onNodeWithText("Earlier").fetchSemanticsNode().boundsInRoot.top
        val later = compose.onNodeWithText("Later").fetchSemanticsNode().boundsInRoot.top
        assertTrue(earlier < later)
        compose.onNodeWithText("Disabled").assertIsDisplayed()
    }

    @Test
    fun listScrollsToLastAlarm() {
        compose.setContent {
            SmagriAlarmClockTheme {
                AlarmListScreen((0..23).map {
                    AlarmListItem(it.toString(), LocalTime.of(it, 0), label = "Alarm $it")
                })
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(
            androidx.compose.ui.test.hasText("Alarm 23"),
        )
        compose.onNodeWithText("Alarm 23").assertIsDisplayed()
    }
}
