package com.danmaryn.alertalarm.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.danmaryn.alertalarm.data.model.AlarmConfig
import com.danmaryn.alertalarm.data.model.UkrainianRegions

class AlarmPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "alert_alarm_prefs"
        private const val KEY_HOUR = "key_hour"
        private const val KEY_MINUTE = "key_minute"
        private const val KEY_ENABLED = "key_enabled"
        private const val KEY_REGION = "key_region"
        private const val KEY_SMART_DELAY = "key_smart_delay"
        private const val KEY_DEADLINE_HOUR = "key_deadline_hour"
        private const val KEY_DEADLINE_MINUTE = "key_deadline_minute"
    }

    fun getAlarmConfig(): AlarmConfig {
        return AlarmConfig(
            hour = prefs.getInt(KEY_HOUR, 7),
            minute = prefs.getInt(KEY_MINUTE, 0),
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            regionName = prefs.getString(KEY_REGION, UkrainianRegions.DEFAULT_REGION)
                ?: UkrainianRegions.DEFAULT_REGION,
            smartDelayEnabled = prefs.getBoolean(KEY_SMART_DELAY, true),
            deadlineHour = prefs.getInt(KEY_DEADLINE_HOUR, 8),
            deadlineMinute = prefs.getInt(KEY_DEADLINE_MINUTE, 30)
        )
    }

    fun saveAlarmConfig(config: AlarmConfig) {
        prefs.edit()
            .putInt(KEY_HOUR, config.hour)
            .putInt(KEY_MINUTE, config.minute)
            .putBoolean(KEY_ENABLED, config.isEnabled)
            .putString(KEY_REGION, config.regionName)
            .putBoolean(KEY_SMART_DELAY, config.smartDelayEnabled)
            .putInt(KEY_DEADLINE_HOUR, config.deadlineHour)
            .putInt(KEY_DEADLINE_MINUTE, config.deadlineMinute)
            .apply()
    }
}
