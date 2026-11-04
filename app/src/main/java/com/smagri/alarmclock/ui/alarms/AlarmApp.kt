package com.smagri.alarmclock.ui.alarms

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.smagri.alarmclock.R
import com.smagri.alarmclock.data.AlarmStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AlarmApp(store: AlarmStore) {
    var alarms by remember { mutableStateOf<List<AlarmListItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    // null = list, empty string = new alarm, otherwise an existing alarm ID.
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(store, loadAttempt) {
        try {
            alarms = withContext(Dispatchers.IO) { store.load() }
            loaded = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = R.string.load_failed
        }
    }

    fun persist(updated: List<AlarmListItem>, closeEditor: Boolean) {
        if (busy || !loaded) return
        busy = true
        scope.launch {
            try {
                // Finish atomic writes even if activity recreation cancels this scope.
                withContext(Dispatchers.IO + NonCancellable) { store.save(updated) }
                alarms = updated
                if (closeEditor) editorId = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = R.string.save_failed
            } finally {
                busy = false
            }
        }
    }

    BackHandler(enabled = editorId != null || busy) {
        if (!busy) editorId = null
    }
    if (loaded && editorId != null) {
        key(editorId) {
            AlarmEditorScreen(
                alarm = alarms.find { it.id == editorId }, busy = busy,
                onSave = { edited -> persist(alarms.filterNot { it.id == edited.id } + edited, true) },
                onCancel = { editorId = null },
                onDelete = { deleted -> persist(alarms.filterNot { it.id == deleted.id }, true) },
            )
        }
    } else {
        AlarmListScreen(
            alarms = alarms, controlsEnabled = loaded && !busy,
            onAdd = { editorId = "" },
            onEdit = { editorId = it.id },
            onToggle = { alarm, enabled ->
                persist(alarms.map { if (it.id == alarm.id) it.copy(enabled = enabled) else it }, false)
            },
        )
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { if (loaded) error = null },
            title = { Text(stringResource(R.string.storage_error)) },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = {
                    error = null
                    if (!loaded) loadAttempt++
                }) { Text(stringResource(if (loaded) R.string.ok else R.string.retry)) }
            },
        )
    }
}
