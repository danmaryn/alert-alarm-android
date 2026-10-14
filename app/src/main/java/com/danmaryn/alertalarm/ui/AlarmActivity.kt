package com.danmaryn.alertalarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmaryn.alertalarm.data.model.AlarmTriggerReason
import com.danmaryn.alertalarm.service.AlarmRingtoneService
import com.danmaryn.alertalarm.service.AlarmScheduler
import com.danmaryn.alertalarm.ui.theme.AlertAlarmTheme
import com.danmaryn.alertalarm.ui.theme.DarkBackground
import com.danmaryn.alertalarm.ui.theme.StatusAmber
import com.danmaryn.alertalarm.ui.theme.StatusGreen
import com.danmaryn.alertalarm.ui.theme.StatusRed
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenFlags()

        val label = intent.getStringExtra(AlarmRingtoneService.EXTRA_LABEL) ?: "Нагадування"
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        val reasonString = intent.getStringExtra(AlarmRingtoneService.EXTRA_TRIGGER_REASON)
        val triggerReason = try {
            if (reasonString != null) AlarmTriggerReason.valueOf(reasonString)
            else AlarmTriggerReason.SCHEDULED_NORMAL
        } catch (e: Exception) {
            AlarmTriggerReason.SCHEDULED_NORMAL
        }

        setContent {
            AlertAlarmTheme(darkTheme = true) {
                AlarmScreen(
                    label = label,
                    reason = triggerReason,
                    onDismiss = {
                        sendServiceAction(AlarmRingtoneService.ACTION_DISMISS, alarmId)
                        finish()
                    },
                    onSnooze = {
                        sendServiceAction(AlarmRingtoneService.ACTION_SNOOZE, alarmId)
                        finish()
                    }
                )
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun sendServiceAction(actionName: String, alarmId: String?) {
        val intent = Intent(this, AlarmRingtoneService::class.java).apply {
            action = actionName
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        startService(intent)
    }
}

@Composable
fun AlarmScreen(
    label: String,
    reason: AlarmTriggerReason,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTimeString by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    val (titleText, badgeColor, descriptionText) = when (reason) {
        AlarmTriggerReason.SCHEDULED_NORMAL -> Triple(
            label,
            StatusGreen,
            "Спрацював за розкладом"
        )
        AlarmTriggerReason.ALERT_CLEARED -> Triple(
            label,
            StatusGreen,
            "Відбій повітряної тривоги!"
        )
        AlarmTriggerReason.DEADLINE_REACHED -> Triple(
            label,
            StatusAmber,
            "Час остаточного спрацювання"
        )
        AlarmTriggerReason.OFFLINE_FAILSAFE -> Triple(
            label,
            StatusGreen,
            "Спрацював за розкладом"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(badgeColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Text(
                text = currentTimeString,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.AlarmOff, contentDescription = null)
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "Вимкнути",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Snooze, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Відкласти (+10 хв)", fontSize = 16.sp)
                }
            }
        }
    }
}
