# FarmVision AgriRover — Comprehensive Master Roadmap

**Document Version:** 1.0.0 (Phase 1 Baseline)  
**Target Platform:** Android Application (Stage A) followed by Physical Rover Hardware (Stages B–E)  
**Current Milestone:** Phase 6 of 15 Complete (Advanced Rover Controller Verified)  

---

## 1. High-Level Master Lifecycle

The FarmVision AgriRover platform is executed in five discrete, sequential stages:

```
[ Stage A: Android Application (Phases 1–15) ]
                    ↓
[ Stage B: Physical Agricultural Rover Chassis & Actuation ]
                    ↓
[ Stage C: ESP32 Real-Time Firmware & Safety Core ]
                    ↓
[ Stage D: Hardware-in-the-Loop Integration & Failsafes ]
                    ↓
[ Stage E: Final Field Validation & Production Delivery ]
```

---

## 2. Stage A: Android Application — 15 Development Phases

### Phase 1: Requirements and Technical Foundation
- **Status:** **COMPLETED (Phase 1 Baseline)**
- **Objective:** Establish a stable, buildable, modern Android project baseline with a premium visual identity, architecture definitions, and safe GitHub preparation.
- **Expected Deliverables:**
  - Android project configured with Kotlin 2.0.21, Jetpack Compose, Material 3, Gradle Kotlin DSL, and Version Catalog (`gradle/libs.versions.toml`).
  - Java 17 toolchain, CompileSdk 35, TargetSdk 35, MinSdk 26.
  - Centralized theme tokens (`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`).
  - Branded initial entry screen (`EntryScreen.kt`) displaying product identity and verified baseline without fake telemetry.
  - Complete documentation (`README.md`, `docs/architecture.md`, `docs/design-system.md`, `docs/roadmap.md`).
  - Git repository initialization and safe GitHub change staging for `Ashish2876/FarmVision-The-AgriRover`.
- **Dependencies:** Workspace & toolchain inspection.
- **Acceptance Criteria:** Successful Gradle compilation, zero missing dependencies, baseline unit tests pass, no premature features or fake data, clean GitHub diff.
- **Evidence:** JVM unit test pass, `./gradlew assembleDebug` APK creation, source and preview inspection.

---

### Phase 2: Design System and Reusable Components
- **Status:** **COMPLETED (Phase 2 Verified)**
- **Objective:** Build a comprehensive, reusable component library implementing the AgriVision design system.
- **Expected Deliverables:**
  - `AgriStatusChip`: Semantic status indicator with animated pulsing dot beacon (`Nominal`, `Warning`, `Alert`, `Info`, `Standby`).
  - `TelemetryMetricCard`: Live sensor card with trend badges, limits, and progress thresholds.
  - `RoverSpeedGauge`: Custom Canvas circular arc gauge for speed, motor RPM, and battery SoC with calibrated ticks and physics-damped animation.
  - `TactileButton`: Outdoor glove-friendly operational trigger with tactile press depression feedback (Primary, Secondary, Tonal, Danger).
  - `EmergencyStopButton`: High-priority industrial safety trigger with pulsing beacon ring, protective bezel, and distinct engaged state.
  - `ConnectionStateBanner`: Real-time network and packet latency indicator (BLE, Wi-Fi Direct, Offline) with dBm signal indicators.
  - `GlassmorphicCard`: Unified container card with aerodynamic borders and layered depth.
  - `ComponentCatalogScreen`: Interactive showcase gallery with live state manipulation (speed slider, E-Stop cutoff, connection simulation).
- **Dependencies:** Phase 1 (Foundation and tokens).
- **Acceptance Criteria:** All components use tokenized styles, WCAG AA contrast compliance verified, zero hardcoded values, 100% Compose preview coverage.
- **Evidence:** Automated unit tests in `ComponentUnitTest.kt`, instrumented tests in `ComponentCatalogTest.kt`, successful compilation and APK packaging.

---

