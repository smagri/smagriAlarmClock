package com.smagri.alarmclock.scheduling

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smagri.alarmclock.data.AlarmStore
import java.io.File
import java.util.concurrent.Executors

class AlarmScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidAlarmScheduler.ACTION_DELIVER &&
            intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        val pending = goAsync()
        val app = context.applicationContext
        executor.execute {
            try {
                val repository = AlarmRepository(AlarmStore(File(app.filesDir, "alarms.json")), AndroidAlarmScheduler(app))
                if (intent.action == AndroidAlarmScheduler.ACTION_DELIVER) {
                    val id = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_ID)
                    if (id != null && intent.hasExtra(AndroidAlarmScheduler.EXTRA_AT)) {
                        repository.delivered(id, intent.getLongExtra(AndroidAlarmScheduler.EXTRA_AT, 0))
                    }
                } else {
                    repository.reconcile()
                }
                app.sendBroadcast(Intent(AndroidAlarmScheduler.ACTION_UPDATED).setPackage(app.packageName))
            } catch (error: Exception) {
                Log.e("AlarmScheduleReceiver", "Could not update alarm schedules", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val executor = Executors.newSingleThreadExecutor()
    }
}
