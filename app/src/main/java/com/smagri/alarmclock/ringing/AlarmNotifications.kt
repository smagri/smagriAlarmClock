package com.smagri.alarmclock.ringing

import android.Manifest
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.smagri.alarmclock.R
import com.smagri.alarmclock.scheduling.AndroidAlarmScheduler
import com.smagri.alarmclock.ui.alarms.AlarmListItem
import java.time.format.DateTimeFormatter

data class AlarmPresentationAccess(val notifications: Boolean, val fullScreen: Boolean)

object AlarmNotifications {
    const val CHANNEL_ID = "active_alarms"
    const val NOTIFICATION_ID = 501

    fun access(context: Context): AlarmPresentationAccess {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.alarm_channel),
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.alarm_channel_description)
            setSound(null, null) // Audio playback and vibration are later stages.
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return AlarmPresentationAccess(
            permission && manager.areNotificationsEnabled() &&
                manager.getNotificationChannel(CHANNEL_ID).importance != NotificationManager.IMPORTANCE_NONE,
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || manager.canUseFullScreenIntent(),
        )
    }

    fun activityIntent(context: Context): Intent = Intent(context, ActiveAlarmActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    fun dismissIntent(context: Context, active: ActiveAlarm): Intent = Intent(context, AlarmActionReceiver::class.java).apply {
        action = AlarmActionReceiver.ACTION_DISMISS
        data = Uri.Builder().scheme("alarm-dismiss").authority(context.packageName)
            .appendPath(active.alarmId).appendPath(active.occurrence.atMillis.toString()).build()
        putExtra(AndroidAlarmScheduler.EXTRA_ID, active.alarmId)
        putExtra(AndroidAlarmScheduler.EXTRA_AT, active.occurrence.atMillis)
    }

    @Suppress("DEPRECATION") // API 35 compatibility; API 36+ uses the scoped replacement.
    private fun openIntent(context: Context): PendingIntent {
        val options = if (Build.VERSION.SDK_INT >= 35) ActivityOptions.makeBasic().apply {
            setPendingIntentCreatorBackgroundActivityStartMode(
                if (Build.VERSION.SDK_INT >= 36) ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                else ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        }.toBundle() else null
        return PendingIntent.getActivity(context, 0, activityIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE, options)
    }

    /** Persisted state is authoritative. Notification removal never dismisses an occurrence. */
    fun update(context: Context, alarms: List<AlarmListItem>, alert: Boolean = false) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val queue = activeAlarms(alarms)
        if (queue.isEmpty()) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        val access = access(context)
        if (!access.notifications) return
        val active = queue.first()
        val occurrence = active.occurrence
        val locale = context.resources.configuration.locales[0]
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        val time = occurrence.time.format(DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(locale, skeleton), locale))
        val open = openIntent(context)
        val dismiss = PendingIntent.getBroadcast(context, 0, dismissIntent(context, active),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(occurrence.label.ifBlank { context.getString(R.string.active_alarm_title) })
            .setContentText(context.getString(R.string.active_notification_text, time))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(!alert)
            .addAction(0, context.getString(R.string.dismiss_alarm), dismiss)
            .apply { if (alert && access.fullScreen) setFullScreenIntent(open, true) }
            .build()
        // Access can change between the check and notify. Retain active state if
        // Android denies presentation; it remains reachable from the main list.
        try { manager.notify(NOTIFICATION_ID, notification) }
        catch (_: SecurityException) { /* User can grant access in Settings. */ }
    }
}
