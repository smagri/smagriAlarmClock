package com.smagri.alarmclock.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smagri.alarmclock.R
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.time.format.TextStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AlarmListScreen(
    alarms: List<AlarmListItem>,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
    onAdd: () -> Unit = {},
    onEdit: (AlarmListItem) -> Unit = {},
    onToggle: (AlarmListItem, Boolean) -> Unit = { _, _ -> },
    showScheduling: Boolean = false,
    permissionRequired: Boolean = false,
    schedulingFailed: Boolean = false,
    onRequestPermission: () -> Unit = {},
    notificationsBlocked: Boolean = false,
    fullScreenRestricted: Boolean = false,
    activeCount: Int = 0,
    onOpenActive: () -> Unit = {},
    onNotificationAccess: () -> Unit = {},
    onFullScreenAccess: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = alarmTimeFormatter()
    val addDescription = stringResource(R.string.add_alarm)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (controlsEnabled) onAdd() },
                modifier = Modifier.semantics {
                    contentDescription = addDescription
                    if (!controlsEnabled) disabled()
                },
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            Text(
                stringResource(R.string.alarms_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Text(
                stringResource(R.string.configuration_notice),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (permissionRequired) {
                Text(stringResource(R.string.exact_permission_needed), Modifier.padding(horizontal = 16.dp))
                TextButton(onClick = onRequestPermission) {
                    Text(stringResource(R.string.allow_exact_alarms))
                }
            }
            if (schedulingFailed) {
                Text(stringResource(R.string.scheduling_failed), Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error)
            }
            if (notificationsBlocked) {
                Text(stringResource(R.string.notifications_blocked), Modifier.padding(horizontal = 16.dp))
                TextButton(onClick = onNotificationAccess) { Text(stringResource(R.string.enable_alarm_notifications)) }
            }
            if (fullScreenRestricted) {
                TextButton(onClick = onFullScreenAccess) { Text(stringResource(R.string.allow_full_screen)) }
            }
            if (activeCount > 0) {
                TextButton(onClick = onOpenActive) { Text(stringResource(R.string.open_active_alarm, activeCount)) }
            }
            if (alarms.isEmpty()) {
                Text(
                    stringResource(R.string.empty_alarms),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(alarms.sortedBy { it.time }, key = { it.id }) { alarm ->
                    Card(onClick = { onEdit(alarm) }, enabled = controlsEnabled, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(alarm.time.format(formatter), style = MaterialTheme.typography.headlineMedium)
                                if (alarm.label.isNotBlank()) {
                                    Text(alarm.label, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    if (alarm.repeatDays.isEmpty()) stringResource(R.string.one_time)
                                    else alarm.repeatDays.sortedBy { it.value }.joinToString(" · ") {
                                        it.getDisplayName(TextStyle.SHORT, locale)
                                    },
                                )
                                Text(stringResource(R.string.default_sound), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    stringResource(if (alarm.enabled) R.string.alarm_enabled else R.string.alarm_disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (showScheduling && alarm.enabled) {
                                    val at = alarm.scheduledAtMillis
                                    val status = when {
                                        permissionRequired -> stringResource(R.string.not_scheduled)
                                        at != null -> {
                                            val date = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
                                            val day = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                                            stringResource(R.string.next_occurrence, day, date.toLocalTime().format(formatter))
                                        }
                                        alarm.activeOccurrences.isNotEmpty() -> stringResource(R.string.alarm_awaiting_dismissal)
                                        alarm.lastDeliveredAtMillis != null -> stringResource(R.string.previous_time_reached)
                                        else -> stringResource(R.string.schedule_pending)
                                    }
                                    Text(status, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            val switchDescription = stringResource(R.string.alarm_switch_description)
                            Switch(
                                checked = alarm.enabled,
                                onCheckedChange = { onToggle(alarm, it) },
                                enabled = controlsEnabled,
                                modifier = Modifier.semantics { contentDescription = switchDescription },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmListLightPreview() {
    SmagriAlarmClockTheme(darkTheme = false, dynamicColor = false) {
        AlarmListScreen(sampleAlarms)
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmListDarkPreview() {
    SmagriAlarmClockTheme(darkTheme = true, dynamicColor = false) {
        AlarmListScreen(sampleAlarms)
    }
}
