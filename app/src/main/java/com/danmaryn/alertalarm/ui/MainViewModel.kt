package com.danmaryn.alertalarm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danmaryn.alertalarm.data.model.AlarmItem
import com.danmaryn.alertalarm.data.model.AlertCheckResult
import com.danmaryn.alertalarm.data.model.AppSettings
import com.danmaryn.alertalarm.data.remote.AerialAlertApiClient
import com.danmaryn.alertalarm.data.repository.AlarmPreferencesRepository
import com.danmaryn.alertalarm.data.repository.AppSettingsRepository
import com.danmaryn.alertalarm.service.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val alarmRepository = AlarmPreferencesRepository(application)
    private val settingsRepository = AppSettingsRepository(application)

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _settings = MutableStateFlow(settingsRepository.getSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _currentAlert = MutableStateFlow<AlertCheckResult?>(null)
    val currentAlert: StateFlow<AlertCheckResult?> = _currentAlert.asStateFlow()

    private val _isCheckingAlert = MutableStateFlow(false)
    val isCheckingAlert: StateFlow<Boolean> = _isCheckingAlert.asStateFlow()

    /**
     * Determines the active region to monitor:
     * - Nearest enabled alarm's region if any exists
     * - Otherwise fallback to settings default region
     */
    val activeMonitoringRegion: StateFlow<String> = combine(_alarms, _settings) { alarmsList, appSettings ->
        val nearestActive = alarmsList
            .filter { it.isEnabled }
            .minByOrNull { it.calculateTargetTimeMillis() }
        nearestActive?.regionName ?: appSettings.defaultRegion
    }.stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.getSettings().defaultRegion)

    init {
        loadAlarms()
        viewModelScope.launch {
            activeMonitoringRegion.collect { region ->
                refreshAlertStatus(region)
            }
        }
    }

    fun loadAlarms() {
        _alarms.value = alarmRepository.getAllAlarms()
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        settingsRepository.saveSettings(newSettings)
    }

    fun toggleAlarmEnabled(item: AlarmItem, enabled: Boolean) {
        val updated = item.copy(isEnabled = enabled)
        saveAlarm(updated)
    }

    fun saveAlarm(item: AlarmItem) {
        alarmRepository.saveOrUpdateAlarm(item)
        loadAlarms()

        val context = getApplication<Application>()
        if (item.isEnabled) {
            AlarmScheduler.scheduleAlarm(context, item)
        } else {
            AlarmScheduler.cancelAlarm(context, item)
        }
    }

    fun deleteAlarm(item: AlarmItem) {
        val context = getApplication<Application>()
        AlarmScheduler.cancelAlarm(context, item)
        alarmRepository.deleteAlarm(item.id)
        loadAlarms()
    }

    fun refreshAlertStatus(regionName: String = activeMonitoringRegion.value) {
        viewModelScope.launch {
            _isCheckingAlert.value = true
            val result = AerialAlertApiClient.checkAlertStatus(getApplication(), regionName)
            _currentAlert.value = result
            _isCheckingAlert.value = false
        }
    }
}
