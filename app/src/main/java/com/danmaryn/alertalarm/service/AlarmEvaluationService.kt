package com.danmaryn.alertalarm.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.danmaryn.alertalarm.AlertAlarmApp
import com.danmaryn.alertalarm.R
import com.danmaryn.alertalarm.data.model.AlarmItem
import com.danmaryn.alertalarm.data.model.AlarmTriggerReason
import com.danmaryn.alertalarm.data.remote.AerialAlertApiClient
import com.danmaryn.alertalarm.data.repository.AlarmPreferencesRepository
import com.danmaryn.alertalarm.receiver.AlarmActionReceiver
import com.danmaryn.alertalarm.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmEvaluationService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHECK_INTERVAL_MS = 25_000L // 25 seconds guarantees < 1 minute precision
        const val ACTION_FORCE_WAKE = "com.danmaryn.alertalarm.ACTION_FORCE_WAKE"
        private const val WAKELOCK_TAG = "AlertAlarm:EvaluationWakeLock"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var preferencesRepository: AlarmPreferencesRepository
    private var serviceWakeLock: PowerManager.WakeLock? = null
    private var currentAlarmItem: AlarmItem? = null

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = AlarmPreferencesRepository(this)
        acquireServiceWakeLock()
    }

    private fun acquireServiceWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        serviceWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
            // Hold wake lock for up to 3 hours maximum safeguard (alert duration)
            acquire(3 * 60 * 60 * 1000L)
        }
    }

    private fun releaseServiceWakeLock() {
        try {
            serviceWakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serviceWakeLock = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        val isDeadline = intent?.getBooleanExtra(AlarmScheduler.EXTRA_IS_DEADLINE, false) ?: false

        currentAlarmItem = if (alarmId != null) {
            preferencesRepository.getAlarmById(alarmId)
        } else {
            preferencesRepository.getAllAlarms().firstOrNull()
        }

        val item = currentAlarmItem
        if (item == null || !item.isEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_FORCE_WAKE) {
            triggerRinging(item, AlarmTriggerReason.SCHEDULED_NORMAL)
            stopSelf()
            return START_NOT_STICKY
        }

        // If this invocation came directly from the exact deadline alarm, trigger immediately!
        if (isDeadline) {
            triggerRinging(item, AlarmTriggerReason.DEADLINE_REACHED)
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = buildMonitoringNotification(
            title = item.label,
            text = "Перевірка статусу повітряної тривоги..."
        )
        startForeground(NOTIFICATION_ID, initialNotification)

        evaluateAndMonitor(item)
        return START_NOT_STICKY
    }

    private fun evaluateAndMonitor(item: AlarmItem) {
        serviceScope.launch {
            // 1. Check network connectivity.
            if (!AerialAlertApiClient.isNetworkAvailable(applicationContext)) {
                triggerRinging(item, AlarmTriggerReason.OFFLINE_FAILSAFE)
                stopSelf()
                return@launch
            }

            // 2. Fetch air raid alert status
            val initialStatus = AerialAlertApiClient.checkAlertStatus(applicationContext, item.regionName)

            if (!initialStatus.isNetworkAvailable || initialStatus.errorMessage != null) {
                triggerRinging(item, AlarmTriggerReason.OFFLINE_FAILSAFE)
                stopSelf()
                return@launch
            }

            // If smart delay is disabled or there is NO active alert, trigger immediately
            if (!item.smartDelayEnabled || !initialStatus.isAlertActive) {
                triggerRinging(item, AlarmTriggerReason.SCHEDULED_NORMAL)
                stopSelf()
                return@launch
            }

            // 3. Air alert is active. Enter monitoring loop with ongoing notification.
            val deadlineMillis = item.calculateDeadlineMillis()
            val waitingNotification = buildMonitoringNotification(
                title = item.label,
                text = "${item.regionName}: очікування відбою. Час остаточного спрацювання: ${item.formattedDeadlineTime}",
                showForceWakeAction = true
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(NOTIFICATION_ID, waitingNotification)

            // 4. Polling loop with high precision (25 sec)
            while (isActive) {
                delay(CHECK_INTERVAL_MS)

                val currentTime = System.currentTimeMillis()
                if (currentTime >= deadlineMillis) {
                    triggerRinging(item, AlarmTriggerReason.DEADLINE_REACHED)
                    break
                }

                val currentStatus = AerialAlertApiClient.checkAlertStatus(applicationContext, item.regionName)
                if (!currentStatus.isAlertActive && currentStatus.isNetworkAvailable) {
                    triggerRinging(item, AlarmTriggerReason.ALERT_CLEARED)
                    break
                }
            }

            stopSelf()
        }
    }

    private fun triggerRinging(item: AlarmItem, reason: AlarmTriggerReason) {
        AlarmReceiver.releaseWakeLock()
        releaseServiceWakeLock()

        val ringIntent = Intent(this, AlarmRingtoneService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, item.id)
            putExtra(AlarmRingtoneService.EXTRA_TRIGGER_REASON, reason.name)
            putExtra(AlarmRingtoneService.EXTRA_VOLUME, item.volume)
            putExtra(AlarmRingtoneService.EXTRA_RINGTONE_URI, item.ringtoneUri)
            putExtra(AlarmRingtoneService.EXTRA_LABEL, item.label)
        }
        ContextCompat.startForegroundService(this, ringIntent)
    }

    private fun buildMonitoringNotification(
        title: String,
        text: String,
        showForceWakeAction: Boolean = false
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, AlertAlarmApp.CHANNEL_MONITOR_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)

        if (showForceWakeAction) {
            val forceWakeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
                action = AlarmActionReceiver.ACTION_FORCE_WAKE
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmItem?.id)
            }
            val forceWakePendingIntent = PendingIntent.getBroadcast(
                this,
                102,
                forceWakeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.action_force_wake),
                forceWakePendingIntent
            )
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        AlarmReceiver.releaseWakeLock()
        releaseServiceWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
