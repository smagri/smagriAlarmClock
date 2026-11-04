package com.smagri.alarmclock

import android.content.res.Configuration
import android.provider.Settings
import android.os.ParcelFileDescriptor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import com.smagri.alarmclock.ui.alarms.AlarmListScreen
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmDisplayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun systemTimeFormatAndBothThemeConfigurations() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val original = Settings.System.getString(resolver, Settings.System.TIME_12_24)
        check(original == null || original == "12" || original == "24")
        fun shell(command: String) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use {
                it.readBytes()
            }
        }
        val mode = mutableIntStateOf(0)
        var backgroundLuminance = 0f
        try {
            shell("settings put system time_12_24 12")
            compose.setContent {
                val configuration = Configuration(LocalConfiguration.current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (mode.intValue == 0) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
                }
                CompositionLocalProvider(LocalConfiguration provides configuration) {
                    SmagriAlarmClockTheme {
                        val luminance = MaterialTheme.colorScheme.background.luminance()
                        SideEffect { backgroundLuminance = luminance }
                        AlarmListScreen(listOf(AlarmListItem("display", LocalTime.of(23, 45))))
                    }
                }
            }
            compose.onNode(hasText("11:45", substring = true) and hasText("PM", substring = true)).assertIsDisplayed()
            compose.runOnIdle { assertTrue(backgroundLuminance > 0.5f) }
            shell("settings put system time_12_24 24")
            compose.runOnIdle { mode.intValue = 1 }
            compose.onNodeWithText("23:45").assertIsDisplayed()
            compose.runOnIdle { assertTrue(backgroundLuminance < 0.5f) }
        } finally {
            if (original == null) shell("settings delete system time_12_24")
            else shell("settings put system time_12_24 $original")
        }
    }
}
