package com.danmaryn.alertalarm.data.model

import java.util.Calendar
import java.util.UUID

data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Нагадування",
    val hour: Int = 7,
    val minute: Int = 0,
    val isEnabled: Boolean = true,
    val regionName: String = UkrainianRegions.DEFAULT_REGION,
    val smartDelayEnabled: Boolean = true,
    val deadlineHour: Int = 8,
    val deadlineMinute: Int = 30,
    val volume: Float = 0.9f,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Системний сигнал за замовчуванням"
) {
    /**
     * Unique integer request code for AlarmManager based on unique ID.
     */
    val requestCode: Int
        get() = Math.abs(id.hashCode() % 100000)

    val deadlineRequestCode: Int
        get() = requestCode + 100000

    /**
     * Calculates the trigger timestamp in millis for the scheduled time.
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
     * Calculates the timestamp in millis for the final trigger time (deadline).
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
        if (calendar.timeInMillis < targetTime) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    val formattedTime: String
        get() = String.format("%02d:%02d", hour, minute)

    val formattedDeadlineTime: String
        get() = String.format("%02d:%02d", deadlineHour, deadlineMinute)
}
