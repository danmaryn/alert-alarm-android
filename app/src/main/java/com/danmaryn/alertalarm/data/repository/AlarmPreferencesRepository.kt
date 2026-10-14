package com.danmaryn.alertalarm.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.danmaryn.alertalarm.data.model.AlarmItem
import com.danmaryn.alertalarm.data.model.UkrainianRegions
import org.json.JSONArray
import org.json.JSONObject

class AlarmPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "alert_alarm_multilist_prefs"
        private const val KEY_ALARMS_JSON = "key_alarms_json"
    }

    @Synchronized
    fun getAllAlarms(): List<AlarmItem> {
        val jsonString = prefs.getString(KEY_ALARMS_JSON, null) ?: return defaultInitialList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<AlarmItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AlarmItem(
                        id = obj.optString("id"),
                        label = obj.optString("label", "Нагадування"),
                        hour = obj.optInt("hour", 7),
                        minute = obj.optInt("minute", 0),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        regionName = obj.optString("regionName", UkrainianRegions.DEFAULT_REGION),
                        smartDelayEnabled = obj.optBoolean("smartDelayEnabled", true),
                        deadlineHour = obj.optInt("deadlineHour", 8),
                        deadlineMinute = obj.optInt("deadlineMinute", 30),
                        volume = obj.optDouble("volume", 0.9).toFloat(),
                        ringtoneUri = if (obj.has("ringtoneUri") && !obj.isNull("ringtoneUri")) obj.getString("ringtoneUri") else null,
                        ringtoneTitle = obj.optString("ringtoneTitle", "Системний сигнал за замовчуванням")
                    )
                )
            }
            if (list.isEmpty()) defaultInitialList() else list
        } catch (e: Exception) {
            defaultInitialList()
        }
    }

    private fun defaultInitialList(): List<AlarmItem> {
        val defaultAlarm = AlarmItem(
            label = "Повсякденне нагадування",
            hour = 7,
            minute = 0,
            isEnabled = false,
            regionName = UkrainianRegions.DEFAULT_REGION,
            smartDelayEnabled = true,
            deadlineHour = 8,
            deadlineMinute = 30,
            volume = 0.9f
        )
        val initial = listOf(defaultAlarm)
        saveAllAlarms(initial)
        return initial
    }

    @Synchronized
    fun saveAllAlarms(alarms: List<AlarmItem>) {
        val jsonArray = JSONArray()
        for (item in alarms) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("hour", item.hour)
                put("minute", item.minute)
                put("isEnabled", item.isEnabled)
                put("regionName", item.regionName)
                put("smartDelayEnabled", item.smartDelayEnabled)
                put("deadlineHour", item.deadlineHour)
                put("deadlineMinute", item.deadlineMinute)
                put("volume", item.volume.toDouble())
                put("ringtoneUri", item.ringtoneUri)
                put("ringtoneTitle", item.ringtoneTitle)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_ALARMS_JSON, jsonArray.toString()).apply()
    }

    @Synchronized
    fun getAlarmById(id: String): AlarmItem? {
        return getAllAlarms().find { it.id == id }
    }

    @Synchronized
    fun saveOrUpdateAlarm(alarm: AlarmItem) {
        val list = getAllAlarms().toMutableList()
        val index = list.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            list[index] = alarm
        } else {
            list.add(alarm)
        }
        saveAllAlarms(list)
    }

    @Synchronized
    fun deleteAlarm(id: String) {
        val list = getAllAlarms().toMutableList()
        list.removeAll { it.id == id }
        saveAllAlarms(list)
    }
}
