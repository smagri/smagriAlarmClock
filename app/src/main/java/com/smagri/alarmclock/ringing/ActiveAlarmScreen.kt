package com.smagri.alarmclock.ringing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.smagri.alarmclock.R
import com.smagri.alarmclock.ui.alarms.alarmTimeFormatter

@Composable
fun ActiveAlarmScreen(
    active: ActiveAlarm,
    waitingCount: Int,
    busy: Boolean,
    error: Boolean,
    onDismiss: () -> Unit,
) {
    val formatter = alarmTimeFormatter()
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(stringResource(R.string.active_alarm_title), style = MaterialTheme.typography.headlineLarge)
            Text(active.occurrence.time.format(formatter), style = MaterialTheme.typography.displayLarge)
            if (active.occurrence.label.isNotBlank()) {
                Text(active.occurrence.label, style = MaterialTheme.typography.headlineMedium)
            }
            Text(stringResource(R.string.visual_alarm_notice))
            if (waitingCount > 0) Text(pluralStringResource(R.plurals.alarms_waiting, waitingCount, waitingCount))
            if (error) Text(stringResource(R.string.dismiss_failed), color = MaterialTheme.colorScheme.error)
            Button(
                onClick = onDismiss, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
            ) {
                Text(stringResource(R.string.dismiss_alarm), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
