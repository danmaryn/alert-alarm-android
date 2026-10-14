package com.danmaryn.alertalarm.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.danmaryn.alertalarm.data.model.AppLanguage
import com.danmaryn.alertalarm.data.model.AppSettings
import com.danmaryn.alertalarm.data.model.ThemeMode
import com.danmaryn.alertalarm.data.model.UkrainianRegions

class AppSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "alert_alarm_app_settings"
        private const val KEY_DEFAULT_REGION = "key_default_region"
        private const val KEY_DEFAULT_VOLUME = "key_default_volume"
        private const val KEY_DEFAULT_RINGTONE_URI = "key_default_ringtone_uri"
        private const val KEY_DEFAULT_RINGTONE_TITLE = "key_default_ringtone_title"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_APP_LANGUAGE = "key_app_language"
    }

    fun getSettings(): AppSettings {
        val region = prefs.getString(KEY_DEFAULT_REGION, UkrainianRegions.DEFAULT_REGION)
            ?: UkrainianRegions.DEFAULT_REGION
        val volume = prefs.getFloat(KEY_DEFAULT_VOLUME, 0.9f)
        val uri = prefs.getString(KEY_DEFAULT_RINGTONE_URI, null)
        val title = prefs.getString(KEY_DEFAULT_RINGTONE_TITLE, "Системний сигнал") ?: "Системний сигнал"

        val themeString = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeString)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        val langString = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.UA.name) ?: AppLanguage.UA.name
        val language = try {
            AppLanguage.valueOf(langString)
        } catch (e: Exception) {
            AppLanguage.UA
        }

        return AppSettings(
            defaultRegion = region,
            defaultVolume = volume,
            defaultRingtoneUri = uri,
            defaultRingtoneTitle = title,
            themeMode = themeMode,
            language = language
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_DEFAULT_REGION, settings.defaultRegion)
            .putFloat(KEY_DEFAULT_VOLUME, settings.defaultVolume)
            .putString(KEY_DEFAULT_RINGTONE_URI, settings.defaultRingtoneUri)
            .putString(KEY_DEFAULT_RINGTONE_TITLE, settings.defaultRingtoneTitle)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_APP_LANGUAGE, settings.language.name)
            .apply()
    }
}