### Phase 3: App Shell and Navigation
- **Status:** **IN PROGRESS (Phase 3 App Shell, Navigation & Screen Integration)**
- **Objective:** Implement the application shell, Material 3 bottom navigation bar, deterministic back policy, and cross-screen state integration.
- **Implemented Deliverables:**
  - `FarmVisionApp`: Production-grade Material 3 app shell hosting the 5 primary destinations (`Home`, `Rover`, `My Farm`, `Analytics`, `Settings`).
  - Canonical destination definitions with routes and stable test tags (`nav_home`, `nav_rover`, `nav_my_farm`, `nav_analytics`, `nav_settings`).
  - Deterministic Back-Navigation Policy via `BackHandler`:
    1. Dismiss active modal sheet first.
    2. Non-Home tab returns to Home.
    3. Home with nothing open exits app.
  - Android 13+ predictive back compatibility (`enableOnBackInvokedCallback="true"`).
  - Combined Drive & Monitor HUD on Rover screen integrating motor speed gauge, battery, E-Stop safety core, directional D-Pad, 4 live sensor readings (moisture, temperature, humidity, light), and freshness timestamp.
  - State retention across tab switches and configuration changes via `rememberSaveable` and top-level shared `FarmViewModel`.
- **Dependencies:** Phase 2 (Design system components).
- **Acceptance Criteria:** Five destinations reachable with correct selected semantics; back transitions work deterministically; cross-screen actions (Home -> Rover, Home -> My Farm) stay synchronized; unit and instrumented tests compile and pass.
- **Evidence:** 36 automated unit tests passing (`AppShellUnitTest`, `FarmViewModelTest`), Compose instrumented tests compiling cleanly in `FarmVisionAppTest.kt`.

---

### Phase 4: Domain Models and Actual Rover Component Architecture
- **Status:** **COMPLETED (Phase 4 Verified)**
- **Objective:** Establish a pure Kotlin data foundation, canonical sensor models, deterministic simulation boundaries, and draft hardware inventory representations matching the 23-item rover bill of materials.
- **Implemented Deliverables:**
  - `SensorDomainModels`: `DataOrigin` (`Simulated`, `LiveHardware`), `ReadingState` (`Available`, `Stale`, `Unavailable`, `Invalid`), `SensorType` with canonical units and physical/agronomical validity boundaries, `SensorReading` with range validation, and `SoilMoistureDualProbeMapping` consolidating two physical probes into rootzone telemetry.
  - `HardwareModels`: Pure Kotlin models covering the exact 23 inventory items across controllers, sensors, motor driver, 4 drive motors, 3 servos, irrigation, power, and chassis; pin assignments unconfigured by default with structural duplicate GPIO detection.
  - `HardwareCatalogSeed`: Static in-code seed representing the exact 23 inventory components; no item marked `Verified`.
  - Architecture Boundaries: `TelemetrySource`, `CommandSink`, `HardwareCatalogSource`, and injectable `TimeProvider` (`SystemTimeProvider`, `FakeTimeProvider`).
  - Single Source of Truth: `MockFarmRepository` implementing boundaries with unified StateFlow streams and simulated origin markers.
  - Test Suites: 57 JVM unit tests passing (18 new tests covering sensor domain, catalog integrity, provisional assumptions, and pin validation) and 12 connected Android UI tests passing.
- **Dependencies:** Phase 3 (App shell).
- **Acceptance Criteria:** 100% of the 23 inventory components represented honestly; canonical units enforced; out-of-range readings marked invalid; simulated origin explicit; all existing screens and tests preserved.
- **Evidence:** 57 JVM unit tests passing in `SensorDomainUnitTest`, `HardwareInventoryUnitTest`, `FarmViewModelTest`, and `AppShellUnitTest`; 12 connected instrumented tests passing on Android Emulator; documentation in `docs/hardware-data-model.md` and `docs/architecture.md`.

---

### Phase 5: Home Dashboard
- **Status:** **COMPLETED (Phase 5 Verified)**
- **Objective:** Deliver the primary mission overview dashboard presenting field status, quick telemetry widgets, and quick action shortcuts.
- **Implemented Deliverables:**
  - `MissionStatusCard`: High-fidelity glassmorphic mission card displaying `activeMission`, current field, rover mode, rover identifier, heartbeat timestamp, and `DataOrigin.Simulated` marker with live pulsing beacon.
  - `TelemetryQuickGlanceGrid`: 2x2 high-contrast metric card grid highlighting Battery % (with voltage & progress gauge), Speed km/h (with active command), Rootzone Soil Moisture % (with dual-probe mapping), and GPS Fix (with 3D fix & coordinates).
  - `QuickActionBar`: Glove-friendly outdoor operational buttons using `TactileButton` for "Field Scan", "Dock Rover", and "Disarm / Re-Arm Motors".
  - `AlertsTickerBar`: Real-time compact alert ticker banner with severity-themed beacons and one-tap modal sheet expansion.
  - `SecondarySensorsSection`: Multi-sensor telemetry visualizers reflecting Phase 4 hardware inventory (DHT22 Temperature & Humidity, Soil pH, HC-SR04 Sonar distance, and solar lux).
  - Test Suites: 62 automated JVM unit tests passing (18 tests in `FarmViewModelTest`), with zero errors and clean APK build.
