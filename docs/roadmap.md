# FarmVision AgriRover — Comprehensive Master Roadmap

**Document Version:** 1.0.0 (Phase 1 Baseline)  
**Target Platform:** Android Application (Stage A) followed by Physical Rover Hardware (Stages B–E)  
**Current Milestone:** Phase 1 of 15 Complete (Baseline Verification & GitHub Preparation)  

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
- **Status:** PLANNED (Awaiting Phase 2 Approval)
- **Objective:** Implement the application shell, top app bars, adaptive navigation rail (tablets) / navigation bar (phones), and type-safe routing.
- **Expected Deliverables:**
  - Navigation Compose integration using Kotlin Serialization type-safe routes.
  - Adaptive layout structure responding to window size classes (Compact, Medium, Expanded).
  - Top app bar displaying rover connection status pill, active field name, and quick-access emergency stop.
- **Dependencies:** Phase 2 (Design system components).
- **Acceptance Criteria:** Back-stack transitions work smoothly without memory leaks; deep-linking route structure established.
- **Evidence:** Navigation unit tests, Compose UI test navigating between stub destinations.

---

### Phase 4: Domain Models and Mock Data Architecture
- **Status:** PLANNED
- **Objective:** Establish the pure Kotlin domain layer, business entities, and high-fidelity physics-based simulation engine (`MockRoverEngine`).
- **Expected Deliverables:**
  - Domain models: `RoverTelemetry`, `SensorPacket`, `SoilProfile`, `PlantHealthRecord`, `RoverCommand`.
  - Interfaces: `RoverTelemetryProvider`, `RoverCommandProvider`.
  - `MockRoverEngine`: Coroutine-driven generator simulating battery drain, GPS waypoint movement, soil moisture fluctuations, and simulated packet drops.
- **Dependencies:** Phase 3 (App shell).
- **Acceptance Criteria:** UI can run entirely without physical hardware; simulation strictly flagged as "SIMULATION ACTIVE".
- **Evidence:** JUnit 4 tests validating mock telemetry stream frequency, value bounds, and cancellation behavior.

---

### Phase 5: Home Dashboard
- **Status:** PLANNED
- **Objective:** Deliver the primary mission overview dashboard presenting field status, quick telemetry widgets, and quick action shortcuts.
- **Expected Deliverables:**
  - Mission status summary card (Active Mission, Current Field, Rover Mode).
  - 4-card telemetry quick-glance grid (Battery %, Speed km/h, Soil Moisture %, GPS Fix).
  - Quick action bar (Start Field Scan, Return to Dock, Disarm Motors).
  - Recent alerts ticker.
- **Dependencies:** Phase 4 (Domain models & mock engine).
- **Acceptance Criteria:** Real-time updates reflect mock stream changes smoothly with 60fps animations; zero UI jank.
- **Evidence:** Compose UI test verifying telemetry value rendering and card interactions.

---

### Phase 6: Rover Controller
- **Status:** PLANNED
- **Objective:** Implement virtual manual joystick piloting, drive modes (Manual, Autonomous Waypoint, Hold Position), and emergency stop.
- **Expected Deliverables:**
  - Virtual 2-axis joystick composable emitting normalized $(x, y)$ velocity vectors.
  - Speed limit selector (Low/Medium/High torque modes).
  - Prominent emergency stop overlay with immediate motor cut confirmation.
  - Camera viewport placeholder with telemetry HUD overlay.
- **Dependencies:** Phase 5 (Dashboard & telemetry state).
- **Acceptance Criteria:** Joystick emits throttled command packets (max 20Hz); E-stop emits immediately with highest priority.
- **Evidence:** Automated test verifying command rate limiting and E-Stop preemption.

---

### Phase 7: Farm and Plant Management
- **Status:** PLANNED
- **Objective:** Create crop and field management modules allowing operators to catalog crops, monitor plant rows, and view health indices.
- **Expected Deliverables:**
  - Field section list and crop variety cards.
  - Plant health profile sheet with chlorophyll, moisture, and growth stage metrics.
  - Add / Edit crop profile workflows.
- **Dependencies:** Phase 4 (Domain entities) & Phase 3 (Navigation).
- **Acceptance Criteria:** CRUD operations update local state; search and filter by health condition work instantly.
- **Evidence:** Repository unit tests and Compose screen tests.

---

### Phase 8: Sensor Monitoring
- **Status:** PLANNED
- **Objective:** Build dedicated multi-sensor telemetry visualizers for comprehensive environmental and soil monitoring.
- **Expected Deliverables:**
  - Environmental readings: Ambient Temperature, Relative Humidity, Solar Radiation (PAR), Barometric Pressure.
  - Subsurface readings: Multi-depth Soil Moisture, Soil pH, NPK (Nitrogen, Phosphorus, Potassium) indicators.
  - Sensor health & calibration status indicators.
- **Dependencies:** Phase 4 (Telemetry models) & Phase 2 (Metric components).
- **Acceptance Criteria:** Values dynamically color-code based on agronomical thresholds (Optimal, Warning, Critical).
- **Evidence:** Unit tests for threshold evaluation rules.

---

### Phase 9: Analytics and History
- **Status:** PLANNED
- **Objective:** Historical trend visualization and field analytics.
- **Expected Deliverables:**
  - Time-series charts (Soil moisture over 24h/7d/30d, battery discharge efficiency).
  - Aggregated statistics: Total area scanned, average soil moisture per plot, operational runtime.
  - Export telemetry report (CSV / JSON summary).
- **Dependencies:** Phase 8 (Sensor telemetry) & Phase 7 (Plant records).
- **Acceptance Criteria:** Charts render efficiently without blocking main thread; responsive zoom/pan.
- **Evidence:** Benchmark test measuring frame render times during historical data loads.

---

### Phase 10: Alerts and Notifications
- **Status:** PLANNED
- **Objective:** Real-time alert dispatching, notification management, and customizable agricultural threshold rules.
- **Expected Deliverables:**
  - Alert inbox with severity badges (Critical, Warning, Advisory).
  - In-app toast/banner alerts for urgent hardware events (e.g., Obstacle detected < 30cm).
  - Push notification integration using Android NotificationManager with high-priority channels.
- **Dependencies:** Phase 4 (Domain events) & Phase 3 (Navigation).
- **Acceptance Criteria:** Critical alerts play distinct audio/vibration pattern; acknowledge/dismiss state persisted.
- **Evidence:** Unit tests verifying notification channel creation and threshold trigger logic.

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
