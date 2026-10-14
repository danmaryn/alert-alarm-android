package com.danmaryn.alertalarm.data.model

data class AlertCheckResult(
    val isAlertActive: Boolean,
    val regionName: String,
    val changedAt: String? = null,
    val isNetworkAvailable: Boolean = true,
    val errorMessage: String? = null
)

enum class AlarmTriggerReason {
    SCHEDULED_NORMAL,     // Rang at scheduled time, no alert
    ALERT_CLEARED,        // Rang right after alert was cleared
    DEADLINE_REACHED,     // Rang because hard deadline was reached during alert
    OFFLINE_FAILSAFE      // Rang at scheduled time because internet was unavailable
}