- **Dependencies:** Phase 4 (Domain models & mock engine).
- **Acceptance Criteria:** Real-time updates reflect mock stream changes smoothly; zero UI jank; all quick action commands execute safely with E-Stop protection.
- **Evidence:** 62 JVM unit tests passing, successful `assembleDebug` APK generation, Compose UI test compatibility.

---

### Phase 6: Rover Controller
- **Status:** **COMPLETED (Phase 6 Verified)**
- **Objective:** Implement virtual manual joystick piloting, drive modes (Manual, Autonomous Waypoint, Hold Position), speed presets (Low, Medium, High), and fail-safe emergency stop.
- **Expected Deliverables:**
  - Virtual 2-axis joystick composable (`VirtualJoystick.kt`) emitting normalized $(x, y)$ velocity vectors with circular perimeter clamping ($r \le 1.0$), continuous radial dead zone ($0.08$), and single-pointer tracking.
  - Coexistence with legacy `CircularDirectionalController` (D-pad with center STOP) for complete accessibility and test preservation (Option 1A).
  - Software speed preset selector (Low 1.5 km/h, Medium 3.5 km/h, High 5.5 km/h) with uncalibrated duty cycle disclaimer.
  - Drive Mode selector pills (Manual, Autonomous Waypoint, Hold Position) enforcing movement permission in repository layer.
  - Command pipeline rate limiter (20 Hz, 50 ms window) with latest-wins trailing emission and immediate neutral/E-stop bypass.
  - Industrial Red Emergency Stop latching power cutoff with deliberate re-arm.
  - Camera viewport header (`SIMULATED CAMERA STREAM • HARDWARE DISCONNECTED`) with HUD overlay and sensor freshness.
- **Dependencies:** Phase 5 (Dashboard & telemetry state).
- **Acceptance Criteria:** Joystick emits throttled command packets (max 20Hz); E-stop emits immediately with highest priority; E-Stop cannot be cleared by joystick drag or mode change; neutral emitted on lifecycle stop/pause.
- **Evidence:** 84 JVM unit tests passing (22 new Phase 6 tests), 17 connected Android instrumented tests passing on emulator, and successful APK assembly.


---

### Phase 7: Farm and Plant Management
- **Status:** **COMPLETED (Phase 7 Verified)**
- **Objective:** Create comprehensive agricultural parcel, zone, and crop management modules allowing operators to catalog farms, plots, and plantings, monitor growth stages, and track hydration.
- **Expected Deliverables:**
  - 3-tier agricultural domain hierarchy: `Farm`, `Plot`, and `Planting` with origin tracking (`RecordOrigin.SAMPLE` vs `USER`).
  - Zero-dependency atomic JSON file store (`JsonFileFarmManagementRepository`, `FarmJsonSerializer`) with versioned envelope schema (`schemaVersion = 1`), serialized writes, atomic file replacement, and corruption recovery.
  - Strict input validation (`FarmValidator`) and area conversions (`AreaConverter`) with physical sanity bounds.
  - Nested sub-route management screens inside existing `MyFarmDestination` (`FarmOverviewScreen`, `FarmDetailScreen`, `PlotDetailScreen`, `PlantingDetailScreen`, `AddEditFarmScreen`, `AddEditPlotScreen`, `AddEditPlantingScreen`).
  - Extended back navigation handling with dirty form draft discard confirmation (`DiscardChangesDialog`).
  - Atomic cascading deletion confirmation dialog displaying exact dependent counts (`DeleteConfirmationDialog`).
  - Material 3 date picker supporting nullable dates and future dates labeled "Planned" (D5).
  - Chronological sorting with unknown dates sorting last in both ascending and descending directions.
  - Unified repository integration projecting plantings into `plantsFlow` for real-time Home screen reflection without altering Phase 6 rover controls.
