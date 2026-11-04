package com.smagri.alarmclock.scheduling

interface AlarmScheduler {
    fun canSchedule(): Boolean
    fun schedule(alarmId: String, triggerAtMillis: Long)
    fun cancel(alarmId: String, triggerAtMillis: Long)
}
