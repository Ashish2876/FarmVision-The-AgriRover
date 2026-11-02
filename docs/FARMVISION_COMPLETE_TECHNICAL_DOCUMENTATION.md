# FarmVision — The AgriRover
# Complete Technical Documentation & System Architecture Manual

**Project Name:** FarmVision – The AgriRover  
**Android Application ID:** `com.farmvision.agrirover`  
**Software Release:** `1.0.0` (Version Code: `1`)  
**Lead Developer & System Architect:** Ashish Sampat Pawar  
**Document Classification:** Master Production Reference  
**Hardware Invariant:** `ACTUATORS_ENABLED = 0` (Confirmed Software Interlock)  
**Date of Release:** November 2026  

---

## 1. System Overview & Technology Stack

FarmVision – The AgriRover is an advanced, production-grade Android smart agriculture system and agricultural robotics control station designed to monitor crop health, interface with robotic field scouts, provide historical microclimate analytics, trigger real-time hazard alerts, and deliver agronomic artificial intelligence guidance via Google Gemini.

### Technology Stack Summary
- **Mobile Client:** Native Android (Kotlin 2.0.21, Jetpack Compose, Material 3 Design System).
- **Architecture:** Clean Architecture + Model-View-ViewModel (MVVM) with Unidirectional Data Flow (UDF) powered by Kotlin Coroutines and StateFlow.
- **AI Gateway Backend:** Python 3.11, FastAPI, Pydantic v2, Starlette, Uvicorn ASGI server.
- **Generative AI:** Google Gemini 3.5 Flash Lite (`gemini-3.5-flash-lite`) mediated strictly through the FastAPI cloud gateway.
- **Target Platform:** Android 15 (API 35), Minimum SDK 26 (Android 8.0 Oreo), Java 17 toolchain.
- **Build System:** Gradle 8.7.2, Android Gradle Plugin 8.7.2, R8 optimization and resource shrinking.
- **Firmware Subsystem:** Espressif ESP32 DevKit V1 (Main Controller) and ESP32-CAM (Video Streamer).

---

## 2. Android Application Architecture

The Android application adheres to strict Clean Architecture principles, ensuring a complete separation of concerns between domain logic, data persistence, and presentation.

```
com.farmvision.agrirover/
├── core/
│   └── designsystem/         # Color palettes, typography, spacing tokens, status badges
├── data/
│   ├── alert/repository/     # Persistent JSON & Preferences alert repositories
│   ├── analytics/            # Deterministic demo data generators
│   ├── assistant/            # JSON serializers, Backend & Mock Assistant providers
│   ├── datasource/           # StandardRoverHttpClient, RoverNetworkBinder
│   ├── device/               # Device communication repos, keystore crypto, transport bridges
│   ├── local/                # SettingsPreferencesManager (SharedPreferences)
│   ├── model/                # RoverTelemetry, PlantItem, SensorReading, FarmSettings
│   └── repository/           # FarmRepository, MockFarmRepository, TelemetryHistory
├── domain/
│   ├── alert/                # AlertEngine, AlertLifecycleManager, AlertRule definitions
│   ├── analytics/            # AnalyticsMathEngine (statistical aggregations, bucketing)
│   ├── assistant/            # ContextBuilder, AssistantContext, AssistantError hierarchy
│   └── device/               # Device connection state machines, command pipelines
└── ui/
    ├── alert/                # AlertCenterSheet, AlertDetailsScreen, AlertSettingsScreen
    ├── analytics/            # AnalyticsScreen, Chart components, KPI cards, Table view
    ├── app/                  # FarmVisionApp (Root Shell, Bottom Navigation Bar)
    ├── assistant/            # AssistantChatScreen, Message bubbles, Context pills
    ├── home/                 # HomeScreen, Mission Control, Telemetry cards, Quick Actions
    ├── plants/               # PlantCatalogScreen, Bed status, Irrigation trackers
    ├── rover/                # RoverScreen (Twin-stick, Cockpit, Mode Selector, E-Stop)
    ├── settings/             # SettingsScreen (8-Module Hub), Sub-screens
    └── viewmodel/            # FarmViewModel (Single Source of Truth)
```

