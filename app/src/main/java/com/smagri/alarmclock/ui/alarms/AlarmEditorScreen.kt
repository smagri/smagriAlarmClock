package com.smagri.alarmclock.ui.alarms

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.smagri.alarmclock.R
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.UUID

@Composable
fun AlarmEditorScreen(
    alarm: AlarmListItem?,
    busy: Boolean,
    onSave: (AlarmListItem) -> Unit,
    onCancel: () -> Unit,
    onDelete: (AlarmListItem) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val formatter = alarmTimeFormatter()
    val id = rememberSaveable { alarm?.id ?: UUID.randomUUID().toString() }
    var hour by rememberSaveable { mutableIntStateOf(alarm?.time?.hour ?: 7) }
    var minute by rememberSaveable { mutableIntStateOf(alarm?.time?.minute ?: 0) }
    var label by rememberSaveable { mutableStateOf(alarm?.label.orEmpty()) }
    var enabled by rememberSaveable { mutableStateOf(alarm?.enabled ?: true) }
    var dayMask by rememberSaveable {
        mutableIntStateOf(alarm?.repeatDays?.fold(0) { mask, day -> mask or (1 shl day.ordinal) } ?: 0)
    }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (showTimePicker) {
        DisposableEffect(context) {
            val dialog = TimePickerDialog(context, { _, h, m ->
                hour = h
                minute = m
                showTimePicker = false
            }, hour, minute, DateFormat.is24HourFormat(context))
            dialog.setOnDismissListener { showTimePicker = false }
            dialog.show()
            onDispose {
                dialog.setOnDismissListener(null)
                dialog.dismiss()
            }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (alarm == null) R.string.add_alarm else R.string.edit_alarm),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(stringResource(R.string.configuration_notice), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { showTimePicker = true }, enabled = !busy) {
                Text(stringResource(R.string.choose_time, LocalTime.of(hour, minute).format(formatter)))
            }
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.alarm_label)) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.alarm_enabled), Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !busy)
            }
            Text(stringResource(R.string.repeat_days), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.no_days_hint), style = MaterialTheme.typography.bodySmall)
            DayOfWeek.entries.forEach { day ->
                val selected = dayMask and (1 shl day.ordinal) != 0
                Row(
                    Modifier.fillMaxWidth().toggleable(
                        value = selected, enabled = !busy, role = Role.Checkbox,
                        onValueChange = { dayMask = dayMask xor (1 shl day.ordinal) },
                    ).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = selected, onCheckedChange = null, enabled = !busy)
                    Text(day.getDisplayName(TextStyle.FULL, locale))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = !busy, onClick = {
                    onSave(AlarmListItem(id, LocalTime.of(hour, minute),
                        DayOfWeek.entries.filter { dayMask and (1 shl it.ordinal) != 0 }.toSet(), label, enabled))
                }) { Text(stringResource(R.string.save_alarm)) }
                TextButton(onClick = onCancel, enabled = !busy) { Text(stringResource(R.string.cancel)) }
            }
            if (alarm != null) {
                TextButton(onClick = { confirmDelete = true }, enabled = !busy) {
                    Text(stringResource(R.string.delete_alarm), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete && alarm != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_alarm)) },
            text = { Text(stringResource(R.string.delete_confirmation)) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    confirmDelete = false
                    onDelete(alarm)
                }) { Text(stringResource(R.string.delete_alarm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