- **Dependencies:** Phase 4 (Domain entities) & Phase 3 (Navigation).
- **Acceptance Criteria:** Full CRUD operations across Farm, Plot, and Planting; cascading deletion with exact count confirmation; search and facet filtering work instantly; persistence survives process restart; zero new permissions or third-party dependencies.
- **Evidence:** 107 JVM unit tests passing (23 new Phase 7 tests across domain validation, repository contract, persistence restart, and ViewModel), 10 connected Android instrumented tests passing on emulator (5 new Phase 7 tests + 5 Phase 6 tests), and successful APK assembly.

---

### Phase 8: Sensor Monitoring, Android–Firmware Integration & Release Readiness
- **Status:** **COMPLETED (Phase 8 Verified)**
- **Historical Roadmap Quote:**
  > "### Phase 8: Sensor Monitoring
  > - **Status:** PLANNED
  > - **Objective:** Build dedicated multi-sensor telemetry visualizers for comprehensive environmental and soil monitoring.
  > - **Expected Deliverables:**
  >   - Environmental readings: Ambient Temperature, Relative Humidity, Solar Radiation (PAR), Barometric Pressure.
  >   - Subsurface readings: Multi-depth Soil Moisture, Soil pH, NPK (Nitrogen, Phosphorus, Potassium) indicators.
  >   - Sensor health & calibration status indicators.
  > - **Dependencies:** Phase 4 (Telemetry models) & Phase 2 (Metric components).
  > - **Acceptance Criteria:** Values dynamically color-code based on agronomical thresholds (Optimal, Warning, Critical).
  > - **Evidence:** Unit tests for threshold evaluation rules."
- **Execution & Integration Hardening Scope:**
  - Expanded and executed as comprehensive Android–Firmware Integration Hardening, dual-probe moisture pipeline, unauthenticated fail-safe E-stop with authenticated reset, per-request network binding, camera stream validation, and hardware bring-up readiness.
- **Implemented Deliverables:**
  - **Safety & Protocol Reconciliation:** Constant-time token validation (`constantTimeEquals()`) in firmware. Reconciled canonical reasonCode `"ESTOP_RESET"` across firmware C++, `docs/hardware-protocol.md`, and Android contract tests. Fail-closed client dispatch with `RoverAuthenticationException` when bearer token is unprovisioned.
  - **Pin Map & Hardware Documentation:** Source of truth consolidated in `firmware/esp32-main/include/config.h`. Reconciled historical documentation discrepancies in `docs/hardware-wiring-checklist.md`. Retained GPIO 5 as `DRAFT / UNVERIFIED` with candidate GPIO 33 proposed under review. Level-shifter and common ground warnings preserved. Automated contract test verifies sync between firmware header and documentation.
  - **Android Connection Layer:** Typed `EndpointConnectionStatus` with distinct `Timeout` and `Error` states. Descriptive failure mapping for auth errors, version mismatches, and timeouts. Per-request socket network binding via `activeNetwork.openConnection(url)` in `StandardRoverHttpClient`. Cleartext traffic strictly limited to `192.168.4.1`, `192.168.4.2`, and loopback in `network_security_config.xml`.
  - **Sensor Monitoring & Dual-Probe Pipeline:** Probe 2 saturation detection ($\le 50$ or $\ge 4050$ counts) with `SensorHealthStatus.Warning` and suspect labeling. Retained strictly raw ADC counts (no invented calibration or unsupported percentages). Non-existent sensors absent from measured telemetry.
  - **Camera Stream Validation:** Implemented `validateCameraStream()` in Android client validating HTTP 200, Content-Type (`multipart/x-mixed-replace` or `image/jpeg`), and non-empty byte frames. Documented missing ESP32-CAM firmware contract.
  - **Hardware First-Connection Checklist:** Created `docs/hardware-first-connection-checklist.md` with step-by-step procedures, voltage rail checks, dry-wheel test, and operator sign-off sheet.
