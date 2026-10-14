package com.danmaryn.alertalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.danmaryn.alertalarm.data.repository.AlarmPreferencesRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val repository = AlarmPreferencesRepository(context)
            val config = repository.getAlarmConfig()
            if (config.isEnabled) {
                AlarmScheduler.scheduleAlarm(context, config)
            }
        }
    }
}
