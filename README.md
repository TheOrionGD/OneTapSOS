# OneTapSOS 🚨
### Personal Safety, Emergency SOS Alert & Intelligent Crisis Response System

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Package](https://img.shields.io/badge/Package-com.onetapsos.app-007ACC?logo=android&logoColor=white)](app/build.gradle.kts)
[![Version](https://img.shields.io/badge/Version-1.2.1-orange.svg)](https://github.com/TheOrionGD/OneTapSOS/releases/tag/v1.2.1)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gemini AI](https://img.shields.io/badge/AI-Google_Gemini-4285F4?logo=google&logoColor=white)](https://ai.google.dev)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**OneTapSOS** (`com.onetapsos.app`) is an advanced, privacy-first personal safety and emergency response Android application built to protect individuals, families, and communities during critical situations. Featuring instantaneous one-touch SOS dispatch, automated fall detection, per-contact custom alert messaging, offline-ready OpenStreetMap navigation, and an intelligent Google Gemini AI safety companion, OneTapSOS empowers users to get immediate life-saving assistance when every second counts.

---

## 🌟 Key Features

### 🚨 Emergency SOS & Alert Dispatch
- **Instant Activation**: One-tap emergency dispatch via an intuitive interface or quick-action triggers.
- **Cancel Countdown Window**: Configurable countdown buffer with haptic feedback to prevent accidental false alarms.
- **Live GPS Broadcast**: Automatically attaches high-accuracy Google Maps location links to outgoing emergency messages.
- **Per-Contact Custom Messages**: Tailor specific emergency instructions for trusted contacts (e.g., Mom, Dad, Physician, Guardian).

### 📡 Live Location & Map Explorer
- **Interactive Safe Map**: Discover nearby emergency services (hospitals, police stations, fire stations, pharmacies) powered by OpenStreetMap (`osmdroid`).
- **Live Sharing & Commute Monitoring**: Broadcast real-time location trails to family members during late-night journeys.
- **Home Screen Widget & Quick Tile**: Trigger SOS alerts straight from the Android Quick Settings drawer or home screen without unlocking the app.

### 🤖 Gemini AI Crisis Companion
- **Context-Aware Assistance**: Direct, structured crisis survival guidance, first-aid protocols, and risk advice powered by the Google Gemini AI SDK.

### 🛡️ Sensor & Battery Safety Engine
- **Intelligent Fall Detection**: Accelerometer-driven background service detects sudden impacts or accidents and initiates an automated countdown.
- **Critical Battery Warning**: Dispatches a low-battery emergency SMS with last-known GPS coordinates when battery drops to 5%.
- **Safety Timer & Check-Ins**: Scheduled safety check-ins and timers for solo travel or risky activities.

### 🌐 Multi-Language Support
- Full localization support for **English**, **Hindi (हिंदी)**, and **Tamil (தமிழ்)**.

### 🔒 Privacy-First Architecture
- **Local SQLite Database**: Encrypted local database stores contacts, location timelines, and incident history strictly on-device.
- **Data Sovereignty**: Complete data control with one-tap data clearing utilities.

---

## 📱 Tech Stack & Libraries

- **Language & Framework**: Kotlin 2.0+, Android SDK 36 (minSdk 24)
- **Architecture**: MVVM, Repository Pattern, Android Jetpack, Foreground Services
- **UI System**: Material 3, Dark Mode OLED Optimization, Haptic Engine
- **Location & Maps**: Google Play Services Location (`play-services-location`), OpenStreetMap (`osmdroid`)
- **AI Intelligence**: Google Generative AI Client SDK (`com.google.ai.client.generativeai`)
- **Machine Learning & Vision**: TensorFlow Lite (`tensorflow-lite-support`), CameraX (`camera-camera2`, `camera-lifecycle`)
- **Database**: SQLite Local Database with Custom OpenHelper

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
   git clone https://github.com/TheOrionGD/OneTapSOS.git
   cd OneTapSOS
   ```

2. **Configure API Keys:**
   Create a `local.properties` file in the root directory:
   ```properties
   sdk.dir=C:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
   ```

3. **Build & Package:**
   ```bash
   # Assemble Debug APK
   ./gradlew assembleDebug

   # Assemble Signed Release APK & AAB Bundle (v1.2.1)
   ./gradlew assembleRelease bundleRelease
   ```

---

## 📄 Documentation & Releases
- 📦 [Latest Release (v1.2.1)](https://github.com/TheOrionGD/OneTapSOS/releases/tag/v1.2.1)
- 🔒 [Privacy Policy Document](OneTapSOSDOCS/privacy_policy.html)
- 📋 [Google Play Store Publishing Specifications](OneTapSOSDOCS/GOOGLE_PLAY_STORE_PUBLISHING_DETAILS.txt)

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