- **Dependencies:** Phase 6 (Rover Controller), Phase 7 (Farm & Plant Management), and Track H1–H7 hardware foundations.
- **Acceptance Criteria:** Unauthenticated E-stop halts motion unconditionally; reset strictly requires valid bearer token; all outputs de-energized on reset; no credentials exposed in code or docs; 100% test pass rate.
- **Evidence:** 188 JVM unit tests passing offline (`.\gradlew.bat --offline testDebugUnitTest`), 22/22 connected Android instrumented tests passing on emulator (`.\gradlew.bat --offline connectedDebugAndroidTest`), debug APK built (18,275,459 bytes).

---

### Phase 9: Analytics and Telemetry History
- **Status:** **COMPLETED (Phase 9 Verified)**
- **Historical Roadmap Quote:**
  > "### Phase 9: Analytics and History
  > - **Status:** PLANNED
  > - **Objective:** Historical trend visualization and field analytics.
  > - **Expected Deliverables:**
  >   - Time-series charts (Soil moisture over 24h/7d/30d, battery discharge efficiency).
  >   - Aggregated statistics: Total area scanned, average soil moisture per plot, operational runtime.
  >   - Export telemetry report (CSV / JSON summary).
  > - **Dependencies:** Phase 8 (Sensor telemetry) & Phase 7 (Plant records).
  > - **Acceptance Criteria:** Charts render efficiently without blocking main thread; responsive zoom/pan.
  > - **Evidence:** Benchmark test measuring frame render times during historical data loads."
- **Execution Scope & Architectural Reality:**
  - **Pipeline Preservation:** Preserved direct Wi-Fi SoftAP HTTP pipeline (`192.168.4.1`) to the ESP32 rover. Backend integration (FastAPI / external database / Retrofit) is marked pending / NOT APPLICABLE this phase.
  - **Roadmap Discrepancies & Deferred Deliverables:**
    - Battery discharge efficiency curve: Deferred (requires physical discharge cycle logging and calibrated battery model not present on rover).
    - Total area scanned & operational runtime: Deferred (requires hardware wheel odometry and active session logging not available in firmware telemetry).
    - Telemetry report export (CSV/JSON): Deferred to avoid unverified external file writes or third-party serialization libraries.
  - **Implemented Deliverables:**
    - **Durable Local Storage:** Production-wired `JsonFileTelemetryHistoryRepository` (`telemetry_history.json`) storing live hardware telemetry samples across application process restarts with 30-day retention and 60-second per-metric write throttling.
    - **Multi-Window Analytics:** Rolling windows (`1h`, `6h`, `24h`, `7d`, `30d`) and Material 3 `CustomDateRangeDialog` with 30-day max span enforcement and future date clamping.
    - **Bounded Canvas Charting:** Pure Compose `HistoricalBoundedCanvasChart` capped at 200 points per series, preserving physical data gaps ($>2.5\times$ expected interval) as line breaks.
    - **Dual Raw ADC Probes:** Surface (Probe 1) and Deep (Probe 2) capacitive soil moisture probes plotted as independent raw 12-bit ADC counts (0–4095); never averaged or converted to percentage.
    - **Accessible Table Alternative:** `AccessibleHistoryTable` toggleable alongside charts, with TalkBack semantic descriptions and 48dp touch targets.
    - **Multi-Sensor Overview Averages:** `AnalyticsOverviewCards` displaying honest arithmetic averages across all 5 monitored metrics with provenance labels.
    - **Rule-Based Anomaly Detection:** 3 rule-based anomaly flag sources (validation exclusions, configured threshold breaches, sudden change continuity flags) and plain-language neutral insights.
- **Dependencies:** Phase 4 (Domain models), Phase 7 (Atomic JSON persistence pattern), Phase 8 (Live telemetry ingestor & network pipeline).
- **Acceptance Criteria:** Survives application restart; honest provenance labeling (Demo vs Live Hardware); gaps drawn as breaks; dual probes never averaged; zero new dependencies; 100% test pass rate.
- **Evidence:** Automated JVM unit tests passing offline (`.\gradlew.bat --offline testDebugUnitTest`), verified restart durability in `TelemetryHistoryRepositoryContractTest`, successful debug APK build (`assembleDebug`).

---

