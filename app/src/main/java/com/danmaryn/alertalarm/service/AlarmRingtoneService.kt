package com.danmaryn.alertalarm.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.danmaryn.alertalarm.AlertAlarmApp
import com.danmaryn.alertalarm.R
import com.danmaryn.alertalarm.data.model.AlarmTriggerReason
import com.danmaryn.alertalarm.data.repository.AlarmPreferencesRepository
import com.danmaryn.alertalarm.receiver.AlarmActionReceiver
import com.danmaryn.alertalarm.ui.AlarmActivity

class AlarmRingtoneService : Service() {

    companion object {
        const val EXTRA_TRIGGER_REASON = "extra_trigger_reason"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        const val EXTRA_LABEL = "extra_label"
        const val ACTION_DISMISS = "com.danmaryn.alertalarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.danmaryn.alertalarm.ACTION_SNOOZE"
        private const val NOTIFICATION_ID = 3001
        private val lock = Any()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var screenWakeLock: PowerManager.WakeLock? = null
    private var triggerReason: AlarmTriggerReason = AlarmTriggerReason.SCHEDULED_NORMAL
    private var alarmLabel: String = "Нагадування"
    private var alarmVolume: Float = 0.9f
    private var ringtoneUriString: String? = null
    private var alarmId: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISMISS) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        } else if (action == ACTION_SNOOZE) {
            snoozeAlarm()
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        alarmLabel = intent?.getStringExtra(EXTRA_LABEL) ?: "Нагадування"
        alarmVolume = intent?.getFloatExtra(EXTRA_VOLUME, 0.9f) ?: 0.9f
        ringtoneUriString = intent?.getStringExtra(EXTRA_RINGTONE_URI)

        val reasonString = intent?.getStringExtra(EXTRA_TRIGGER_REASON)
        triggerReason = try {
            if (reasonString != null) AlarmTriggerReason.valueOf(reasonString)
            else AlarmTriggerReason.SCHEDULED_NORMAL
        } catch (e: Exception) {
            AlarmTriggerReason.SCHEDULED_NORMAL
        }

        synchronized(lock) {
            // Wake up display immediately via hardware wake lock
            acquireScreenWakeLock()

            val notification = buildRingingNotification()
            startForeground(NOTIFICATION_ID, notification)

            if (mediaPlayer == null) {
                startPlayback()
            }

            // Launch AlarmActivity over lockscreen
            val activityIntent = Intent(this, AlarmActivity::class.java).apply {
                putExtra(EXTRA_TRIGGER_REASON, triggerReason.name)
                putExtra(EXTRA_LABEL, alarmLabel)
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            try {
                startActivity(activityIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return START_STICKY
    }

    private fun startPlayback() {
        try {
            val alertUri: Uri = if (!ringtoneUriString.isNullOrEmpty()) {
                try {
                    Uri.parse(ringtoneUriString)
                } catch (e: Exception) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                val clampedVolume = alarmVolume.coerceIn(0.05f, 1.0f)
                setVolume(clampedVolume, clampedVolume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, fallbackUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    val clampedVolume = alarmVolume.coerceIn(0.05f, 1.0f)
                    setVolume(clampedVolume, clampedVolume)
                    prepare()
                    start()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // Vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 1000, 600, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireScreenWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        @Suppress("DEPRECATION")
        screenWakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "AlertAlarm:ScreenWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(15 * 60 * 1000L) // 15 minutes maximum
        }
    }

    private fun stopAlarm() {
        synchronized(lock) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                vibrator?.cancel()
                vibrator = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                screenWakeLock?.let {
                    if (it.isHeld) it.release()
                }
                screenWakeLock = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun snoozeAlarm() {
        val currentId = alarmId ?: return
        val prefs = AlarmPreferencesRepository(this)
        val item = prefs.getAlarmById(currentId) ?: return

        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, 10)
        }
        val snoozed = item.copy(
            hour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            minute = calendar.get(java.util.Calendar.MINUTE),
            isEnabled = true
        )
        AlarmScheduler.scheduleAlarm(this, snoozed)
    }

    private fun buildRingingNotification(): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_TRIGGER_REASON, triggerReason.name)
            putExtra(EXTRA_LABEL, alarmLabel)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val uniqueReqCode = (System.currentTimeMillis() % 100000).toInt()
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            uniqueReqCode,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            201,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            202,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = when (triggerReason) {
            AlarmTriggerReason.SCHEDULED_NORMAL -> "Спрацював за розкладом"
            AlarmTriggerReason.ALERT_CLEARED -> "Відбій тривоги!"
            AlarmTriggerReason.DEADLINE_REACHED -> "Час остаточного спрацювання"
            AlarmTriggerReason.OFFLINE_FAILSAFE -> "Спрацював за розкладом"
        }

        return NotificationCompat.Builder(this, AlertAlarmApp.CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alarmLabel)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.action_dismiss), dismissPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.action_snooze), snoozePendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