### Unidirectional Data Flow (UDF)
1. **Single Source of Truth:** `FarmViewModel` maintains primary application state as immutable StateFlows (`telemetry`, `plants`, `alerts`, `sensorReadings`, `analyticsUiState`, `settings`).
2. **Deterministic Synchronization:** State updates from repository events, user preference changes, or simulated engine ticks emit through Kotlin Flows to composable UI screens.
3. **Hardware Isolation:** Actuation commands are routed through `RoverCommandPipeline`, which enforces `ACTUATORS_ENABLED = 0` before any network packet can be dispatched.

---

## 3. Application Modules & User Interface Destinations

The application shell provides five primary bottom navigation destinations plus universal overlay surfaces:

### 1. Home (Dashboard & Mission Control)
- High-level mission overview (`STANDBY`, `AUTONOMOUS SCOUTING`, `MANUAL`).
- Real-time rover telemetry summary (Battery %, Speed, Motor Temp, Heartbeat).
- Multi-sensor acquisition widgets: Dual-probe capacitive soil moisture, DHT22 air temperature/humidity, soil pH, sonar obstacle clearance, solar radiation lux.
- Quick action shortcuts (Field Scan, Dock Rover, Disarm).
- AI Agriculture Assistant entry card.

### 2. Rover (Robotics Cockpit & Piloting Station)
- **Portrait View:** Camera viewport, virtual directional controls, speed presets, tactile emergency stop.
- **Landscape View:** Fullscreen immersive mode (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`), twin-stick dual-axis steering controls, live simulated field position map, latency monitors.
- **Emergency Stop:** Latched hardware/software cutoff button with explicit operator re-arm verification.
- **Failsafe Neutral Dispatch:** Orientation changes and lifecycle pause events automatically emit `JoystickVector.NEUTRAL` (0.0, 0.0).

### 3. My Farm (Crop & Plant Catalog)
- Real-time catalog of crops across farm sectors (Roma Tomato, Sweet Corn, Bell Pepper, Golden Soybean).
- Health percentages, vegetative growth stages, Days-to-Harvest countdowns, and soil parameter tracking.
- Interactive crop hydration simulator with immediate StateFlow updates.

### 4. Analytics (Historical Trends & Telemetry Math)
- Sensor time-series chart powered by `AnalyticsMathEngine` with adaptive bucketing (max 60 chart points).
- Time ranges: 1 Hour, 6 Hours, 24 Hours, 7 Days, 30 Days, Custom Date Range Picker.
- Dual-metric comparison (Soil Moisture Probe 1 vs. Probe 2, Air Temp vs. Soil Temp).
- Statistical rollups: Minimum, Maximum, Mean, Standard Deviation, Trend slope.
- Abnormal reading detection and raw historical telemetry table with pagination.
- Explicit `DEMO DATA` versus `LIVE HARDWARE` provenance pill indicators.

### 5. Settings Hub (8 Modular Sub-Screens)
- **General Settings:** Theme mode (System, Dark, Light), metric/imperial unit preferences.
- **Rover & Device Settings:** Rover name, transport protocol selector, configurable Backend Host URL, simulated engine master toggle.
- **AI Assistant Settings:** Granular privacy consent controls, conversation history purging.
- **Alert Thresholds:** High/low boundary configurations for moisture, temperature, humidity, battery.
- **Notification Preferences:** In-app audio and vibration switches.
- **Privacy & Security:** Data export, local data wipe, network boundary audit.
- **Data Management:** Cache statistics, offline storage diagnostics.
- **About FarmVision:** Gradle version display (`1.0.0`), developer attribution (*Ashish Sampat Pawar*).

### Universal Alert Center Overlay
- Universal operational surface accessible from any screen via top AppBar bell badge.
- Active alert list with severity color-coding (Critical, Warning, Info).
- Filter chips, search bar, acknowledgement actions, and historical resolution logs.
- Deep-link target from Android notification shade.

---

## 4. Cloud AI Gateway & FastAPI Backend

The FarmVision AI Assistant is decoupled from the Android client by a dedicated FastAPI cloud proxy service (`backend/`).

### Architecture Benefits
1. **Zero Credential Exposure:** Android client code and APK binaries contain zero Google Gemini API keys.
2. **Prompt Injection & Safety Control:** Agronomic system prompts, safety checks, and payload validation are managed centrally on the backend.
3. **Cross-Platform Compatibility:** The FastAPI proxy can serve Android clients, web dashboards, and IoT edge devices identically.

### API Endpoints

#### `GET /health` and `GET /api/v1/health`
Indicates service health, uptime, configured model, and Gemini API key configuration status.
```json
{
  "status": "ok",
  "service": "farmvision-ai-assistant",
  "version": "1.0.0",
  "gemini_configured": true,
  "model": "gemini-3.5-flash-lite"
}
```

#### `POST /api/v1/assistant/chat`
Processes farmer inquiries and returns agronomic guidance with detailed provenance.
- **Request Payload:**
  - `client_message_id`: UUID for deduplication and local state mapping.
  - `conversation_id`: ID grouping user discussion threads.
  - `user_message`: Text prompt submitted by the operator.
  - `context`: Aggregated farm context governed by privacy consent (`sensor_context`, `alert_context`, `crop_profile`, `historical_context`).
  - `force_mock`: Boolean forcing offline mock responses for testing.
- **Response Payload:**
  - `client_message_id`: Echoed client UUID.
  - `conversation_id`: Active thread ID.
  - `reply_text`: Agronomic response generated by Gemini.
  - `provenance`: Array of sources consulted (`Google Gemini`, `RoverTelemetry`, `AlertRepository`).
  - `model_used`: Explicit model identification (`gemini-3.5-flash-lite`).

---

## 5. Google Gemini AI Integration & Sensor Honesty

The AI Assistant integration operates under strict agricultural honesty invariants:

1. **Uncalibrated Soil Moisture Invariant:** Raw capacitive sensor readings (`0–4095 ADC counts`) are explicitly labeled as `UNCALIBRATED_ADC`. The AI model is strictly instructed **never** to fabricate a moisture percentage or claim soil is wet/dry without soil-texture calibration curves.
2. **Uncalibrated pH Invariant:** Analog pH probe readings are labeled as `UNCALIBRATED`. The AI advises physical laboratory confirmation before chemical amendments.
3. **Actuator Safety Reminder:** The AI assistant strictly informs users that it has **zero physical actuation capabilities** (`ACTUATORS_ENABLED = 0`) and cannot open valves, run pumps, or steer rovers.
4. **Stale Telemetry Warning:** Readings older than 30 seconds carry `[STALE READING]` tags, instructing the model to warn the operator.
5. **Rate-Limit Handling (HTTP 429):** Backend translates Gemini quota exhaustion into clean JSON with suggested retry delays; Android client locks input and renders a non-intrusive rate-limit warning.

---

## 6. Data Storage & Persistence Implementation Status

| Component | Storage Mechanism | Status | Notes |
|:---|:---|:---:|:---|
| **User Preferences** | Android `SharedPreferences` via `SettingsPreferencesManager` | **PRODUCTION** | Persists theme, measurement units, rover name, backend URL, and simulation engine state. |
| **Alert Storage** | Local JSON file (`alerts.json`) via `JsonFileAlertRepository` | **PRODUCTION** | Persists active alerts, acknowledged status, and resolution timestamps. |
| **Assistant History** | Local JSON file (`assistant_conversations.json`) via `JsonFileAssistantRepository` | **PRODUCTION** | Persists conversation threads, user consent choices, and messages. |
| **Telemetry History** | Local JSON file via `JsonFileTelemetryHistoryRepository` | **PRODUCTION** | Stores timestamped sensor samples with data origin tags. |
| **Relational DB (PostgreSQL)** | None (Stateless FastAPI Backend) | **DEFERRED** | Relational schemas and SQL ORM are intentionally out of scope for the current standalone phase. |

---

## 7. Hardware & Firmware Integration References

### Communication Architecture
The Android client communicates with the rover via direct Wi-Fi SoftAP HTTP requests:
- **SSID:** `AgriRover-AP` | **Password:** `FarmVision2026`
- **Subnet:** `192.168.4.0/24`
- **Main Controller (ESP32 DevKit V1):** `http://192.168.4.1:80`
- **Vision Streamer (ESP32-CAM):** `http://192.168.4.2:81`

### Hardware Invariant: `ACTUATORS_ENABLED = 0`
- All hardware actuator commands (L298N motor bridges, 12V diaphragm pumps, relay coils, servo pan/tilt) are disarmed in software and firmware.
- To commission physical hardware in the future, the embedded robotics team must explicitly set `ACTUATORS_ENABLED = 1` in `firmware/esp32-main/src/main.cpp` after verifying hardware emergency-stop circuits.

---

## 8. Testing & Quality Assurance Verification

- **Android Unit Tests:** **340 / 340 tests passed** (100% success rate). Covers math engines, state machines, repositories, viewmodels, and error mappers.
- **Backend Pytest Suite:** **15 / 15 tests passed** (100% success rate). Covers health checks, schema validation, mock fallback, rate limiting, and timeout mapping.
- **Total Automated Test Count:** **355 / 355 tests passed**.
- **Live Emulator Validation:** Tested on `emulator-5554` running Android 15 (API 35). Verified 5-tab traversal, landscape immersive mode, notification deep-linking, and live Gemini AI conversation.

---

## 9. Security, Privacy & Network Configuration

1. **Cleartext Traffic Restriction:** Enforced via `app/src/main/res/xml/network_security_config.xml`. Public internet traffic requires HTTPS. Cleartext is permitted strictly for the rover SoftAP subnet (`192.168.4.0/24`) and developer loopbacks (`10.0.2.2`, `127.0.0.1`).
2. **Minimal Android Permissions:** Manifest requests only 4 standard permissions (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `POST_NOTIFICATIONS`). Zero invasive permissions.
3. **Secret Isolation:** `backend/.env` is ignored by Git. No API keys exist in the Android source tree.
4. **Dual-Layer Privacy Enforcement:** Client-side `ContextBuilder` nullifies disabled data categories before serialization; backend verifies consent flags before prompt construction.

---

## 10. Build, Packaging & Release Distribution

- **Build Configuration:** AGP 8.7.2, Kotlin 2.0.21, `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`.
- **R8 Minification:** Shrank release APK from 18.2 MB to 5.85 MB (68% size reduction) with ProGuard keep rules.
- **Signing Architecture:** Conditional signing via `local.signing.properties` or environment variables. Production key is supplied by the owner.
- **Release Distribution Package:** Assembled at `C:\PROJECT_RELEASES\FarmVision\Phase15\` containing:
  - `FarmVision-debug.apk` (18.19 MB)
  - `FarmVision-release.apk` (5.85 MB)
  - Complete documentation guides (`RELEASE_BUILD_GUIDE.md`, `ANDROID_SIGNING_GUIDE.md`, etc.)
  - Emulator validation screenshots
  - `FarmVision_Phase15_Release_Package.zip` (24.89 MB, SHA-256: `9af9d9611f7713afb7794f6ff9178b401777e7c4434fc8993f7087e27dabdeaa`).
