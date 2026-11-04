package com.smagri.alarmclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smagri.alarmclock.ui.alarms.AlarmListScreen
import com.smagri.alarmclock.ui.alarms.sampleAlarms
import com.smagri.alarmclock.ui.theme.SmagriAlarmClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmagriAlarmClockTheme {
                AlarmListScreen(alarms = sampleAlarms)
            }
        }
    }
}
