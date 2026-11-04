package com.smagri.alarmclock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmStoreTest {
    private val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        "alarm-store-test-${UUID.randomUUID()}.json")

    @After fun cleanUp() { file.delete() }

    @Test fun savesAllConfigurationAndReadsThroughNewStoreInstance() {
        val alarms = listOf(
            AlarmListItem("first", LocalTime.of(0, 0), DayOfWeek.entries.toSet(), "Wake \"up\" ☀", false),
            AlarmListItem("second", LocalTime.of(23, 59)),
        )
        assertTrue(AlarmStore(file).load().isEmpty())
        AlarmStore(file).save(alarms)
        assertEquals(alarms, AlarmStore(file).load())
        val edited = alarms.first().copy(time = LocalTime.of(12, 34),
            repeatDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.SUNDAY), enabled = true)
        AlarmStore(file).save(listOf(edited))
        assertEquals(listOf(edited), AlarmStore(file).load())
        AlarmStore(file).save(emptyList())
        assertTrue(AlarmStore(file).load().isEmpty())
    }

    @Test fun malformedOrUnsupportedDataIsReportedAndPreserved() {
        listOf("invalid JSON", """{"version":99,"alarms":[]}""",
            """{"version":1,"alarms":[{"id":"a","hour":25,"minute":0,"repeatDays":[],"label":"","enabled":true}]}"""
        ).forEach { invalid ->
            file.writeText(invalid)
            assertThrows(Exception::class.java) { AlarmStore(file).load() }
            assertEquals(invalid, file.readText())
        }
    }

    @Test fun unwritableDestinationReportsFailure() {
        file.writeText("Existing file blocks a directory")
        val store = AlarmStore(File(file, "alarms.json"))
        assertThrows(Exception::class.java) { store.save(emptyList()) }
        assertEquals("Existing file blocks a directory", file.readText())
    }
}
