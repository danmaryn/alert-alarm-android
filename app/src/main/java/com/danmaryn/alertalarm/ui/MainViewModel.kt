package com.danmaryn.alertalarm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danmaryn.alertalarm.data.model.AlarmItem
import com.danmaryn.alertalarm.data.model.AlertCheckResult
import com.danmaryn.alertalarm.data.model.UkrainianRegions
import com.danmaryn.alertalarm.data.remote.AerialAlertApiClient
import com.danmaryn.alertalarm.data.repository.AlarmPreferencesRepository
import com.danmaryn.alertalarm.service.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlarmPreferencesRepository(application)

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _currentAlert = MutableStateFlow<AlertCheckResult?>(null)
    val currentAlert: StateFlow<AlertCheckResult?> = _currentAlert.asStateFlow()

    private val _isCheckingAlert = MutableStateFlow(false)
    val isCheckingAlert: StateFlow<Boolean> = _isCheckingAlert.asStateFlow()

    init {
        loadAlarms()
        refreshCurrentAlert(UkrainianRegions.DEFAULT_REGION)
    }

    fun loadAlarms() {
        _alarms.value = repository.getAllAlarms()
    }

    fun toggleAlarmEnabled(item: AlarmItem, enabled: Boolean) {
        val updated = item.copy(isEnabled = enabled)
        saveAlarm(updated)
    }

    fun saveAlarm(item: AlarmItem) {
        repository.saveOrUpdateAlarm(item)
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
        repository.deleteAlarm(item.id)
        loadAlarms()
    }

    fun refreshCurrentAlert(regionName: String = UkrainianRegions.DEFAULT_REGION) {
        viewModelScope.launch {
            _isCheckingAlert.value = true
            val result = AerialAlertApiClient.checkAlertStatus(getApplication(), regionName)
            _currentAlert.value = result
            _isCheckingAlert.value = false
        }
    }
}
