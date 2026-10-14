package com.danmaryn.alertalarm.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.danmaryn.alertalarm.data.model.AlarmItem
import com.danmaryn.alertalarm.data.model.AlertCheckResult
import com.danmaryn.alertalarm.data.model.AppLanguage
import com.danmaryn.alertalarm.data.model.UkrainianRegions
import com.danmaryn.alertalarm.ui.components.DaysOfWeekPicker
import com.danmaryn.alertalarm.ui.components.RegionPickerDialog
import com.danmaryn.alertalarm.ui.components.SettingsDialog
import com.danmaryn.alertalarm.ui.components.WheelTimePicker
import com.danmaryn.alertalarm.ui.i18n.AppStrings
import com.danmaryn.alertalarm.ui.theme.AlertAlarmTheme
import com.danmaryn.alertalarm.ui.theme.DarkBackground
import com.danmaryn.alertalarm.ui.theme.DarkSurface
import com.danmaryn.alertalarm.ui.theme.DarkSurfaceVariant
import com.danmaryn.alertalarm.ui.theme.StatusGreen
import com.danmaryn.alertalarm.ui.theme.StatusRed

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled reactively */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestSystemAlarmPermissions()

        setContent {
            val settings by viewModel.settings.collectAsState()
            AlertAlarmTheme(themeMode = settings.themeMode) {
                MainAlarmScreen(
                    viewModel = viewModel,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openAppNotificationSettings()
                        }
                    }
                )
            }
        }
    }

    private fun checkAndRequestSystemAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    fun openAppNotificationSettings() {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:$packageName")
            }
        }
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAlarmScreen(
    viewModel: MainViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    val alarms by viewModel.alarms.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val alertStatus by viewModel.currentAlert.collectAsState()
    val isChecking by viewModel.isCheckingAlert.collectAsState()
    val activeRegion by viewModel.activeMonitoringRegion.collectAsState()
    val context = LocalContext.current
    val lang = settings.language

    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Check notifications permission status
    var notificationsGranted by remember {
        mutableStateOf(isNotificationPermissionGranted(context))
    }

    LaunchedEffect(Unit) {
        notificationsGranted = isNotificationPermissionGranted(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.get("alarms_header", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAlertStatus() }) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.LightGray
                            )
                        }
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAlarm = AlarmItem(
                        label = AppStrings.get("new_alarm", lang),
                        hour = 7,
                        minute = 30,
                        regionName = settings.defaultRegion,
                        volume = settings.defaultVolume,
                        ringtoneUri = settings.defaultRingtoneUri,
                        ringtoneTitle = settings.defaultRingtoneTitle,
                        deadlineHour = 8,
                        deadlineMinute = 30
                    )
                    isCreatingNew = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Notification Permission Banner (Samsung One UI / iOS Style)
            if (!notificationsGranted) {
                item {
                    NotificationPermissionBanner(
                        lang = lang,
                        onEnableClick = {
                            onRequestNotificationPermission()
                            notificationsGranted = isNotificationPermissionGranted(context)
                        }
                    )
                }
            }

            // Live Alert Status Card for Active Region
            item {
                LiveAlertStatusCard(
                    regionName = activeRegion,
                    status = alertStatus,
                    lang = lang
                )
            }

            if (alarms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AppStrings.get("no_alarms", lang),
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(alarms, key = { it.id }) { item ->
                    AlarmItemCard(
                        item = item,
                        lang = lang,
                        onToggle = { enabled -> viewModel.toggleAlarmEnabled(item, enabled) },
                        onEdit = {
                            editingAlarm = item
                            isCreatingNew = false
                        },
                        onDelete = { viewModel.deleteAlarm(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    editingAlarm?.let { item ->
        AlarmEditDialog(
            alarm = item,
            isNew = isCreatingNew,
            lang = lang,
            onDismiss = { editingAlarm = null },
            onSave = { updated ->
                viewModel.saveAlarm(updated)
                editingAlarm = null
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            onSave = { updatedSettings ->
                viewModel.updateSettings(updatedSettings)
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

private fun isNotificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
    }
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    return nm?.areNotificationsEnabled() ?: true
}

@Composable
fun NotificationPermissionBanner(
    lang: AppLanguage,
    onEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = AppStrings.get("notif_perm_title", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = AppStrings.get("notif_perm_desc", lang),
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onEnableClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = AppStrings.get("notif_perm_btn", lang), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LiveAlertStatusCard(
    regionName: String,
    status: AlertCheckResult?,
    lang: AppLanguage
) {
    val isAlert = status?.isAlertActive == true
    val isOffline = status?.isNetworkAvailable == false
    val statusBg = when {
        isOffline -> Color(0xFF2C2C2E)
        isAlert -> StatusRed.copy(alpha = 0.16f)
        else -> StatusGreen.copy(alpha = 0.16f)
    }
    val statusColor = when {
        isOffline -> Color.LightGray
        isAlert -> StatusRed
        else -> StatusGreen
    }
    val statusText = when {
        isOffline -> AppStrings.get("alert_offline", lang)
        isAlert -> AppStrings.get("alert_active", lang)
        else -> AppStrings.get("alert_calm", lang)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = statusBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = regionName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isAlert) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StatusRed,
                    modifier = Modifier.size(24.dp)
                )
            } else if (!isOffline) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AlarmItemCard(
    item: AlarmItem,
    lang: AppLanguage,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.isEnabled) Color.White else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.formattedTime,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 46.sp),
                        color = if (item.isEnabled) Color.White else Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle with region, repeat days and deadline
            val daysStr = item.formatDaysSummary(lang)
            val detailsText = if (item.smartDelayEnabled) {
                "$daysStr • ${item.regionName} • ${AppStrings.get("deadline_time", lang)}: ${item.formattedDeadlineTime}"
            } else {
                "$daysStr • ${item.regionName}"
            }

            Text(
                text = detailsText,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.ringtoneTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmEditDialog(
    alarm: AlarmItem,
    isNew: Boolean,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(alarm.label) }
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var smartDelay by remember { mutableStateOf(alarm.smartDelayEnabled) }
    var deadlineHour by remember { mutableIntStateOf(alarm.deadlineHour) }
    var deadlineMinute by remember { mutableIntStateOf(alarm.deadlineMinute) }
    var region by remember { mutableStateOf(alarm.regionName) }
    var volume by remember { mutableFloatStateOf(alarm.volume) }
    var ringtoneUri by remember { mutableStateOf(alarm.ringtoneUri) }
    var ringtoneTitle by remember { mutableStateOf(alarm.ringtoneTitle) }
    var selectedDays by remember { mutableStateOf(alarm.daysOfWeek) }

    var showRegionPicker by remember { mutableStateOf(false) }

    // Launcher for system ringtone picker
    val systemRingtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                ringtoneUri = uri.toString()
                val r = RingtoneManager.getRingtone(context, uri)
                ringtoneTitle = r?.getTitle(context) ?: "Системний сигнал"
            }
        }
    }

    // Launcher for custom audio file picker
    val audioFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            ringtoneUri = uri.toString()

            var name = "Власний аудіофайл"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            ringtoneTitle = name
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) AppStrings.get("new_alarm", lang) else AppStrings.get("edit_alarm", lang),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tactile Wheel Time Picker for Alarm Trigger Time
                Text(
                    text = AppStrings.get("trigger_time", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                WheelTimePicker(
                    initialHour = hour,
                    initialMinute = minute,
                    onTimeChanged = { h, m ->
                        hour = h
                        minute = m
                    }
                )

                // Label input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(AppStrings.get("label", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Days of week repeat picker
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = AppStrings.get("repeat_days", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DaysOfWeekPicker(
                        selectedDays = selectedDays,
                        onDaysChanged = { selectedDays = it },
                        lang = lang
                    )
                }

                // Smart delay toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.get("smart_delay", lang),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = AppStrings.get("smart_delay_desc", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = smartDelay,
                        onCheckedChange = { smartDelay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Deadline Time with Wheel Picker (Active if smart delay is enabled)
                if (smartDelay) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = AppStrings.get("deadline_time", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        WheelTimePicker(
                            initialHour = deadlineHour,
                            initialMinute = deadlineMinute,
                            onTimeChanged = { h, m ->
                                deadlineHour = h
                                deadlineMinute = m
                            },
                            height = 140.dp
                        )
                    }
                }

                // Region selector card opening unified search modal
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showRegionPicker = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(AppStrings.get("region", lang), fontSize = 12.sp, color = Color.Gray)
                                Text(region, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                // Volume slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(AppStrings.get("volume", lang), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${(volume * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0.05f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Ringtone sound selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(AppStrings.get("sound", lang), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ringtoneTitle,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    if (!ringtoneUri.isNullOrEmpty()) {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(ringtoneUri))
                                    }
                                }
                                systemRingtoneLauncher.launch(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("system_sounds", lang), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                audioFileLauncher.launch(arrayOf("audio/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("custom_file", lang), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = alarm.copy(
                        label = label.ifBlank { AppStrings.get("new_alarm", lang) },
                        hour = hour,
                        minute = minute,
                        smartDelayEnabled = smartDelay,
                        deadlineHour = deadlineHour,
                        deadlineMinute = deadlineMinute,
                        regionName = region,
                        volume = volume,
                        ringtoneUri = ringtoneUri,
                        ringtoneTitle = ringtoneTitle,
                        daysOfWeek = selectedDays
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )

    if (showRegionPicker) {
        RegionPickerDialog(
            selectedRegion = region,
            onRegionSelected = { region = it },
            onDismiss = { showRegionPicker = false },
            lang = lang
        )
    }
}
