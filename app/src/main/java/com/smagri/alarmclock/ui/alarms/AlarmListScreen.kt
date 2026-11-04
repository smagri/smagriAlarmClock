package com.smagri.alarmclock.ui.alarms

import android.text.format.DateFormat
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smagri.alarmclock.R
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun AlarmListScreen(alarms: List<AlarmListItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeSkeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
    val formatter = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, timeSkeleton), locale,
    )
    val addDescription = stringResource(R.string.add_alarm_unavailable)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            // Material 3 FAB has no enabled parameter. Muted colours and disabled
            // semantics identify this visual placeholder; it has no action.
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = addDescription
                    disabled()
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
                stringResource(R.string.sample_notice),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(alarms.sortedBy { it.time }, key = { it.id }) { alarm ->
                    Card(Modifier.fillMaxWidth()) {
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
                                    stringResource(if (alarm.enabled) R.string.sample_enabled else R.string.sample_disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            val switchDescription = stringResource(R.string.alarm_switch_description)
                            Switch(
                                checked = alarm.enabled,
                                onCheckedChange = {},
                                enabled = false,
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
