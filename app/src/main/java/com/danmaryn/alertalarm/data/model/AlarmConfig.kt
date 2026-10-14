package com.danmaryn.alertalarm.data.model

import java.util.Calendar

data class AlarmConfig(
    val hour: Int = 7,
    val minute: Int = 0,
    val isEnabled: Boolean = false,
    val regionName: String = UkrainianRegions.DEFAULT_REGION,
    val smartDelayEnabled: Boolean = true,
    val deadlineHour: Int = 8,
    val deadlineMinute: Int = 30
) {
    /**
     * Calculates the trigger timestamp in millis for the target alarm time.
     * If the time today has already passed, schedules for tomorrow.
     */
    fun calculateTargetTimeMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    /**
     * Calculates the timestamp in millis for the hard deadline.
     * Ensures deadline is set on the appropriate day corresponding to the alarm.
     */
    fun calculateDeadlineMillis(): Long {
        val targetTime = calculateTargetTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = targetTime
            set(Calendar.HOUR_OF_DAY, deadlineHour)
            set(Calendar.MINUTE, deadlineMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If deadline hour is numerically earlier than alarm hour (e.g. alarm 23:00, deadline 01:00),
        // it means the deadline is on the next day relative to the alarm.
        if (calendar.timeInMillis < targetTime) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    val formattedAlarmTime: String
        get() = String.format("%02d:%02d", hour, minute)

    val formattedDeadlineTime: String
        get() = String.format("%02d:%02d", deadlineHour, deadlineMinute)
}
