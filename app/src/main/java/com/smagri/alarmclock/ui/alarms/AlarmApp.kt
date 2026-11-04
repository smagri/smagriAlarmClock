package com.smagri.alarmclock.ui.alarms

import androidx.activity.compose.BackHandler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.smagri.alarmclock.R
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.scheduling.AlarmRepository
import com.smagri.alarmclock.scheduling.AndroidAlarmScheduler
import com.smagri.alarmclock.scheduling.ScheduleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AlarmApp(store: AlarmStore, repository: AlarmRepository? = null) {
    var alarms by remember { mutableStateOf<List<AlarmListItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    // null = list, empty string = new alarm, otherwise an existing alarm ID.
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(true) }
    var schedulingFailed by remember { mutableStateOf(false) }

    fun applyResult(result: ScheduleResult) {
        alarms = result.alarms
        permissionGranted = result.permissionGranted
        schedulingFailed = result.schedulingFailed
    }

    DisposableEffect(context, repository) {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) loadAttempt++
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { loadAttempt++ }
        }
        if (repository != null) {
            lifecycle?.addObserver(observer)
            ContextCompat.registerReceiver(context, receiver,
                IntentFilter(AndroidAlarmScheduler.ACTION_UPDATED), ContextCompat.RECEIVER_NOT_EXPORTED)
        }
        onDispose {
            if (repository != null) {
                lifecycle?.removeObserver(observer)
                context.unregisterReceiver(receiver)
            }
        }
    }

    LaunchedEffect(store, loadAttempt, busy) {
        if (busy) return@LaunchedEffect
        try {
            val result = withContext(Dispatchers.IO) {
                repository?.reconcile() ?: ScheduleResult(store.load(), true)
            }
            applyResult(result)
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
                val result = withContext(Dispatchers.IO + NonCancellable) {
                    repository?.saveConfigurations(updated) ?: run {
                        store.save(updated)
                        ScheduleResult(updated, true)
                    }
                }
                applyResult(result)
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
            showScheduling = repository != null,
            permissionRequired = !permissionGranted,
            schedulingFailed = schedulingFailed,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            "package:${context.packageName}".toUri()))
                    } catch (_: android.content.ActivityNotFoundException) {
                        error = R.string.settings_unavailable
                    }
                }
            },
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
