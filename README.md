# Snooze Control ⏰

**Snooze Control** is an Android wake-up alarm clock app engineered specifically for heavy sleepers. Built with modern Android technologies—including **Jetpack Compose**, **Material 3**, **Room Database**, **CameraX**, and **Google ML Kit**—it forces physical and mental activity before turning off the alarm.

---

## 🌟 Key Features

### 🧩 Wake-Up Missions
- **Math Challenge:** Solve randomly generated arithmetic problems (addition, subtraction, multiplication) to stop the alarm ringing.
- **Barcode / QR Code Challenge:** Scan a pre-registered barcode or QR code (e.g., toothpaste in the bathroom, coffee container in the kitchen) to turn off the alarm, physically forcing you out of bed.
- **None:** Standard one-tap alarm dismissal.

### 💤 Smart Snooze Rules
- **Configurable Snooze Duration:** Choose between 5, 10, or 15 minutes per snooze.
- **Max Snooze Limit:** Hard-capped at 3 snoozes per alarm instance to prevent endless sleeping in. Snooze counters reset automatically upon completing the wake-up mission.

### 🔊 Crescendo Volume
- **Gradual Sound Ramp:** Alarm sound starts soft (5% volume) and linearly ramps up to 100% volume over 20 seconds for a pleasant wake-up experience.

### 🛡️ Anti-Bypass Security Protections
- **Hardware Volume Key Overrides:** Intercepts volume keys while the alarm screen is active to prevent turning off or muting sound.
- **Back Gesture Blocking:** Back gestures and back buttons are locked during ringing until the challenge is solved or snoozed.

### 🎨 Modern Material 3 & Jetpack Compose
- **Adaptive Light & Dark Themes:** Fully styled with Material 3 design tokens.
- **Reboot Persistence:** Automatically reschedules all active alarms upon device restart using `RECEIVE_BOOT_COMPLETED`.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** MVVM (Model-View-ViewModel) with `StateFlow` and Coroutines
- **Database:** Room Persistence Library with KSP
- **Camera & Vision:** CameraX + Google ML Kit Barcode Scanning
- **Scheduling & Background:** `AlarmManager` (`setAlarmClock`) + Android Foreground Service (`mediaPlayback`)
- **Min SDK:** 26 (Android 8.0 Oreo) | **Target SDK:** 34 (Android 14)

---

## 📁 Project Structure

```
SnoozeControl/
├── app/
│   ├── src/main/java/com/snoozecontrol/
│   │   ├── data/            # Room Database & DAO
│   │   ├── model/           # Data Models (AlarmItem, ChallengeType)
│   │   ├── receiver/        # AlarmReceiver & BootReceiver
│   │   ├── scheduler/       # AndroidAlarmScheduler
│   │   ├── service/         # AlarmService (Foreground sound & crescendo)
│   │   ├── ui/              # Compose Screens & Components
│   │   │   ├── AddEditAlarmScreen.kt
│   │   │   ├── AlarmDismissScreen.kt
│   │   │   ├── AlarmScreen.kt
│   │   │   ├── BarcodeRegistrationScreen.kt
│   │   │   └── theme/
│   │   ├── util/            # BarcodeScanner & Helpers
│   │   └── viewmodel/       # ViewModels
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## 🚀 Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/SnoozeControl.git
   ```
2. **Open in Android Studio:**
   - Open Android Studio (Ladybug 2024.2.1+ recommended).
   - Sync Gradle project files.
3. **Run on Device or Emulator:**
   - Ensure Camera permission is granted if testing Barcode mode.

---

## 📄 License

```text
Copyright (c) 2025 Snooze Control. All rights reserved.
```
