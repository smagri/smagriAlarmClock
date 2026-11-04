package com.smagri.alarmclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smagri.alarmclock.ui.alarms.AlarmApp
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AlarmStore(File(filesDir, "alarms.json"))
        setContent {
            SmagriAlarmClockTheme {
                AlarmApp(store)
            }
        }
    }
}
