package com.smagri.alarmclock.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smagri.alarmclock.data.AlarmStore
import com.smagri.alarmclock.scheduling.AlarmRepository
import com.smagri.alarmclock.scheduling.AndroidAlarmScheduler
import java.io.File
import java.util.concurrent.Executors

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS) return
        val id = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_ID) ?: return
        if (!intent.hasExtra(AndroidAlarmScheduler.EXTRA_AT)) return
        val at = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_AT, 0)
        val pending = goAsync()
        val app = context.applicationContext
        executor.execute {
            try {
                val repository = AlarmRepository(AlarmStore(File(app.filesDir, "alarms.json")), AndroidAlarmScheduler(app))
                val result = repository.dismiss(id, at)
                AlarmNotifications.update(app, result.alarms)
                app.sendBroadcast(Intent(AndroidAlarmScheduler.ACTION_UPDATED).setPackage(app.packageName))
            } catch (error: Exception) {
                Log.e("AlarmActionReceiver", "Could not dismiss alarm; active state retained", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.smagri.alarmclock.DISMISS_ALARM"
        private val executor = Executors.newSingleThreadExecutor()
    }
}
