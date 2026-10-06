# SOSense 🚨
### Personal Safety, Emergency SOS Alert & Intelligent Crisis Response

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gemini AI](https://img.shields.io/badge/AI-Google_Gemini-4285F4?logo=google&logoColor=white)](https://ai.google.dev)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**SOSense** is an advanced, privacy-first personal safety and emergency response Android application built to protect individuals, families, and communities during critical crises. With instantaneous one-touch SOS dispatch, automated accelerometer-driven fall detection, per-contact custom alert messaging, offline-ready Safe Map discovery, and one of the best Google product Gemini AI safety companion, SOSense empowers users to get immediate life-saving help when every second counts.

---

## 🌟 Key Features

### 🚨 Emergency SOS & Alert Dispatch
- **One-Touch & Hardware Trigger**: Instant emergency activation via on-screen button or by holding physical volume keys for 3 seconds.
- **Cancel Countdown Window**: Configurable countdown buffer with haptic feedback to prevent accidental false alarms.
- **Live GPS Broadcast**: Automatically captures high-accuracy coordinates and attaches a real-time Google Maps link to outgoing emergency alerts.
- **Per-Contact Custom SOS Messages**: Tailor specific emergency instructions for each contact (e.g., specific messages for Mom, Dad, Physician, Guardian).

### 🛡️ Sensor & Background Monitoring
- **Intelligent Fall Detection**: Accelerometer-driven service detects sudden high-impact falls or accidents and initiates an automated emergency countdown if unresponsive.
- **Critical Battery Alert**: Dispatches a low-battery emergency SMS alert with last known coordinates when the device reaches 5% battery.
- **Quick Settings Tile**: Trigger emergency alerts straight from the Android Notification & Quick Settings drawer without unlocking the full app.

### 🗺️ Safe Map & Offline Emergency Hub
- **Interactive Safe Map**: Discover nearby hospitals, police stations, fire stations, and 24/7 pharmacies powered by OpenStreetMap and live routing.
- **Live Tracking & Breadcrumbs**: Share real-time location trails with family members during late-night commutes or unsafe situations.
- **Fake Call Generator**: Simulate incoming phone calls to discreetly escape uncomfortable or dangerous environments.
- **Offline First Aid & Disaster Guides**: Instant offline protocols for CPR, severe bleeding, burns, earthquakes, and flood survival.

### 🤖 Gemini AI Safety Assistant
- Context-aware emergency assessment, crisis protocol navigation, and personalized safety tips powered by Google Gemini AI.

### 🔒 Privacy-First Architecture
- **Local SQLite Storage**: Encrypted local database stores contacts, location logs, and incident history on-device without unauthorized cloud tracking.
- **Data Deletion Control**: One-tap "Clear All Data" utility empowers users with complete data sovereignty.

---

## 📱 Tech Stack & Libraries

- **Language**: Kotlin
- **Architecture**: MVVM, Repository Pattern, Android Jetpack, Foreground Services
- **UI & Transitions**: Material 3, Dark Mode OLED optimization, Custom Micro-interactions & Haptic Engine
- **Location & Maps**: Google Play Services Location (`play-services-location`), OpenStreetMap (`osmdroid`)
- **AI Intelligence**: Google Generative AI Client SDK (`com.google.ai.client.generativeai`)
- **Computer Vision & ML**: TensorFlow Lite (`tensorflow-lite-support`), CameraX (`camera-camera2`, `camera-lifecycle`)
- **Database**: Local SQLite with custom OpenHelper and safe migrations

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 34 / 36
- JDK 17
- Physical device or Emulator with Google Play Services

### Setup & Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/TheOrionGD/SOSence.git
   cd SOSence
   ```

2. **Configure API Keys:**
   Create a `local.properties` file in the root directory:
   ```properties
   sdk.dir=C:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
   ```

3. **Build and Run:**
   ```bash
   # Assemble Debug APK
   ./gradlew assembleDebug

   # Assemble Signed Release APK & AAB Bundle
   ./gradlew assembleRelease bundleRelease
   ```

---

## 📄 Privacy Policy & Play Store Details
- [Privacy Policy Document](SOSenseDOCS/privacy_policy.html)
- [Google Play Store Publishing Specifications](SOSenseDOCS/GOOGLE_PLAY_STORE_PUBLISHING_DETAILS.txt)

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
