package com.danmaryn.alertalarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.danmaryn.alertalarm.data.model.AlarmItem
import com.danmaryn.alertalarm.ui.MainActivity

object AlarmScheduler {

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_IS_DEADLINE = "extra_is_deadline"

    /**
     * Schedules exact alarm for an AlarmItem via setAlarmClock.
     * Also schedules exact deadline alarm as an authoritative safeguard.
     */
    fun scheduleAlarm(context: Context, item: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return
            }
        }

        val triggerTimeMillis = item.calculateTargetTimeMillis()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, item.id)
            putExtra(EXTRA_IS_DEADLINE, false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            item.requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        // If smart delay is enabled, schedule the deadline via setAlarmClock as well.
        // This guarantees that the deadline fires at the exact minute without any sleep delay.
        if (item.smartDelayEnabled) {
            val deadlineMillis = item.calculateDeadlineMillis()
            val deadlineIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(EXTRA_ALARM_ID, item.id)
                putExtra(EXTRA_IS_DEADLINE, true)
            }
            val deadlinePendingIntent = PendingIntent.getBroadcast(
                context,
                item.deadlineRequestCode,
                deadlineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val deadlineClockInfo = AlarmManager.AlarmClockInfo(deadlineMillis, showPendingIntent)
            alarmManager.setAlarmClock(deadlineClockInfo, deadlinePendingIntent)
        }
    }

    /**
     * Cancels scheduled alarms for a specific AlarmItem.
     */
    fun cancelAlarm(context: Context, item: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel primary alarm
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, item.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Cancel deadline alarm
        val deadlineIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, item.id)
        }
        val deadlinePendingIntent = PendingIntent.getBroadcast(
            context,
            item.deadlineRequestCode,
            deadlineIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (deadlinePendingIntent != null) {
            alarmManager.cancel(deadlinePendingIntent)
            deadlinePendingIntent.cancel()
        }
    }
}
