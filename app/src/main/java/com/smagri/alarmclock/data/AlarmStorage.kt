package com.smagri.alarmclock.data

import com.smagri.alarmclock.ui.alarms.AlarmListItem

interface AlarmStorage {
    fun load(): List<AlarmListItem>
    fun save(alarms: List<AlarmListItem>)
}