### Phase 10: Intelligent Alert and Notification Management
- **Status:** **COMPLETED (Phase 10 Verified)**
- **Historical Roadmap Quote:**
  > "### Phase 10: Alerts and Notifications
  > - **Status:** PLANNED
  > - **Objective:** Real-time alert dispatching, notification management, and customizable agricultural threshold rules.
  > - **Expected Deliverables:**
  >   - Alert inbox with severity badges (Critical, Warning, Advisory).
  >   - In-app toast/banner alerts for urgent hardware events (e.g., Obstacle detected < 30cm).
  >   - Push notification integration using Android NotificationManager with high-priority channels.
  > - **Dependencies:** Phase 4 (Domain events) and Phase 3 (Navigation).
  > - **Acceptance Criteria:** Critical alerts play distinct audio/vibration pattern; acknowledge/dismiss state persisted.
  > - **Evidence:** Unit tests verifying notification channel creation and threshold trigger logic."
- **Execution Scope and Architectural Reality:**
  - **Zero-Actuator Invariant:** Alert processing operates strictly as a read-only monitoring and notification system. Alert rules and lifecycle transitions never issue motor, steering, braking, or actuator commands.
  - **Telemetry Honesty:** In LIVE mode, raw soil moisture ADC readings (0-4095 counts) are strictly never converted to uncalibrated moisture percentages; Soil pH threshold alerts are suppressed with an explicit UNCALIBRATED SENSOR advisory; Unsupported sensors (battery voltage, GPS fix, motor temperature) display "Unavailable" and never raise false alerts. DEMO mode alerts carry visible SIM badges.
- **Implemented Deliverables:**
  - **Domain Models and Severity Hierarchy:** Typed `AlertCategory`, `AlertSeverity` (`Critical`, `Warning`, `Information`), `AlertStatus`, `DataQualityState`, and deterministic composite deduplication keys (`category:ruleId:sourceId:origin`).
  - **Pure Evaluation Engine and Debounce:** Framework-independent `AlertEngine` with N-count debounce confirmation (default 3 cycles) and directional hysteresis recovery margins.
  - **Data Quality and Saturation Validation:** `DataQualityValidator` verifying live electrical margins (<= 50 or >= 4050 counts) and classifying readings (`Valid`, `Missing`, `Invalid`, `Stale`, `Uncalibrated`).
  - **Durable Atomic Storage:** `JsonFileAlertRepository` with atomic file replacement (`.tmp` to `.json`), 30-day/500-record bounded retention pruning, and `.bak` corruption recovery.
  - **Notification Infrastructure:** `AlertNotificationManager` with 3 Android notification channels (`Critical`, `Warning`, `Information`), repeat cooldown throttling (5m critical, 15m warning), and `FLAG_IMMUTABLE` deep-link PendingIntents into the Alert Centre.
  - **Alert Centre and Settings UI:** Unified `AlertCenterScreen` (search, severity pills, category filters, tabs), `AlertDetailsScreen` (audit provenance, lifecycle history), `AlertSettingsScreen` (threshold tuning with validation), and `AlertHistoryScreen`.
- **Dependencies:** Phase 4 (Domain models), Phase 8 (Telemetry pipeline), Phase 9 (Persistent storage pattern).
- **Acceptance Criteria:** All alerts categorized with honest provenance; strict read-only evaluation; 100% test pass rate.
- **Evidence:** 273 automated unit tests passing (0 failures, 0 errors), clean debug APK assembled, verified on emulator.

---

### Phase 11: Device and Communication Architecture
- **Status:** PLANNED
- **Objective:** Formalize the transport abstraction and implement production-ready protocol bridges (BLE / Wi-Fi SoftAP / WebSocket).
- **Expected Deliverables:**
  - Bluetooth Low Energy (BLE) scanner, GATT service discovery, and MTU negotiation.
  - Wi-Fi Socket / WebSocket client with auto-reconnect and exponential backoff.
  - Protocol serializer/deserializer with CRC16 frame validation.
  - Device pairing and discovery management screen.
- **Dependencies:** Phase 4 (Telemetry provider contracts) & Communication protocol selection.
- **Acceptance Criteria:** Seamless failover between protocols; connection loss triggers warning within 250ms.
- **Evidence:** Network disconnect simulation test and protocol parsing unit tests.

---

