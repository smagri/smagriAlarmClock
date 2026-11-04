package com.smagri.alarmclock.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.smagri.alarmclock.R
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.scheduling.AlarmRepository
import com.smagri.alarmclock.scheduling.AndroidAlarmScheduler
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActiveAlarmActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
    private val updates = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { refresh++ }
    }

    @Suppress("DEPRECATION") // Legacy locked-screen flags are needed only on API 26.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        ContextCompat.registerReceiver(this, updates, IntentFilter(AndroidAlarmScheduler.ACTION_UPDATED),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        val store = AlarmStore(File(filesDir, "alarms.json"))
        val repository = AlarmRepository(store, AndroidAlarmScheduler(applicationContext))
        setContent {
            SmagriAlarmClockTheme {
                var queue by remember { mutableStateOf<List<ActiveAlarm>>(emptyList()) }
                var busy by remember { mutableStateOf(false) }
                var loadFailed by remember { mutableStateOf(false) }
                var dismissFailed by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(refresh, busy) {
                    if (busy) return@LaunchedEffect
                    try {
                        queue = activeAlarms(withContext(Dispatchers.IO) { store.load() })
                        loadFailed = false
                        if (queue.isEmpty()) finish()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        loadFailed = true
                    }
                }
                val active = queue.firstOrNull()
                if (active != null) {
                    ActiveAlarmScreen(active, queue.size - 1, busy, dismissFailed || loadFailed) {
                        if (!busy) {
                            busy = true
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO + NonCancellable) {
                                        repository.dismiss(active.alarmId, active.occurrence.atMillis).also {
                                            AlarmNotifications.update(applicationContext, it.alarms)
                                            sendBroadcast(Intent(AndroidAlarmScheduler.ACTION_UPDATED).setPackage(packageName))
                                        }
                                    }
                                    queue = activeAlarms(result.alarms)
                                    dismissFailed = false
                                    if (queue.isEmpty()) finish()
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    dismissFailed = true
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                } else {
                    Scaffold { padding ->
                        TextButton(onClick = { refresh++ }, modifier = Modifier.padding(padding)) {
                            Text(stringResource(if (loadFailed) R.string.active_load_failed else R.string.loading_active_alarm))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); refresh++ }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); refresh++ }
    override fun onDestroy() { unregisterReceiver(updates); super.onDestroy() }
}
