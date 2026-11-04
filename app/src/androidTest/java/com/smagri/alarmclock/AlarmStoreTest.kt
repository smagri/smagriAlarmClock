package com.smagri.alarmclock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.util.AtomicFile
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

    @After fun cleanUp() { AtomicFile(file).delete() }

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

    @Test fun invalidSavePreservesPreviouslySavedConfiguration() {
        val original = listOf(AlarmListItem("original", LocalTime.of(7, 0), label = "Keep me"))
        val store = AlarmStore(file)
        store.save(original)
        val originalBytes = file.readBytes()
        val alarm = original.single()
        listOf(
            listOf(alarm.copy(id = " ")),
            listOf(alarm, alarm.copy(label = "Duplicate ID")),
            listOf(alarm.copy(time = LocalTime.of(7, 0, 30))),
            listOf(alarm.copy(time = LocalTime.of(7, 0, 0, 1))),
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { store.save(invalid) }
            assertArrayEquals(originalBytes, file.readBytes())
            assertEquals(original, AlarmStore(file).load())
        }
    }

    @Test fun unfinishedAtomicWriteKeepsLastCompletedSave() {
        val original = listOf(AlarmListItem("original", LocalTime.of(6, 30), enabled = false))
        AlarmStore(file).save(original)
        // Simulate a process stopping after writing only part of a replacement,
        // without finishWrite or failWrite being called.
        AtomicFile(file).startWrite().use { it.write("{\"version\":".toByteArray()) }
        assertEquals(original, AlarmStore(file).load())
        val edited = original.map { it.copy(label = "Next complete save") }
        AlarmStore(file).save(edited)
        assertEquals(edited, AlarmStore(file).load())
    }

    @Test fun changesToOneAlarmDoNotChangeOtherSavedAlarms() {
        val first = AlarmListItem("a", LocalTime.of(0, 0), setOf(DayOfWeek.MONDAY), "First")
        val second = AlarmListItem("b", LocalTime.of(23, 59), setOf(DayOfWeek.SUNDAY), "", false)
        AlarmStore(file).save(listOf(first, second))
        val changed = first.copy(time = LocalTime.of(18, 15), repeatDays = DayOfWeek.entries.toSet(),
            label = "Edited", enabled = false)
        AlarmStore(file).save(listOf(changed, second))
        assertEquals(listOf(changed, second), AlarmStore(file).load())
        AlarmStore(file).save(listOf(second))
        assertEquals(listOf(second), AlarmStore(file).load())
    }

    @Test fun versionOneConfigurationsMigrateWithoutLosingFields() {
        file.writeText("""{"version":1,"alarms":[{"id":"legacy","hour":23,"minute":59,"repeatDays":[1,7],"label":"Original","enabled":false}]}""")
        val original = AlarmListItem("legacy", LocalTime.of(23, 59),
            setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), "Original", false)
        assertEquals(listOf(original), AlarmStore(file).load())
        AlarmStore(file).save(listOf(original))
        assertEquals(2, org.json.JSONObject(file.readText()).getInt("version"))
        assertEquals(listOf(original), AlarmStore(file).load())
    }

    @Test fun pendingAndDeliveredOccurrencesSurviveReload() {
        val alarm = AlarmListItem("occurrence", LocalTime.of(7, 0),
            scheduledAtMillis = 1_800_000_000_000L, lastDeliveredAtMillis = 1_799_913_600_000L)
        AlarmStore(file).save(listOf(alarm))
        assertEquals(listOf(alarm), AlarmStore(file).load())
        val delivered = alarm.copy(scheduledAtMillis = null)
        AlarmStore(file).save(listOf(delivered))
        assertEquals(listOf(delivered), AlarmStore(file).load())
    }
}
