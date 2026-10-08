# OneTapSOS — Comprehensive UX Information Architecture & Navigation Graph

## 1. Executive Product Vision
**OneTapSOS** is an emergency SOS dispatch and personal safety platform designed to provide instantaneous, fail-safe crisis response and continuous prevention monitoring.

The application architecture is structured into **5 Primary Pillars**:
```text
                             SOSENSE GLOBAL SHELL
                                      │
        ┌──────────────┬──────────────┼──────────────┬──────────────┐
        ▼              ▼              ▼              ▼              ▼
     [ 🏠 HOME ]   [ 🛡️ SAFETY ]   [ 🗺️ MAP ]   [ 👥 CONTACTS ]  [ ⚙️ MORE ]
        │              │              │              │              │
    Emergency      Prevention &   Location &     Emergency      Medical ID &
    Command Hub     Monitoring    Safe Havens      Circle       System Guides
```

---

## 2. Global Navigation System & Tab Hierarchy

### 🏠 Tab 1: HOME (Emergency SOS Command Center)
- **Persistent Header**: Greeting, Live Status Badge (`🟢 ARMED & PROTECTED` / `🚨 SOS ACTIVE`), AI Assistant Shortcut, Notifications.
- **Emergency Active Overlay**: Instant situational banner when SOS is transmitting with real-time GPS and 1-tap "I'm Safe" and "Live Tracking".
- **Primary Hero Element**: Dominant SOS Button with ripple pulse animation, 3-second hold buffer, and hardware volume key sync.
- **Readiness HUD Grid**:
  - Trusted Contacts Card (`TrustedContactsActivity`)
  - Live GPS Fix Card (`MapActivity`)
  - Battery Safety Card (`BatterySafetyActivity`)
  - Fall Detection Status Card (`FallDetectionService`)
- **Quick Action Grid**:
  - 📢 Emergency Broadcast (`EmergencyBroadcastActivity`)
  - 🤖 AI Safety Companion (`ChatActivity`)
  - 📞 Fake Call Simulation (`FakeCallActivity`)
  - ⏱️ Quick Check-In (`CheckInActivity`)
- **Recent Incident Log Card** (`SOSHistoryActivity`)

### 🛡️ Tab 2: SAFETY (Active Prevention & Monitoring Suite)
- **Active Safety Tools**:
  - 🚶 Journey Mode & Route Escort (`JourneyModeActivity`, `LocationShareActiveActivity`)
  - ⏱️ Safety Timer & Countdown (`SafetyTimerActivity`, `SafetyTimerActiveActivity`)
  - ⏱️ Scheduled Check-In (`CheckInActivity`, `SafetyCheckInHistoryActivity`)
- **Incident & Risk Assessment**:
  - ⚠️ Report Unsafe Situation (`UnsafeSituationActivity`)
  - 📝 Incident Documentation Log (`IncidentReportActivity`, `IncidentHistoryActivity`)
- **Device & System Readiness**:
  - 🎒 Emergency Kit Checklist (`EmergencyPreparationActivity`)
  - 🔋 5% Battery Alert Settings (`BatteryAlertSettingsActivity`)
  - 🩺 System Diagnostics (`SOSDiagnosticsActivity`)

### 🗺️ Tab 3: MAP (Location Intelligence & Safe Navigation)
- **Interactive OpenStreetMap View**: Real-time GPS pinpoint, heading, safe zone overlays.
- **Safe Havens & POI Discovery**:
  - 🚨 Nearby Police, Hospitals & Fire Stations (`NearbyHelpActivity`)
  - 🛣️ Safe Navigation Routes (`SafeRouteActivity`)
  - 📡 Real-time Location Sharing (`ShareLocationActivity`, `LiveTrackingActivity`)
  - 🛡️ Safe Zones & Geofencing (`SafeMapActivity`)
  - 📜 Location Timeline & Breadcrumbs (`LocationHistoryActivity`, `LocationDashboardActivity`)
  - ⚙️ Map Display Settings (`MapSettingsActivity`, `LocationSharingSettingsActivity`)

