package com.smagri.alarmclock.data

import android.util.AtomicFile
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import com.smagri.alarmclock.ringing.AlarmOccurrence
import java.io.File
import java.io.FileNotFoundException
import java.time.DayOfWeek
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

/** Call from an IO dispatcher. A malformed file is reported, never silently replaced. */
class AlarmStore(file: File) : AlarmStorage {
    private val file = AtomicFile(file)
    // Also serializes reads after activity recreation against an in-flight save.
    companion object { private val lock = Any() }

    override fun load(): List<AlarmListItem> = synchronized(lock) {
        val text = try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: FileNotFoundException) {
            if (file.baseFile.exists()) throw error
            return@synchronized emptyList()
        }
        val document = JSONObject(text)
        val version = document.getInt("version")
        require(version in 1..3) { "Unsupported alarm file version" }
        val records = document.getJSONArray("alarms")
        val alarms = (0 until records.length()).map { index ->
            val record = records.getJSONObject(index)
            val days = record.getJSONArray("repeatDays")
            AlarmListItem(
                id = record.getString("id"),
                time = LocalTime.of(record.getInt("hour"), record.getInt("minute")),
                repeatDays = (0 until days.length()).map { DayOfWeek.of(days.getInt(it)) }.toSet(),
                label = record.getString("label"),
                enabled = record.getBoolean("enabled"),
                scheduledAtMillis = if (version >= 2 && !record.isNull("scheduledAtMillis"))
                    record.getLong("scheduledAtMillis") else null,
                lastDeliveredAtMillis = if (version >= 2 && !record.isNull("lastDeliveredAtMillis"))
                    record.getLong("lastDeliveredAtMillis") else null,
                activeOccurrences = if (version >= 3) {
                    val active = record.getJSONArray("activeOccurrences")
                    (0 until active.length()).map { i ->
                        val occurrence = active.getJSONObject(i)
                        AlarmOccurrence(occurrence.getLong("atMillis"),
                            LocalTime.of(occurrence.getInt("hour"), occurrence.getInt("minute")),
                            occurrence.getString("label"))
                    }
                } else emptyList(),
            )
        }
        validate(alarms)
        alarms
    }

    override fun save(alarms: List<AlarmListItem>) = synchronized(lock) {
        // Validate before opening the atomic file so invalid input cannot replace
        // the last successfully saved configuration.
        validate(alarms)
        val records = JSONArray()
        alarms.forEach { alarm ->
            records.put(JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.time.hour)
                put("minute", alarm.time.minute)
                put("repeatDays", JSONArray(alarm.repeatDays.sortedBy { it.value }.map { it.value }))
                put("label", alarm.label)
                put("enabled", alarm.enabled)
                put("scheduledAtMillis", alarm.scheduledAtMillis ?: JSONObject.NULL)
                put("lastDeliveredAtMillis", alarm.lastDeliveredAtMillis ?: JSONObject.NULL)
                put("activeOccurrences", JSONArray().apply {
                    alarm.activeOccurrences.forEach { occurrence ->
                        put(JSONObject().apply {
                            put("atMillis", occurrence.atMillis)
                            put("hour", occurrence.time.hour)
                            put("minute", occurrence.time.minute)
                            put("label", occurrence.label)
                        })
                    }
                })
            })
        }
        val bytes = JSONObject().put("version", 3).put("alarms", records)
            .toString().toByteArray(Charsets.UTF_8)
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    private fun validate(alarms: List<AlarmListItem>) {
        require(alarms.all { it.id.isNotBlank() }) { "Blank alarm ID" }
        require(alarms.map { it.id }.distinct().size == alarms.size) { "Duplicate alarm IDs" }
        require(alarms.all { it.time.second == 0 && it.time.nano == 0 }) {
            "Alarm times must have minute precision"
        }
        require(alarms.all { alarm ->
            alarm.activeOccurrences.map { it.atMillis }.distinct().size == alarm.activeOccurrences.size &&
                alarm.activeOccurrences.all { it.time.second == 0 && it.time.nano == 0 }
        }) { "Invalid active occurrences" }
    }
}
