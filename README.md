# AlertAlarm – Smart Air Raid Alert-Aware Alarm Clock for Android

AlertAlarm is a native Android application designed for residents of Ukraine to optimize sleep and daily schedules during wartime conditions. The application intelligently adjusts alarm behavior based on real-time air raid alert statuses.

---

## Authorship & Attribution

- **Concept, Requirements & Product Logic:** Danylo Marynenko ([@danmaryn](https://github.com/danmaryn))
- **Software Engineering & Implementation:** Developed by Antigravity AI Agent powered by Google Gemini Flash.

---

## Problem Statement & Core Logic

Frequent nighttime and early-morning air raid alerts disrupt sleep schedules, halt public transportation, and postpone operational routines. Standard alarms wake users regardless of ongoing threats, often causing unnecessary sleep fragmentation.

AlertAlarm addresses this problem through the following deterministic state flow:

```
[ Scheduled Alarm Time Reached ]
               │
               ▼
   Is Internet Connection Available?
        ├── NO  ──────> [ Trigger Alarm Immediately (Offline Failsafe) ]
        └── YES
             │
             ▼
   Is Air Raid Alert Active in Region?
        ├── NO  ──────> [ Trigger Alarm Immediately (Normal Flow) ]
        └── YES
             │
             ▼
[ Suppress Alarm & Enter Background Monitoring ]
(Silent Foreground Service with Ongoing Status)
             │
             ├── [ All-Clear Received ] ──────> [ Trigger Alarm Immediately ]
             └── [ Hard Deadline Reached ] ───> [ Trigger Alarm Immediately ]
```

### Key Scenarios

1. **Normal Wake-Up:** If the region is calm at the scheduled time, the alarm fires normally.
2. **Alert Suppression:** If an alert is active at the scheduled time, sound is suppressed, and the app transitions into silent monitoring mode.
3. **All-Clear Trigger:** As soon as the air raid alert is canceled in the region, the alarm rings immediately.
4. **Hard Deadline Safeguard:** A configurable maximum wake-up time ensures the user is awakened even if an alert persists.
5. **Offline Failsafe:** If network connectivity is lost at the scheduled time, the alarm rings immediately to prevent oversleeping due to connectivity failures.

---

## Technical Architecture

The application is built in Kotlin adhering to modern Android architecture principles:

- **Exact Alarms (`AlarmManager.setAlarmClock`):** Guarantees execution from deep system sleep (Doze Mode) and displays the native system alarm indicator in the status bar.
- **Controlled Concurrency & WakeLock:** `WakeLock` is held strictly during the network evaluation phase to prevent CPU suspension before decision execution.
- **Audio & DND Bypass (`AudioAttributes.USAGE_ALARM`):** Plays audio via the dedicated alarm audio stream to ensure sound audibility even when Do Not Disturb (DND) mode is active.
- **Lockscreen Integration:** Full-screen intent (`AlarmActivity`) wakes the screen and presents dismiss/snooze controls over the lockscreen.
- **Foreground Service Monitoring:** Long-running evaluation during alerts complies with Android 14+ background execution policies via `specialUse` foreground service types.
- **Declarative UI:** Interface crafted with Jetpack Compose and Material 3, supporting dark theme and dynamic system styling.

---

## Project Structure

```
com.danmaryn.alertalarm/
├── AlertAlarmApp.kt               # Application class & Notification Channel setup
├── data/
│   ├── model/
│   │   ├── AlarmConfig.kt         # Alarm configuration model and time calculations
│   │   ├── AlertStatus.kt         # API response models and trigger reasons
│   │   └── Region.kt              # List of Ukrainian administrative regions
│   ├── remote/
│   │   └── AerialAlertApiClient.kt# HTTP client for real-time alert data
│   └── repository/
│       └── AlarmPreferencesRepository.kt # Persistence layer via SharedPreferences
├── service/
│   ├── AlarmReceiver.kt           # Wakeup broadcast receiver from AlarmManager
│   ├── AlarmScheduler.kt          # Exact alarm scheduling utilities
│   ├── AlarmEvaluationService.kt  # Foreground evaluation and polling loop
│   ├── AlarmRingtoneService.kt    # Ringtone playback, vibration, and lockscreen caller
│   └── BootReceiver.kt            # Re-schedules active alarms on device reboot
├── receiver/
│   └── AlarmActionReceiver.kt     # Handles notification actions (Dismiss, Snooze, Force Wake)
└── ui/
    ├── MainActivity.kt            # Primary configuration screen (Jetpack Compose)
    ├── AlarmActivity.kt           # Fullscreen alarm ringing activity
    ├── MainViewModel.kt           # UI state holder and business logic coordinator
    └── theme/                     # Material 3 color schemes, typography, and styling
```

---

## Permissions Overview

| Permission | Purpose |
| :--- | :--- |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Precise system wake-up at the exact scheduled minute |
| `WAKE_LOCK` | Keeps processor awake while evaluating alert status over HTTP |
| `FOREGROUND_SERVICE` | Runs silent monitoring service while awaiting alert all-clear |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Complies with Android 14 requirements for custom alarm logic |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Reliable continuous playback of the alarm audio stream |
| `USE_FULL_SCREEN_INTENT` | Launches alarm activity directly over the secure lockscreen |
| `RECEIVE_BOOT_COMPLETED` | Restores scheduled alarms automatically after device restart |
| `POST_NOTIFICATIONS` | Displays alarm and status notifications on Android 13+ |
| `INTERNET` & `ACCESS_NETWORK_STATE` | Queries real-time air alert status and validates connectivity |

---

## Build and Installation

### Prerequisites
- JDK 17
- Android SDK Platform 34 (API 34)
- Gradle 8.8 (bundled via Gradle Wrapper)

### Compilation
To build the debug APK via command line:

```bash
# On Linux / macOS:
./gradlew assembleDebug

# On Windows:
.\gradlew.bat assembleDebug
```

The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## License

This project is licensed under the [MIT License](LICENSE).
