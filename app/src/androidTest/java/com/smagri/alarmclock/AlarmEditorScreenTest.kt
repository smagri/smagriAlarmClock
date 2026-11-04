package com.smagri.alarmclock

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smagri.alarmclock.ui.alarms.AlarmEditorScreen
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.time.DayOfWeek
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmEditorScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun independentWeekdaysAndDraftSurviveStateRestoration() {
        val restoration = StateRestorationTester(compose)
        var saved: AlarmListItem? = null
        restoration.setContent {
            SmagriAlarmClockTheme {
                AlarmEditorScreen(null, false, { saved = it }, {}, {})
            }
        }
        compose.onNodeWithText("Label (optional)").performTextInput("Draft")
        DayOfWeek.entries.forEach { day ->
            val name = day.name.lowercase().replaceFirstChar { it.uppercase() }
            compose.onNodeWithText(name).performScrollTo().performClick()
        }
        compose.onNodeWithText("Tuesday").performScrollTo().performClick()
        compose.onNodeWithText("Thursday").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("Draft", saved?.label)
            assertEquals(DayOfWeek.entries.toSet() - setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), saved?.repeatDays)
            assertTrue(saved!!.enabled)
        }
    }
}
