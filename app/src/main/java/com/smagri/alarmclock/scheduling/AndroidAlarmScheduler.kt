package com.smagri.alarmclock.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.smagri.alarmclock.MainActivity

class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {
    private val manager = context.getSystemService(AlarmManager::class.java)

    override fun canSchedule(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    internal fun intent(id: String, at: Long): Intent = Intent(context, AlarmScheduleReceiver::class.java).apply {
        action = ACTION_DELIVER
        data = Uri.Builder().scheme("alarm").authority(context.packageName)
            .appendPath(id).appendPath(at.toString()).build()
        putExtra(EXTRA_ID, id)
        putExtra(EXTRA_AT, at)
    }

    override fun schedule(alarmId: String, triggerAtMillis: Long) {
        val operation = PendingIntent.getBroadcast(context, 0, intent(alarmId, triggerAtMillis),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val show = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, show), operation)
    }

    override fun cancel(alarmId: String, triggerAtMillis: Long) {
        val operation = PendingIntent.getBroadcast(context, 0, intent(alarmId, triggerAtMillis),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        manager.cancel(operation)
        operation.cancel()
    }

    companion object {
        const val ACTION_DELIVER = "com.smagri.alarmclock.DELIVER_SCHEDULE"
        const val EXTRA_ID = "alarmId"
        const val EXTRA_AT = "triggerAtMillis"
        const val ACTION_UPDATED = "com.smagri.alarmclock.SCHEDULES_UPDATED"
    }
}
