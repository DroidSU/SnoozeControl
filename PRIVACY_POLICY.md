# Privacy Policy for Snooze Control

**Effective Date:** May 20, 2025  
**Last Updated:** May 20, 2025

**Snooze Control** ("we," "our," or "us") is committed to protecting your privacy. This Privacy
Policy explains how information about you is collected, used, and disclosed when you use our mobile
application **Snooze Control** (the "App").

We operate on a **privacy-first** principle: your personal data stays on your device whenever
possible, and we do not sell, track, or share your personal information.

---

## 1. Information We Collect and How We Use It

### A. Local Device Storage (No Cloud Upload)

* **Alarm Data & Settings:** Your scheduled alarms, repeat preferences, ringtone selections, snooze
  limits, math challenge difficulty levels, and registered barcodes/QR codes are saved **locally on
  your device** using an encrypted SQLite database managed by Android's Room Persistence Library.
* **App Preferences:** App configuration options (such as bedtime reminder settings and weather
  location preferences) are stored strictly on your local device.

### B. Camera & Barcode Data

* **Camera Access:** If you enable the **Barcode / QR Code Wake-Up Challenge**, the App requests
  permission to access your device camera.
* **On-Device Vision Processing:** Camera frames are analyzed locally in real time using **Google ML
  Kit Barcode Scanning** to match against your pre-registered target barcode/QR code.
* **No Image Storage or Transmission:** Camera feeds and barcode data are processed in temporary
  device memory. We do not capture, record, store, or transmit camera images or video recordings to
  external servers.

### C. Motion & Sensor Data

* **Accelerometer Access:** If you select the **Shake Wake-Up Challenge**, the App reads real-time
  motion events from your device's physical accelerometer sensor to register shake progress.
* **No Tracking:** Sensor data is discarded immediately after the challenge is completed and is
  never stored or transmitted.

### D. Location Data (Optional)

* **Weather Forecasts:** The App requests coarse/fine location access (`ACCESS_COARSE_LOCATION` /
  `ACCESS_FINE_LOCATION`) solely to fetch localized, real-time weather information (temperature,
  weather code, wind speed) displayed on the **Morning Dashboard** post-alarm briefing.
* **Third-Party Weather API:** Location coordinates are sent anonymously to the **Open-Meteo API**
  to retrieve weather forecasts. Location coordinates are not attached to any personal identifiers
  and are neither stored on remote servers nor used for tracking.

### E. Text-To-Speech (TTS)

* The App utilizes your device's built-in Android **TextToSpeech** engine to read out loud your
  morning briefing (time, weather, motivational quote). All text generation and audio synthesis
  occur locally on your device.

---

## 2. Permissions Required & Purpose

To function reliably as an alarm clock app, Snooze Control requires the following permissions:

| Permission                                                 | Purpose                                                                                   |
|:-----------------------------------------------------------|:------------------------------------------------------------------------------------------|
| `SCHEDULE_EXACT_ALARM`                                     | Ensures alarms ring at the precise scheduled time.                                        |
| `USE_FULL_SCREEN_INTENT`                                   | Displays the full-screen alarm dismissal UI even when the device screen is locked.        |
| `CAMERA`                                                   | Scans barcodes/QR codes during challenge setup and alarm dismissal.                       |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`          | Fetches local weather conditions for the Morning Dashboard.                               |
| `POST_NOTIFICATIONS`                                       | Displays bedtime reminders, upcoming alarm alerts, and active alarm status notifications. |
| `RECEIVE_BOOT_COMPLETED`                                   | Automatically reschedules all active alarms after device restart.                         |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`                     | Prevents OEM battery savers from killing alarm timers in the background.                  |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Ensures uninterrupted audio playback while the alarm is ringing.                          |
| `WAKE_LOCK`                                                | Turns on and keeps the screen awake while an alarm is actively ringing.                   |
| `VIBRATE`                                                  | Provides haptic vibration feedback for alarms and challenge interactions.                 |

---

## 3. Third-Party Services & Libraries

The App integrates trusted open-source and Google libraries. These services operate under their
respective privacy policies:

* **Google ML Kit (Barcode Scanning):** Processes vision data locally on the
  device. [Google Privacy Policy](https://policies.google.com/privacy)
* **Open-Meteo API:** Provides non-commercial weather data without requiring API keys or personal
  identifiers. [Open-Meteo Terms & Privacy](https://open-meteo.com/en/terms)

We do not use third-party analytics (such as Google Analytics or Firebase Analytics), ad networks,
tracking pixels, or crash reporting services that gather personal identifiable information (PII).

---

## 4. Data Security

Because all core app data is stored locally within Android's sandboxed application data directory,
your information is protected by standard Android OS security mechanisms. We do not maintain
external servers, databases, or user accounts.

---

## 5. Children's Privacy

Snooze Control does not knowingly collect or solicit any personal information from children under
the age of 13. The App does not require registration or personal identifier submission.

---

## 6. Your Rights & Data Control

Since all data resides on your device, you maintain full control:

* **Deletion:** You can clear all app data at any time via Android System Settings
  (`Settings > Apps > Snooze Control > Storage > Clear Data`).
* **Permissions Control:** You can revoke optional permissions (such as Camera or Location) at any
  time through Android System Settings.
* **App Uninstall:** Uninstalling the App permanently removes all locally stored database entries
  and preferences.

---

## 7. Changes to This Privacy Policy

We may update our Privacy Policy periodically to reflect app updates or legal requirements. Any
changes will be posted on this page with an updated "Last Updated" date.

---

## 8. Contact Us

If you have any questions, feedback, or concerns regarding this Privacy Policy or the App's data
practices, please contact us:

* **Repository:** [Snooze Control on GitHub](https://github.com/your-username/SnoozeControl)
* **Email Support:** support@snoozecontrol.app