### Phase 12: AI Agriculture Assistant
- **Status:** PLANNED
- **Objective:** Integrate on-device computer vision and agronomic recommendation intelligence.
- **Expected Deliverables:**
  - On-device inference pipeline for leaf disease classification (TensorFlow Lite / MediaPipe).
  - Camera scan interface with live bounding box overlay and confidence score.
  - Conversational agronomic advice assistant answering crop management questions.
- **Dependencies:** Phase 7 (Plant models) & Phase 6 (Camera feed).
- **Acceptance Criteria:** On-device model inferences complete in $<150\text{ms}$ on modern mid-range Android devices.
- **Evidence:** TFLite model inference benchmark report and classification accuracy test suite.

---

### Phase 13: Settings, Persistence, and Personalization
- **Status:** PLANNED
- **Objective:** User preferences, local database persistence (Room), and device calibration settings.
- **Expected Deliverables:**
  - Room database implementation for offline caching of telemetry, alerts, and field logs.
  - DataStore Preferences for user settings (Imperial/Metric units, Dark/Light theme, joystick deadzones).
  - Rover calibration wizard (Compass/IMU recalibration, motor trim adjustment).
- **Dependencies:** Phase 1 (Foundation) & Phase 4 (Domain models).
- **Acceptance Criteria:** Settings persist across app cold starts; database migrations tested and versioned.
- **Evidence:** Room migration tests and DataStore integration unit tests.

---

### Phase 14: Integration, Testing, Accessibility, and Optimization
- **Status:** PLANNED
- **Objective:** End-to-end integration across all app modules, performance profiling, accessibility auditing, and battery optimization.
- **Expected Deliverables:**
  - End-to-end user journey tests (Dashboard → Piloting → Plant Scan → Historical Review).
  - Baseline Profiles and R8 optimization for sub-second cold starts.
  - Full TalkBack screen reader accessibility and content description coverage.
- **Dependencies:** Phases 1 through 13.
- **Acceptance Criteria:** Zero memory leaks detected via LeakCanary; TalkBack audit passes 100%; startup time $< 800\text{ms}$.
- **Evidence:** Android Vitals reports, automated UI test suite recording, accessibility audit logs.

---

### Phase 15: Final Polish and Delivery
- **Status:** PLANNED
- **Objective:** Final visual polish, release signing, user manual, and production APK/AAB generation.
- **Expected Deliverables:**
  - Release APK and Google Play App Bundle (AAB).
  - Final signed Git release tag (`v1.0.0`).
  - Comprehensive user and developer manuals.
- **Dependencies:** Phase 14 (All tests passing).
- **Acceptance Criteria:** Release build passes lint with zero warnings; release binary verified on target physical test devices.
- **Evidence:** Final APK verification, checksum generation, and release documentation.

---

### Track H: Physical Hardware Center, Live Integration & Protocol Engineering
- **Current Milestone:** **TRACK H COMPLETED & VERIFIED (Milestones H1–H7 Complete)**

| Milestone | Scope | Deliverables & Progress | Status |
|---|---|---|---|
| **H1** | UI Foundation & Catalog Presentation | Hardware Center entry, 23 components cataloged, 5 status axes, truthful unconfigured states, zero false claims. | **COMPLETED** |
| **H2** | 7 Status Axes, Settings & Prober | SharedPreferences persistence, ConnectionTester probe engine, dynamic counting rules (13 electronic items), Demo/Live mode toggle. | **COMPLETED** |
| **H3** | Protocol Spec & Pure Code Models | Formal protocol specification v1.0.0 (`docs/hardware-protocol.md`), hardware wiring checklist (`docs/hardware-wiring-checklist.md`), pure Kotlin models (`RoverProtocolModels.kt`), validator (`ProtocolValidator.kt`), message parser (`ProtocolMessageParser.kt`), 9 contract fixtures, 23 unit tests. | **COMPLETED** |
| **H4** | Android Networking Layer | Scoped cleartext config (`network_security_config.xml`), Wi-Fi SoftAP `bindProcessToNetwork` (`RoverNetworkBinder`), `StandardRoverHttpClient` & `RoverProtocolClient`, live telemetry polling, token redaction, 15 unit tests. | **COMPLETED** |
| **H5** | ESP32 Firmware (F0 Sensors Only) | PlatformIO/Arduino ESP32 firmware in `firmware/esp32-main/`, FreeRTOS dual-core tasks, Wi-Fi SoftAP (`192.168.4.1`), REST endpoints (`/capabilities`, `/telemetry`, `/command`, `/emergency-stop`, `/status`), actuators strictly disabled in F0 (`ERR_ACTUATOR_DISABLED`), `firmware/README.md`, cross-platform contract tests. Flashing gated by `AUTHORIZE FLASH`. | **COMPLETED** |
| **H6** | Actuator Verification & Stage F1 | Physical wiring verification guide (`docs/hardware-verified.md`), TB6612FNG differential drive (20 kHz PWM, 5% deadband, slew rate limiter), 3 kinematic servos, 10s pump safety cutoff, 250ms deadman failsafe, E-Stop latching core, 7 unit tests. Physical actuation gated by `WIRING CONFIRMED`. | **COMPLETED** |
| **H7** | Final Polish, Accessibility & Handover | Accessibility semantics (`heading()`, `Role.Button`, filter content descriptions), WCAG AAA/AA contrast compliance, 2.0x font scaling previews, comprehensive handover manual (`docs/hardware-handover.md`), full regression suite (180 tests passing). | **COMPLETED** |

