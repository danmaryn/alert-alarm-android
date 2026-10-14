package com.danmaryn.alertalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.danmaryn.alertalarm.service.AlarmEvaluationService
import com.danmaryn.alertalarm.service.AlarmRingtoneService
import com.danmaryn.alertalarm.service.AlarmScheduler

class AlarmActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.danmaryn.alertalarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.danmaryn.alertalarm.ACTION_SNOOZE"
        const val ACTION_FORCE_WAKE = "com.danmaryn.alertalarm.ACTION_FORCE_WAKE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        when (intent.action) {
            ACTION_DISMISS -> {
                val serviceIntent = Intent(context, AlarmRingtoneService::class.java).apply {
                    action = AlarmRingtoneService.ACTION_DISMISS
                }
                context.startService(serviceIntent)
            }
            ACTION_SNOOZE -> {
                val serviceIntent = Intent(context, AlarmRingtoneService::class.java).apply {
                    action = AlarmRingtoneService.ACTION_SNOOZE
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                context.startService(serviceIntent)
            }
            ACTION_FORCE_WAKE -> {
                val serviceIntent = Intent(context, AlarmEvaluationService::class.java).apply {
                    action = AlarmEvaluationService.ACTION_FORCE_WAKE
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                context.startService(serviceIntent)
            }
        }
    }
}
