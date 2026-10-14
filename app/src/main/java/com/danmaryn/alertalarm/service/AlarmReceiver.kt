package com.danmaryn.alertalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.danmaryn.alertalarm.ACTION_ALARM_TRIGGER"
        private var wakeLock: PowerManager.WakeLock? = null

        fun acquireWakeLock(context: Context) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AlertAlarm:AlarmReceiverWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(60 * 1000L) // 1 minute timeout safeguard
            }
        }

        fun releaseWakeLock() {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ALARM_TRIGGER) {
            acquireWakeLock(context)

            val serviceIntent = Intent(context, AlarmEvaluationService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