---


## 3. Subsequent Hardware & Integration Stages (Stages B through E)

*Note: The following stages represent physical rover engineering and are scheduled after the completion of Stage A application milestones.*

```mermaid
gantt
    title FarmVision AgriRover Complete Program Schedule
    dateFormat  YYYY-MM
    section Stage A: Android App
    Phase 1 : Foundation & Architecture      :done, p1, 2026-10, 1M
    Phases 2-5 : Design, Nav, Dash          :p2, after p1, 2M
    Phases 6-10 : Controller, Plants, Alert  :p3, after p2, 2M
    Phases 11-15 : Device, AI, Polish        :p4, after p3, 2M
    section Stage B: Rover Hardware
    Chassis & Motors Assembly               :b1, after p3, 2M
    Power System & Electronics              :b2, after b1, 1M
    section Stage C: ESP32 Firmware
    Firmware Sensing & Motor Control        :c1, after b2, 2M
    Safety Watchdog & Failsafes             :c2, after c1, 1M
    section Stage D & E: Field Integration
    HIL Telemetry Testing                   :d1, after c2, 1M
    Final Field Trials & Delivery           :e1, after d1, 1M
```

### Stage B: Physical Agricultural Rover (Hardware Engineering)
1. **Chassis & All-Terrain Drivetrain**: Sturdy 4WD or tracked chassis with high ground clearance for furrowed soil.
2. **Motors & Actuators**: High-torque metal gear DC motors with optical/magnetic encoders and dual H-bridge motor drivers (e.g., BTS7960 or L298N high-current).
3. **Power Management**: High-capacity LiFePO4 or 18650 Li-ion battery pack with BMS (Battery Management System), DC-DC step-down converters (5V and 3.3V logic rails), and isolated power filtering.
4. **Sensor Payloads**: Soil capacitive moisture probes, DHT22/SHT31 environmental sensors, ultrasonic/LiDAR obstacle sensors, and optical camera module.

### Stage C: ESP32 Firmware & Safety Controller
1. **Core RTOS Architecture**: FreeRTOS tasks partitioned across ESP32 dual cores (Core 0: High-speed communications & safety watchdog; Core 1: Motor PWM generation, encoder PID loops, and sensor polling).
2. **Command Interpreter**: Binary/JSON packet parser validating CRC checksums, sequence IDs, and velocity bounds.
3. **Hardware Watchdog & Fail-Safe Core**: Standalone timer cutting motor enable pins if no valid heartbeat is received within 500ms.

### Stage D: Hardware-in-the-Loop Integration
1. **End-to-End Handshake**: Android app connects to real ESP32; validates live sensor telemetry streams against physical values.
2. **Stress & Fault Injection**: Disconnecting battery cables, creating radio interference, and verifying immediate motor shutdown on the physical rover.

### Stage E: Final Validation & Field Delivery
1. **Agronomic Field Trials**: Full daytime outdoor trials across crops (corn, tomatoes, soybeans).
2. **Thermal & Environmental Stress**: Validating rover operation under high ambient heat ($>40^\circ\text{C}$) and dusty soil conditions.
3. **Production Handover**: Complete system documentation, schematics, source repositories, and demonstration video.
