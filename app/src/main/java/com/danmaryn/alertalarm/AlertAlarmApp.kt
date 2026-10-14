package com.danmaryn.alertalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AlertAlarmApp : Application() {

    companion object {
        const val CHANNEL_ALARM_ID = "channel_alarm_ring_v2"
        const val CHANNEL_MONITOR_ID = "channel_alarm_monitor_v2"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High priority channel for active alarm notification.
            // Sound is explicitly set to null because audio playback is managed directly
            // by AlarmRingtoneService via MediaPlayer with custom volume and audio tracks.
            // This prevents duplicate sound overlapping.
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alarm_desc)
                enableVibration(false) // Managed directly by service vibrator
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Low priority channel for ongoing silent monitoring during active air alerts
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_ID,
                getString(R.string.channel_monitor_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_monitor_desc)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager?.createNotificationChannel(alarmChannel)
            notificationManager?.createNotificationChannel(monitorChannel)
        }
    }
}
