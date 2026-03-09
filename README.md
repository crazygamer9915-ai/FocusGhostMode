# 👻 Focus Ghost Mode

**A deep-focus Android app that silently captures everything while you focus — then replays it like a ghost timeline.**

---

## Screenshots Preview

```
┌─────────────────────────┐    ┌─────────────────────────┐
│   Focus Ghost Mode      │    │   Session Replay        │
│                         │    │                         │
│        👻               │    │  FOCUS SESSION SUMMARY  │
│                         │    │  March 09, 2026  09:15  │
│  [● GHOST MODE ACTIVE]  │    │  1h 23m                 │
│                         │    │  12 notifs  3 calls     │
│       01:23:45          │    │                         │
│                         │    │  TIMELINE REPLAY        │
│  [ End Ghost Mode ]     │    │  09:15 🔔 WhatsApp…     │
│                         │    │  09:20 🔔 Gmail…        │
│  [ View History ]       │    │  09:42 📞 Missed: Mom   │
└─────────────────────────┘    └─────────────────────────┘
```

---

## Features

- **Ghost Mode Toggle** — Activate/deactivate with one tap
- **Silent Capture** — Notifications and calls logged without disturbing you
- **Timeline Replay** — Beautiful scrollable timeline after each session
- **Focus Stats** — Duration, notification count, call count, most active app
- **Session History** — Browse all past sessions, tap to view timeline
- **Export** — Share as CSV or formatted text report
- **Material Design 3** — Dark theme, smooth animations

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Material Design 3, ViewBinding |
| Database | Room (SQLite) |
| Concurrency | Kotlin Coroutines + Flow |
| Notification Capture | NotificationListenerService |
| Call Detection | TelephonyManager BroadcastReceiver |
| Background | Foreground Service |
| Architecture | MVVM + Repository Pattern |

---

## Project Structure

```
app/src/main/java/com/focusghostmode/
├── GhostApplication.kt              # Application class
├── data/
│   ├── db/
│   │   ├── GhostDatabase.kt         # Room database
│   │   └── GhostDao.kt              # DAOs for sessions + events
│   ├── model/
│   │   ├── FocusSession.kt          # Session entity
│   │   └── CapturedEvent.kt         # Notification/Call entity
│   └── repository/
│       └── GhostRepository.kt       # Single source of truth
├── service/
│   ├── GhostNotificationService.kt  # NotificationListenerService
│   ├── GhostForegroundService.kt    # Keeps ghost mode alive, manages DND
│   ├── PhoneStateReceiver.kt        # Captures calls
│   └── BootReceiver.kt              # Restores after reboot
├── ui/
│   ├── home/
│   │   ├── MainActivity.kt          # Home screen
│   │   └── HomeViewModel.kt         # Home state management
│   ├── timeline/
│   │   ├── TimelineActivity.kt      # Session replay screen
│   │   └── TimelineAdapter.kt       # Timeline RecyclerView
│   ├── history/
│   │   ├── HistoryActivity.kt       # Past sessions list
│   │   └── HistoryAdapter.kt        # Session cards
│   └── onboarding/
│       └── OnboardingActivity.kt    # Permission setup wizard
└── utils/
    ├── GhostPreferences.kt          # SharedPreferences wrapper
    └── Utils.kt                     # TimeUtils + ExportUtils
```

---

## Build Instructions

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK** with API 34 installed

### Steps

1. **Open the project in Android Studio:**
   - File → Open → Select the `FocusGhostMode` folder

2. **Sync Gradle:**
   - Android Studio will prompt to sync. Click "Sync Now"
   - Or: File → Sync Project with Gradle Files

3. **Build the APK:**
   ```
   Build → Build Bundle(s)/APK(s) → Build APK(s)
   ```
   The APK will be at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Or build via command line:**
   ```bash
   # On Linux/Mac:
   ./gradlew assembleDebug
   
   # On Windows:
   gradlew.bat assembleDebug
   ```

5. **Install on device:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Required Permissions Setup (On Device)

After installing, the app guides you through:

1. **Notification Access** *(Required)*
   - Settings → Apps → Special app access → Notification access → Enable for Ghost Mode

2. **Do Not Disturb Access** *(Recommended)*
   - Settings → Apps → Special app access → Do Not Disturb access → Enable

3. **Phone & Contacts** *(Recommended)*
   - Standard runtime permission — tap Grant in the onboarding screen

---

## Architecture Notes

### Notification Capture Flow
```
NotificationListenerService.onNotificationPosted()
    → Check if Ghost Mode is active (GhostPreferences)
    → Extract package, title, text
    → GhostRepository.captureNotification()
    → Room DB insert
```

### Call Capture Flow
```
PhoneStateReceiver.onReceive()
    → TelephonyManager.EXTRA_STATE_RINGING → log as INCOMING
    → State goes RINGING → IDLE (no OFFHOOK) → log as MISSED
    → Look up contact name via ContactsContract
    → GhostRepository.captureCall()
    → Room DB insert
```

### Ghost Mode Activation Flow
```
User taps "Activate Ghost Mode"
    → Start GhostForegroundService (ACTION_START)
    → GhostRepository.startSession() → get sessionId
    → Save sessionId + startTime to GhostPreferences
    → Enable Do Not Disturb (if permission granted)
    → Show persistent ghost indicator notification
```

---

## Minimum Requirements

- Android 10 (API 29) or higher
- ~15 MB storage for app
- Works on any screen size

---

## License

MIT License — Free to use and modify.
