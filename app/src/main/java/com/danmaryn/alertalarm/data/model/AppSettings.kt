package com.danmaryn.alertalarm.data.model

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

enum class AppLanguage(val code: String, val title: String) {
    UA("ua", "UA (Українська)"),
    RU("ru", "RU (Русский)"),
    EN("en", "EN (English)")
}

data class AppSettings(
    val defaultRegion: String = UkrainianRegions.DEFAULT_REGION,
    val defaultVolume: Float = 0.9f,
    val defaultRingtoneUri: String? = null,
    val defaultRingtoneTitle: String = "Системний сигнал",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.UA
)
