# AlertAlarm – Smart Air Raid Alert-Aware Alarm & Reminder for Android

AlertAlarm is a native Android application designed to optimize daily routines and scheduled events during wartime conditions in Ukraine. The application dynamically adjusts alarm timing based on real-time air raid alert statuses.

---

## Authorship & Attribution

- **Concept, Requirements & Product Logic:** Danylo Marynenko ([@danmaryn](https://github.com/danmaryn))
- **Software Engineering & Implementation:** Developed by Antigravity AI Agent powered by Google Gemini Flash.

---

## Problem Statement & Operational Logic

Air raid alerts regularly disrupt daily activities, close supermarkets and administrative centers, halt public transport, and alter schedules. Traditional alarms trigger unconditionally, forcing users to interrupt sleep or prepare for tasks that cannot be performed while an alert is active.

AlertAlarm provides an intelligent, event-driven scheduling workflow:

```
[ Scheduled Alarm Time Reached ]
               │
               ▼
   Is Internet Connection Available?
        ├── NO  ──────> [ Trigger Alarm Immediately (Failsafe) ]
        └── YES
             │
             ▼
   Is Air Raid Alert Active in Region?
        ├── NO  ──────> [ Trigger Alarm Immediately (Normal Flow) ]
        └── YES
             │
             ▼
[ Suppress Alarm & Enter Precision Monitoring ]
(Foreground Service with Sub-Minute Polling & Active WakeLock)
             │
             ├── [ All-Clear Received ] ──────────────────> [ Trigger Alarm Immediately ]
             └── [ Final Scheduled Time (Deadline) ] ────> [ Trigger Alarm Immediately ]
```

### Core Features

1. **Multiple Alarms & Custom Labels:** Create distinct alarms for various routines (e.g. "Підйом", "Магазин", "Зустріч", "Робота").
2. **Alert Delay & All-Clear Ringing:** If an alert is active in the selected region at the scheduled time, ringing is suppressed until the all-clear signal is detected.
3. **Час остаточного спрацювання (Final Deadline):** Guarantees that the alarm will ring no later than the specified time, regardless of ongoing alerts.
4. **Sub-Minute Precision (< 1 min):** Synchronized exact scheduling and battery-optimized 25-second evaluation cycles eliminate sleep delay on modern devices (including aggressive battery managers such as Samsung One UI).
5. **Volume Control & Custom Audio:** Adjustable volume slider (5% to 100%) and support for both system ringtones and user-selected audio files via the Android Storage Access Framework.
6. **Unified Audio Pipeline:** Single-instance playback with dedicated alarm audio attributes (`USAGE_ALARM`), bypassing Do Not Disturb (DND) modes without sound doubling.
7. **Offline Failsafe:** Rings on time if no network connection is available.

---

## Technical Architecture

The application is built in Kotlin adhering to modern Android architecture principles:

- **Exact Alarm Scheduling (`AlarmManager.setAlarmClock`):** Scheduled times and final deadline timestamps are registered directly with the OS kernel to pierce Doze mode and Samsung One UI sleep policies.
- **Power Management:** Controlled `PARTIAL_WAKE_LOCK` prevents CPU suspension during active alert monitoring without significant power consumption.
- **Audio & DND Bypass (`AudioAttributes.USAGE_ALARM`):** Playback directly utilizes MediaPlayer with isolated channel routing, preventing system notification sound collisions.
- **Lockscreen Integration:** Full-screen intent (`AlarmActivity`) turns on the screen and presents dismiss/snooze controls over the lockscreen.
- **Modern UI:** Declarative interface using Jetpack Compose and Material 3, including custom time dial selection and region picker.

---

## Permissions Overview

| Permission | Purpose |
| :--- | :--- |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Precise system wake-up at the exact scheduled minute |
| `WAKE_LOCK` | Maintains CPU execution state during alert evaluation |
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
