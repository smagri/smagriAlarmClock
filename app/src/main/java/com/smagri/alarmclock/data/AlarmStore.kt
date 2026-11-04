package com.smagri.alarmclock.data

import android.util.AtomicFile
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import java.io.File
import java.io.FileNotFoundException
import java.time.DayOfWeek
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

/** Call from an IO dispatcher. A malformed file is reported, never silently replaced. */
class AlarmStore(file: File) {
    private val file = AtomicFile(file)
    // Also serializes reads after activity recreation against an in-flight save.
    companion object { private val lock = Any() }

    fun load(): List<AlarmListItem> = synchronized(lock) {
        val text = try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: FileNotFoundException) {
            if (file.baseFile.exists()) throw error
            return@synchronized emptyList()
        }
        val document = JSONObject(text)
        require(document.getInt("version") == 1) { "Unsupported alarm file version" }
        val records = document.getJSONArray("alarms")
        val alarms = (0 until records.length()).map { index ->
            val record = records.getJSONObject(index)
            val days = record.getJSONArray("repeatDays")
            AlarmListItem(
                id = record.getString("id").also { require(it.isNotBlank()) },
                time = LocalTime.of(record.getInt("hour"), record.getInt("minute")),
                repeatDays = (0 until days.length()).map { DayOfWeek.of(days.getInt(it)) }.toSet(),
                label = record.getString("label"),
                enabled = record.getBoolean("enabled"),
            )
        }
        require(alarms.map { it.id }.distinct().size == alarms.size) { "Duplicate alarm IDs" }
        alarms
    }

    fun save(alarms: List<AlarmListItem>) = synchronized(lock) {
        val records = JSONArray()
        alarms.forEach { alarm ->
            records.put(JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.time.hour)
                put("minute", alarm.time.minute)
                put("repeatDays", JSONArray(alarm.repeatDays.sortedBy { it.value }.map { it.value }))
                put("label", alarm.label)
                put("enabled", alarm.enabled)
            })
        }
        val bytes = JSONObject().put("version", 1).put("alarms", records)
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
}
