package com.danmaryn.alertalarm.service

import android.app.Notification
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
import com.danmaryn.alertalarm.data.model.AlarmConfig
import com.danmaryn.alertalarm.data.model.AlarmTriggerReason
import com.danmaryn.alertalarm.data.repository.AlarmPreferencesRepository
import com.danmaryn.alertalarm.receiver.AlarmActionReceiver
import com.danmaryn.alertalarm.ui.AlarmActivity

class AlarmRingtoneService : Service() {

    companion object {
        const val EXTRA_TRIGGER_REASON = "extra_trigger_reason"
        const val ACTION_DISMISS = "com.danmaryn.alertalarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.danmaryn.alertalarm.ACTION_SNOOZE"
        private const val NOTIFICATION_ID = 3001
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var screenWakeLock: PowerManager.WakeLock? = null
    private var triggerReason: AlarmTriggerReason = AlarmTriggerReason.SCHEDULED_NORMAL

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

        val reasonString = intent?.getStringExtra(EXTRA_TRIGGER_REASON)
        triggerReason = try {
            if (reasonString != null) AlarmTriggerReason.valueOf(reasonString)
            else AlarmTriggerReason.SCHEDULED_NORMAL
        } catch (e: Exception) {
            AlarmTriggerReason.SCHEDULED_NORMAL
        }

        acquireScreenWakeLock()
        startForeground(NOTIFICATION_ID, buildRingingNotification())
        startPlayback()

        // Also launch AlarmActivity directly
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_TRIGGER_REASON, triggerReason.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(activityIntent)

        return START_STICKY
    }

    private fun startPlayback() {
        // 1. Audio playback with USAGE_ALARM to bypass DND (Do Not Disturb)
        try {
            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Vibration
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
        screenWakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "AlertAlarm:ScreenWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L) // 10 minutes max
        }
    }

    private fun stopAlarm() {
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

    private fun snoozeAlarm() {
        val prefs = AlarmPreferencesRepository(this)
        val config = prefs.getAlarmConfig()
        // Snooze for 10 minutes
        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, 10)
        }
        val snoozedConfig = config.copy(
            hour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            minute = calendar.get(java.util.Calendar.MINUTE),
            isEnabled = true
        )
        AlarmScheduler.scheduleAlarm(this, snoozedConfig)
    }

    private fun buildRingingNotification(): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_TRIGGER_REASON, triggerReason.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            201,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            202,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (triggerReason) {
            AlarmTriggerReason.SCHEDULED_NORMAL -> getString(R.string.alarm_firing_normal)
            AlarmTriggerReason.ALERT_CLEARED -> getString(R.string.alarm_firing_clear)
            AlarmTriggerReason.DEADLINE_REACHED -> getString(R.string.alarm_firing_deadline)
            AlarmTriggerReason.OFFLINE_FAILSAFE -> getString(R.string.alarm_firing_offline)
        }

        return NotificationCompat.Builder(this, AlertAlarmApp.CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Натисніть для вимкнення")
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
