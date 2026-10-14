package com.danmaryn.alertalarm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danmaryn.alertalarm.data.model.AlarmConfig
import com.danmaryn.alertalarm.data.model.AlertCheckResult
import com.danmaryn.alertalarm.data.remote.AerialAlertApiClient
import com.danmaryn.alertalarm.data.repository.AlarmPreferencesRepository
import com.danmaryn.alertalarm.service.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlarmPreferencesRepository(application)

    private val _config = MutableStateFlow(repository.getAlarmConfig())
    val config: StateFlow<AlarmConfig> = _config.asStateFlow()

    private val _currentAlert = MutableStateFlow<AlertCheckResult?>(null)
    val currentAlert: StateFlow<AlertCheckResult?> = _currentAlert.asStateFlow()

    private val _isCheckingAlert = MutableStateFlow(false)
    val isCheckingAlert: StateFlow<Boolean> = _isCheckingAlert.asStateFlow()

    init {
        refreshCurrentAlert()
    }

    fun updateAlarmTime(hour: Int, minute: Int) {
        val updated = _config.value.copy(hour = hour, minute = minute)
        saveAndReschedule(updated)
    }

    fun updateDeadlineTime(hour: Int, minute: Int) {
        val updated = _config.value.copy(deadlineHour = hour, deadlineMinute = minute)
        saveAndReschedule(updated)
    }

    fun updateRegion(regionName: String) {
        val updated = _config.value.copy(regionName = regionName)
        saveAndReschedule(updated)
        refreshCurrentAlert(regionName)
    }

    fun setSmartDelayEnabled(enabled: Boolean) {
        val updated = _config.value.copy(smartDelayEnabled = enabled)
        saveAndReschedule(updated)
    }

    fun setAlarmEnabled(enabled: Boolean) {
        val updated = _config.value.copy(isEnabled = enabled)
        saveAndReschedule(updated)
    }

    private fun saveAndReschedule(newConfig: AlarmConfig) {
        _config.value = newConfig
        repository.saveAlarmConfig(newConfig)

        val context = getApplication<Application>()
        if (newConfig.isEnabled) {
            AlarmScheduler.scheduleAlarm(context, newConfig)
        } else {
            AlarmScheduler.cancelAlarm(context)
        }
    }

    fun refreshCurrentAlert(regionName: String = _config.value.regionName) {
        viewModelScope.launch {
            _isCheckingAlert.value = true
            val result = AerialAlertApiClient.checkAlertStatus(getApplication(), regionName)
            _currentAlert.value = result
            _isCheckingAlert.value = false
        }
    }
}
