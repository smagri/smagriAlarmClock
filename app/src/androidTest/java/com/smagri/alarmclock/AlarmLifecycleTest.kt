package com.smagri.alarmclock

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smagri.alarmclock.data.AlarmStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Exercise the real launcher activity; remove only this test's labelled fixture. */
@RunWith(AndroidJUnit4::class)
class AlarmLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val label = "Lifecycle test ${UUID.randomUUID()}"
    private var relaunched: ActivityScenario<MainActivity>? = null
    private var store: AlarmStore? = null

    @After fun removeFixture() {
        relaunched?.close()
        compose.activityRule.scenario.close()
        store?.let { storage -> storage.save(storage.load().filterNot { it.label == label }) }
    }

    private fun waitForLabel() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun savedAlarmSurvivesRotationAndClosingAndRelaunchingActivity() {
        store = AlarmStore(File(compose.activity.filesDir, "alarms.json"))
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Add Alarm").fetchSemanticsNodes()
                .any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        }
        compose.onNodeWithContentDescription("Add Alarm").performClick()
        compose.onNodeWithText("Label (optional)").performTextInput(label)
        compose.onNodeWithText("Monday").performScrollTo().performClick()
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        compose.onNodeWithText("Label (optional)").performScrollTo().assertTextContains(label)
        compose.onNodeWithText("Save").performScrollTo().performClick()
        waitForLabel()
        val saved = store!!.load().single { it.label == label }
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        compose.waitUntil(10_000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }
        waitForLabel()
        compose.activityRule.scenario.close()
        relaunched = ActivityScenario.launch(MainActivity::class.java)
        waitForLabel()
        assertEquals(saved, store!!.load().single { it.label == label })
    }
}