### 👥 Tab 4: CONTACTS (Emergency Circle & Messaging)
- **Emergency Circle Hub**:
  - Primary Emergency Contact spotlight with 1-tap test call and SMS.
  - ➕ Add Trusted Contact (`AddTrustedContactActivity`)
  - ✏️ Edit Contact & Custom Messages (`EditTrustedContactActivity`, `ContactDetailActivity`)
  - 👥 Emergency Groups (Family, Friends, Work) (`GroupsActivity`)
  - 📝 Emergency Message Templates (`MessageTemplateActivity`, `ImSafeMessageActivity`, `EmergencyMessagesActivity`)
  - 💬 Message Delivery Threads (`ConversationActivity`, `MessageHistoryActivity`)
  - 🔑 Permissions Manager (`ContactPermissionActivity`, `ContactSelectionActivity`)

### ⚙️ Tab 5: MORE (Medical ID, Guides & Preferences)
- **Medical & Identity**:
  - 👤 User Profile (`ProfileActivity`)
  - 🪪 Medical Emergency Card (`EmergencyCardActivity`)
- **Emergency Hotlines & Life-Saving Guides**:
  - 📞 Emergency Hotlines (112, 100, 108) (`EmergencyNumbersActivity`)
  - 🩹 Offline First Aid Protocols (`FirstAidActivity`)
  - 🌪️ Disaster Readiness Protocols (`DisasterGuideActivity`)
  - 💡 Safety Tips & Best Practices (`SafetyTipsActivity`)
- **System Configuration**:
  - 🚨 SOS Settings & Hardware Trigger (`SOSSettingsActivity`)
  - 🔔 Notification Preferences & Testing Lab (`NotificationSettingsActivity`)
  - 🌐 Multi-language Support (EN, TA, HI) (`LanguageSettingsActivity`)
  - 🔒 App Security & Privacy Controls (`AppLockActivity`, `PrivacySettingsActivity`, `DataManagementActivity`)
  - 🎨 Appearance & Themes (`AppearanceSettingsActivity`)
  - 🛡️ Permissions Overview (`PermissionsActivity`)
  - 📱 Panic Widget Setup (`PanicWidgetActivity`)
  - ❓ Help & Feature Directory (`HelpAndSupportActivity`)

---

## 3. Connected Emergency Flow Diagram
```text
[ USER TRIGGERS SOS ]
       │
       ├── Hardware Volume Hold (3s)
       ├── Screen SOS Button (3s Hold)
       ├── Quick Settings Tile
       └── Fall Detection Freefall Impact
       │
       ▼
[ SOS COUNTDOWN (5s/15s) ] ── (Cancel) ──► [ CANCELLED & RESTORED ]
       │ (Timeout)
       ▼
[ SOS ACTIVATION & GPS FIX ]
       │
       ▼
[ SMS & LOCATION DISPATCH ] ──► [ NOTIFY N CONTACTS WITH GPS LINK ]
       │
       ▼
[ EMERGENCY ACTIVE STATE ] ◄────────────────────────┐
       │                                            │
       ├──► [ LIVE TRACKING & CRADLE MAP ]          │
       ├──► [ EMERGENCY MESSAGES HUB ]              │
       └──► [ CALL POLICE / AMBULANCE ]             │
       │                                            │
       ▼ (User taps "I'm Safe")                     │
[ SEND "I'M SAFE" SMS WITH LOCATION ]               │
       │                                            │
       ▼                                            │
[ SOS RESOLVED SUMMARY ] ──► (View Log) ──► [ SOS DETAILS ]
```

---

## 4. Design Tokens & Visual Hierarchy
- **Palette**: Deep Navy OLED (`#1A1A2E`), Midnight Surface (`#16213E`), Card Elevation (`#252A3D`), Electric Cyan (`#00D9FF`), Emergency Red (`#E63946`), Safety Green (`#06D6A0`), Warning Yellow (`#FFD60A`).
- **Typography**: Clear hierarchy with bold state announcements, high contrast, readable numbers.
- **Haptics**: Tactile pulse confirmation for emergency activation and cancel countdowns.
