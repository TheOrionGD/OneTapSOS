### 🚀 SOSence v1.0.4 Release Notes

#### 🚨 SOS Emergency Log & Single Source of Truth
- Unified SOS activation lifecycle across manual buttons, hardware power clicks, and fall detection into SQLite Room/DatabaseHelper (`TABLE_SOS_EVENTS`).
- Home Dashboard HUD and `SOSHistoryActivity` now query the exact same persisted event records.
- Added event status pills (`ACTIVE` with pulsing indicator vs `RESOLVED` with duration timestamp).

#### 🏃 Real Multi-Stage Fall Detection Engine
- Implemented 5-stage state machine (`NORMAL` → `FREE_FALL_CANDIDATE` → `IMPACT_DETECTED` → `POST_IMPACT_ANALYSIS` → `FALL_CONFIRMED_COUNTDOWN` → `TRIGGERING_SOS`).
- Multi-sensor fallback support (`TYPE_ACCELEROMETER`, `TYPE_GYROSCOPE`, `TYPE_GRAVITY`, `TYPE_LINEAR_ACCELERATION`).
- Continuous background foreground service with Android 14/15/16 compliance.
- 15-second high-priority countdown notification with interactive "I'M OK" and "SEND SOS" action triggers.
- Developer simulation test trigger in `BackgroundDiagnosticsActivity` & sensor telemetry in `SOSDiagnosticsActivity`.

#### 🗺️ Safe Map Overpass Redundancy & Offline Cache
- Integrated `LocationHelper` for Android 12-16 dual permission handling (`ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`) and Google Play Services GPS resolution prompt.
- Redundant multi-endpoint OpenStreetMap Overpass queries with timeouts and fallbacks.
- Dynamic distance calculation to user's real GPS coordinates with category filtering (Police, Hospital, Pharmacy, Fire Station).
- Offline havens cache in SQLite (`TABLE_CACHED_SAFE_ZONES`) with in-app Retry mechanism.

#### 📱 UI & Screen Deduplication
- Scoped Scheduled Check-In exclusively to Safety Tab; replaced duplicate action on Home with Live Tracking.
- Routed POI filter chips in MapFragment directly to `SafeMapActivity` with filter extras.
- Clean icon-only Bottom Navigation Bar (`labelVisibilityMode="unlabeled"`).
- Upgraded target version to `versionCode = 4`, `versionName = "1.0.4"` with `compileSdk = 36` / `targetSdk = 36`.
