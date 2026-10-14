package com.danmaryn.alertalarm.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.danmaryn.alertalarm.AlertAlarmApp
import com.danmaryn.alertalarm.R
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
        private const val CHECK_INTERVAL_MS = 35_000L // 35 seconds
        const val ACTION_FORCE_WAKE = "com.danmaryn.alertalarm.ACTION_FORCE_WAKE"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var preferencesRepository: AlarmPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = AlarmPreferencesRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_FORCE_WAKE) {
            triggerRinging(AlarmTriggerReason.SCHEDULED_NORMAL)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground immediately with low-priority notification
        val initialNotification = buildMonitoringNotification(
            title = getString(R.string.alarm_waiting_title),
            text = "Перевірка статусу повітряної тривоги..."
        )
        startForeground(NOTIFICATION_ID, initialNotification)

        evaluateAndMonitor()
        return START_NOT_STICKY
    }

    private fun evaluateAndMonitor() {
        serviceScope.launch {
            val config = preferencesRepository.getAlarmConfig()

            // 1. Check network connectivity.
            // If offline, failsafe rule: ring immediately at scheduled time so user can decide.
            if (!AerialAlertApiClient.isNetworkAvailable(applicationContext)) {
                triggerRinging(AlarmTriggerReason.OFFLINE_FAILSAFE)
                stopSelf()
                return@launch
            }

            // 2. Fetch air raid alert status
            val initialStatus = AerialAlertApiClient.checkAlertStatus(applicationContext, config.regionName)

            // If API error or no network during response, ring as failsafe
            if (!initialStatus.isNetworkAvailable || initialStatus.errorMessage != null) {
                triggerRinging(AlarmTriggerReason.OFFLINE_FAILSAFE)
                stopSelf()
                return@launch
            }

            // If smart delay is disabled or there is NO active alert, ring normally!
            if (!config.smartDelayEnabled || !initialStatus.isAlertActive) {
                triggerRinging(AlarmTriggerReason.SCHEDULED_NORMAL)
                stopSelf()
                return@launch
            }

            // 3. Air raid alert is active! Do NOT ring yet.
            // Update notification to indicate silent waiting for alert clear.
            val deadlineMillis = config.calculateDeadlineMillis()
            val waitingNotification = buildMonitoringNotification(
                title = getString(R.string.alarm_waiting_title),
                text = getString(
                    R.string.alarm_waiting_text,
                    config.regionName,
                    config.formattedDeadlineTime
                ),
                showForceWakeAction = true
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(NOTIFICATION_ID, waitingNotification)

            // 4. Periodic polling loop until alert cleared or deadline reached
            while (isActive) {
                delay(CHECK_INTERVAL_MS)

                val currentTime = System.currentTimeMillis()

                // Check deadline condition
                if (currentTime >= deadlineMillis) {
                    triggerRinging(AlarmTriggerReason.DEADLINE_REACHED)
                    break
                }

                // Check alert status
                val currentStatus = AerialAlertApiClient.checkAlertStatus(applicationContext, config.regionName)

                // If alert has ended (alertnow == false)
                if (!currentStatus.isAlertActive && currentStatus.isNetworkAvailable) {
                    triggerRinging(AlarmTriggerReason.ALERT_CLEARED)
                    break
                }
            }

            stopSelf()
        }
    }

    private fun triggerRinging(reason: AlarmTriggerReason) {
        AlarmReceiver.releaseWakeLock()
        val ringIntent = Intent(this, AlarmRingtoneService::class.java).apply {
            putExtra(AlarmRingtoneService.EXTRA_TRIGGER_REASON, reason.name)
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
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
