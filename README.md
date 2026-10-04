# Snooze Control ⏰

[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-blue)](https://developer.android.com/about/versions/14)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-orange)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

**Snooze Control** is a feature-rich, ultra-reliable Android wake-up alarm clock app engineered
specifically for heavy sleepers. Built from the ground up using **Kotlin**, **Jetpack Compose**,
**Material 3**, **Room Persistence Library**, **Hilt**, **CameraX**, and **Google ML Kit**, Snooze
Control forces physical and mental activity before turning off an alarm—ensuring you wake up on
time, every time.

---

## 🌟 Key Features

### 🧩 Wake-Up Missions & Challenges

Turn off your alarm only by proving you are wide awake:

* 🧮 **Math Challenge:** Solve randomly generated math problems with customizable difficulty levels
  (**Easy**, **Medium**, **Hard**) before the alarm can be dismissed.
* 📷 **Barcode / QR Code Challenge:** Scan a pre-registered barcode or QR code (e.g., toothpaste tube
  in the bathroom, coffee bag in the kitchen) using on-device **CameraX** and **Google ML Kit**
  vision processing, forcing you out of bed.
* 📳 **Shake Challenge:** Physically shake your smartphone to fill up a fluid dynamic liquid wave
  meter (`LiquidProgressWave`), powered by real-time hardware accelerometer detection.
* 🔔 **Standard Dismissal:** Classic one-tap alarm dismissal when no challenge is needed.

---

### 💤 Smart Snooze Management

* **Flexible Snooze Durations:** Configure snooze times to 5, 10, or 15 minutes per alarm.
* **Hard-Capped Snooze Limit:** Set a maximum snooze allowance (e.g., 3 snoozes max) to stop endless
  sleeping in. Snooze counters reset automatically once the wake-up mission is completed.

---

### 🔊 Crescendo Volume Ramp

* **Gentle Sound Ramping:** Prevent abrupt panic waking with a linear volume crescendo that ramps
  smoothly from 5% to 100% volume over 20 seconds.
* **Custom Ringtone Picker:** Select system ringtones, custom audio files, or alarm tones per alarm
  instance.

---

### ☀️ Morning Dashboard & Audio Briefing

* **Post-Dismissal Briefing:** Once the alarm mission is conquered, transition to a slick morning
  dashboard screen.
* **Real-Time Weather Forecast:** Fetches localized temperature, weather conditions, and wind speed
  via the **Open-Meteo API** and **Ktor HTTP Client**.
* **Daily Motivational Quotes:** Displays inspiring quotes to jumpstart your morning.
* **Text-To-Speech (TTS) Voice Briefing:** Hands-free vocal reading of the time, current weather,
  and daily quote.

---

### 🌙 Bedtime Reminders & Upcoming Alarm Alerts

* **Bedtime Notification:** Configurable bedtime alerts reminding you when it's time to sleep based
  on your scheduled morning alarms.
* **Upcoming Alarm Notification:** Displays a notification prior to alarm trigger with a convenient
  **Dismiss Early** option for early risers.

---

### 🛡️ Anti-Bypass & Reliability Safeguards

* **Hardware Volume Key Override:** Intercepts hardware volume controls while ringing to prevent
  accidental muting or turning off the alarm.
* **Back Gesture & Button Lockout:** Blocks back navigation gestures and touch bypasses until the
  challenge is completed or snoozed.
* **Reboot Auto-Reschedule:** Listens to `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, and
  `MY_PACKAGE_REPLACED` to automatically reschedule all active alarms after device reboots.
* **Direct Boot Aware Services:** Built with direct boot compatibility so alarms trigger even before
  initial user unlock after reboot.
* **OEM Battery Optimization Assistant:** Built-in guidance for custom Android OEMs (Xiaomi
  MIUI/HyperOS, Samsung OneUI, Huawei EMUI, Oppo ColorOS, Vivo FuntouchOS, OnePlus OxygenOS) to
  ensure background execution permissions and prevent process killing.

---

## 🛠️ Architecture & Tech Stack

Snooze Control adheres to modern Android app architecture standards using **MVVM (
Model-View-ViewModel)** with Unidirectional Data Flow (UDF), Kotlin Coroutines, and `StateFlow`.

```
┌────────────────────────────────────────────────────────┐
│                      Compose UI                        │
│ (AlarmScreen, AddEditAlarmScreen, AlarmDismissScreen)  │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                      ViewModels                        │
│   (AlarmViewModel, AddEditViewModel, DismissViewModel) │
└───────────────────────────┬────────────────────────────┘
                            │
             ┌──────────────┴──────────────┐
             ▼                             ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│     Room Database        │  │     Ktor Weather Client  │
│    (Local SQLite)        │  │    (Open-Meteo API)      │
└──────────────────────────┘  └──────────────────────────┘
```

* **Language:** [Kotlin 2.0+](https://kotlinlang.org/)
* **UI
  Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3 Design Tokens](https://m3.material.io/)
* **Dependency Injection:** [Hilt (Dagger)](https://dagger.dev/hilt/)
* **Local
  Database:** [Room Persistence Library](https://developer.android.com/training/data-storage/room)
  with [KSP](https://kotlinlang.org/docs/ksp-overview.html)
* **Asynchronous Flow:** Kotlin Coroutines & `StateFlow`
* **Networking:** [Ktor Client](https://ktor.io/) (`ktor-client-cio`,
  `ktor-client-content-negotiation`)
* **Camera & Vision
  Processing:** [CameraX](https://developer.android.com/training/camerax) + [Google ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning)
* **Scheduling:** Android `AlarmManager` (`setAlarmClock`) + `ForegroundService` (`mediaPlayback`)
* **Voice Synthesis:** Android `TextToSpeech` (TTS)
* **Minimum SDK:** 26 (Android 8.0 Oreo) | **Target SDK:** 34 (Android 14)

---

## 📁 Project Structure

```
SnoozeControl/
├── app/
│   ├── src/
│   │   ├── androidTest/              # Instrumented UI Tests
│   │   └── main/
│   │       ├── AndroidManifest.xml   # Permissions, Services, Receivers & Activities
│   │       └── java/com/snoozecontrol/
│   │           ├── MainActivity.kt                  # App Root Activity & Navigation
│   │           ├── AddAlarmActivity.kt              # Add / Edit Alarm Activity
│   │           ├── AlarmDismissActivity.kt          # Full-screen ringing & challenge UI
│   │           ├── SnoozeControlApplication.kt      # Hilt Application Entry point
│   │           ├── api/                             # Ktor Open-Meteo API Client & DTOs
│   │           ├── data/                            # Room Entity, DAO & Database
│   │           ├── di/                              # Hilt Dependency Injection Modules
│   │           ├── model/                           # Domain Models (AlarmItem, ChallengeType)
│   │           ├── receiver/                        # Alarm, Bedtime & Boot Receivers
│   │           ├── scheduler/                       # AlarmManager Scheduler Interfaces & Impl
│   │           ├── service/                         # Foreground Service for Sound & Volume Ramp
│   │           ├── ui/                              # Jetpack Compose Screens & Canvas UI
│   │           │   ├── AddEditAlarmScreen.kt
│   │   │       ├── AlarmDismissScreen.kt
│   │   │       ├── AlarmScreen.kt
│   │   │       ├── BarcodeRegistrationScreen.kt
│   │   │       ├── MorningDashboardScreen.kt
│   │   │       ├── LiquidProgressWave.kt
│   │   │       └── theme/                           # Color, Typography & M3 Theme
│   │   │       ├── util/                            # BarcodeScanner, ShakeDetector, WeatherRepo
│   │   │       └── viewmodel/                       # ViewModels
│   └── build.gradle.kts
├── PRIVACY_POLICY.md                        # Application Privacy Policy
├── README.md                                # Project Documentation
└── build.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites

* **Android Studio:** Ladybug (2024.2.1) or newer
* **JDK:** Version 17 or higher
* **Android Device / Emulator:** API Level 26 (Android 8.0) or higher (Physical device recommended
  for testing CameraX barcode scanning and Shake sensors)

### Build & Run Instructions
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/SnoozeControl.git
   cd SnoozeControl
   ```
2. **Open in Android Studio:**
    - Launch Android Studio and select **Open**.
    - Navigate to the `SnoozeControl` directory and sync Gradle.
3. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
4. **Deploy to Device:**
    - Connect your Android device via USB with ADB Debugging enabled.
    - Run `app` configuration from Android Studio or execute:
      ```bash
      ./gradlew installDebug
      ```

---

## 🔒 Privacy & Data Security

Snooze Control is designed with privacy at its core:

* **100% Local Storage:** Alarms, barcodes, and settings are stored exclusively on your device in
  local Room database tables.
* **On-Device Computer Vision:** Barcode scanning processes video frames locally in memory using
  Google ML Kit. No photos or video feeds are ever stored or uploaded.
* **Anonymous Weather Location:** Location permission is used strictly to fetch localized weather
  data from Open-Meteo API during the morning briefing. No location tracking or user identifiers are
  transmitted or retained.

For full details, please review our complete [Privacy Policy](PRIVACY_POLICY.md).

---

## 📄 License

```text
Copyright (c) 2025 Snooze Control. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations workouts specified under the License.
```
