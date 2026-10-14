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
    val ringtoneTitle: String = "Системний сигнал",
    val daysOfWeek: Set<Int> = emptySet() // 1 = Mon, 2 = Tue, ... 7 = Sun. Empty = One-time.
) {
    val requestCode: Int
        get() = Math.abs(id.hashCode() % 100000)

    val deadlineRequestCode: Int
        get() = requestCode + 100000

    /**
     * Calculates the target timestamp in millis.
     * If daysOfWeek is specified, finds the nearest matching weekday.
     * Otherwise, schedules for today (if in future) or tomorrow.
     */
    fun calculateTargetTimeMillis(): Long {
        val now = Calendar.getInstance()

        if (daysOfWeek.isEmpty()) {
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

        // Repeating on specific days of week
        // Calendar.DAY_OF_WEEK: Sunday = 1, Monday = 2, ... Saturday = 7
        // Convert to ISO 1 = Mon .. 7 = Sun:
        val currentIsoDay = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        var daysToAdd = 0
        var found = false

        for (offset in 0..7) {
            val candidateDay = ((currentIsoDay - 1 + offset) % 7) + 1
            if (daysOfWeek.contains(candidateDay)) {
                val candidateCalendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, offset)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (candidateCalendar.after(now)) {
                    daysToAdd = offset
                    found = true
                    break
                }
            }
        }

        if (!found) {
            daysToAdd = 1
        }

        val target = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysToAdd)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return target.timeInMillis
    }

    /**
     * Calculates timestamp for the deadline (Час остаточного спрацювання).
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

    fun formatDaysSummary(lang: AppLanguage): String {
        if (daysOfWeek.isEmpty()) {
            return when (lang) {
                AppLanguage.UA -> "Один раз"
                AppLanguage.RU -> "Один раз"
                AppLanguage.EN -> "Once"
            }
        }
        if (daysOfWeek.size == 7) {
            return when (lang) {
                AppLanguage.UA -> "Щодня"
                AppLanguage.RU -> "Ежедневно"
                AppLanguage.EN -> "Every day"
            }
        }
        if (daysOfWeek == setOf(1, 2, 3, 4, 5)) {
            return when (lang) {
                AppLanguage.UA -> "Будні (Пн-Пт)"
                AppLanguage.RU -> "Будни (Пн-Пт)"
                AppLanguage.EN -> "Weekdays"
            }
        }
        if (daysOfWeek == setOf(6, 7)) {
            return when (lang) {
                AppLanguage.UA -> "Вихідні (Сб-Нд)"
                AppLanguage.RU -> "Выходные (Сб-Вс)"
                AppLanguage.EN -> "Weekends"
            }
        }

        val dayNames = when (lang) {
            AppLanguage.UA -> mapOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Нд")
            AppLanguage.RU -> mapOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
            AppLanguage.EN -> mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
        }
        return daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }
}
