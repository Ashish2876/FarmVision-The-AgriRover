# FarmVision AgriRover

[![Platform](https://img.shields.io/badge/Platform-Android%20(Kotlin%20%2F%20Compose)-123B2A?style=for-the-badge&logo=android)](app/)
[![Java](https://img.shields.io/badge/Java%20Toolchain-17-62C985?style=for-the-badge&logo=openjdk)](gradle/libs.versions.toml)
[![Material 3](https://img.shields.io/badge/Design%20System-Material%203-218C5B?style=for-the-badge)](docs/design-system.md)
[![Repository](https://img.shields.io/badge/GitHub-Ashish2876%2FFarmVision--The--AgriRover-0B1410?style=for-the-badge&logo=github)](https://github.com/Ashish2876/FarmVision-The-AgriRover)

> **FarmVision AgriRover** is an intelligent smart-agriculture robotics platform combining advanced field vision, autonomous rover telemetry, sensor acquisition, and precision crop analytics.

---

## 1. Application Overview

FarmVision AgriRover launches directly into a polished, production-style mobile operator command center built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

The application features a 5-destination bottom navigation shell:

1. **Home Dashboard**:
   - Live rover battery, voltage, and connection state.
   - Real-time agricultural sensor cards: Rootzone Soil Moisture (`%`), Ambient Temperature (`°C`), Air Humidity (`%`), and Solar Radiation (`lx`).
   - Monospace tabular figures (`tnum`) to eliminate layout jitter during telemetry polling.
   - Crop health summary across monitored plots.
   - Recent environmental threshold alerts and quick actions.
2. **Rover Control Center**:
   - Virtual 2-axis piloting joystick with continuous radial dead zone and physics spring return.
   - Circular directional D-pad with central STOP for complete accessibility and dual-mode piloting.
   - Tactile vector telemetry and differential steering kinematics HUD (Left/Right duty cycle).
   - Speed preset selector (Low, Medium, High) with uncalibrated duty cycle disclaimer.
   - Drive mode selector (Manual Piloting, Autonomous Waypoint Scan, Hold Position).
   - Dedicated **Emergency Stop (E-Stop)** safety core with immediate power cutoff and motor re-arming.
3. **My Farm**:
   - Crop catalog with variety details, plot locations, and growth stages.
   - Rootzone moisture monitoring and watering recommendations.
   - Interactive irrigation logging and category filtering (All, Healthy, Needs Attention).
4. **Field Analytics**:
   - Custom Canvas time-series line charts for historical soil moisture and ambient temperature trends.
   - Time-range selector (24 Hours, 7 Days, 30 Days).
   - Aggregated agronomic KPIs (average daily moisture, thermal swings, irrigation volume).
5. **Settings & Preferences**:
   - Rover connection protocol configuration (Wi-Fi SoftAP, Bluetooth Low Energy, MQTT Broker).
   - Simulation / Mock Engine toggle.
   - Measurement unit customization (Celsius/Fahrenheit, Metric/Imperial).
   - Platform architecture and safety watchdog specifications.

> **Simulation / Hardware Boundary Notice**: All sensor telemetry, rover actuation vectors, and historical trends are generated locally via `MockFarmRepository` and explicitly labelled with **DEMO DATA** badges. Physical rover motors, ESP32 firmware actuation, and live field communication will be integrated during subsequent hardware milestones.

---

## 2. Technology Stack & Dependencies

| Layer | Technology | Specification |
| :--- | :--- | :--- |
| **Language** | Kotlin | `2.0.21` |
| **UI Framework** | Jetpack Compose & Material 3 | Compose BOM `2024.10.01` |
| **Build Tooling** | Gradle & Android Gradle Plugin (AGP) | Gradle `8.11` • AGP `8.7.2` |
| **Dependency Management** | Gradle Version Catalog | `gradle/libs.versions.toml` |
| **JDK Toolchain** | OpenJDK / Microsoft JDK | Java 17 (`17.0.18+8`) |
| **Android SDKs** | Compile SDK / Target SDK / Min SDK | Compile `35` • Target `35` • Min `26` (Android 8.0+) |
| **Architecture** | Unidirectional Data Flow (UDF) & Clean Architecture | Layered Presentation, Domain, and Data |
| **Testing** | JUnit 4, AndroidX Test, Compose UI Test | JUnit `4.13.2` • 188 Passing JVM Unit Tests (20 suites) • 22 Connected Android Tests |
| **Microcontroller Firmware** | ESP32 DevKit V1 FreeRTOS C++ | PlatformIO • Dual-Core RTOS • Rest API v1.0.0 |

---

## 3. Visual Identity & Design Principles

FarmVision AgriRover introduces a nature-inspired agricultural tech design language:
- **Nature-Inspired Palette**:
  - `Deep Forest (#123B2A)`: Grounded primary containers and structural anchors.
  - `Primary Green (#218C5B)`: High-priority operational triggers and brand highlights.
  - `Light Primary Green (#176840)`: High-contrast button containers achieving 6.34:1 WCAG AA contrast.
  - `Fresh Leaf (#62C985)`: Active telemetry indicators and prominent headings.
  - `Mint Accent (#A8E6C1)`: Tactical highlights, chip borders, and glowing badges.
  - `Warm Off-White (#F5F8F3)`: Clean, readable light background.
  - `Dark Background (#0B1410)` & `Dark Surface (#14221B)`: High-contrast outdoor canvases.
- **Accessibility & Outdoor Ergonomics**:
  - Minimum touch target standard: $\ge 48\text{dp}$ on all interactive triggers.
  - Semantic status indicators always combine geometric icons with explicit text (never color dot alone).
  - Tabular numeric figures (`fontFeatureSettings = "tnum"`) prevent character shifting during continuous data streams.

---

## 4. Repository Structure

```text
FarmVision/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/farmvision/agrirover/
│       │   │   ├── MainActivity.kt        # Launches FarmVisionApp shell directly
│       │   │   ├── data/
│       │   │   │   ├── local/             # SettingsPreferencesManager (SharedPreferences persistence)
│       │   │   │   ├── model/             # RoverTelemetry, PlantItem, FarmAlert, SensorDataPoint, FarmSettings
│       │   │   │   └── repository/        # FarmRepository & MockFarmRepository (StateFlow streams)
│       │   │   ├── core/designsystem/
│       │   │   │   ├── component/         # Reusable buttons, cards, status, metrics, states
│       │   │   │   └── theme/             # Spacing, Contrast calculator, Theme extensions
│       │   │   └── ui/
│       │   │       ├── app/               # FarmVisionApp & FarmVisionDestination (Bottom Nav)
│       │   │       ├── viewmodel/         # FarmViewModel (Shared StateFlow architecture)
│       │   │       ├── home/              # HomeScreen (Dashboard, telemetry, and notification sheet)
│       │   │       ├── rover/             # RoverScreen (Piloting pad, E-Stop cutoff, speed HUD)
│       │   │       ├── farm/              # MyFarmScreen (Crop catalog & reactive irrigation sync)
│       │   │       ├── analytics/         # AnalyticsScreen (Dynamic 24h/7d/30d Canvas charts)
│       │   │       ├── settings/          # SettingsScreen (Persistent units & protocol bridge)
│       │   │       ├── entry/             # Branded foundation entry screen
│       │   │       └── theme/             # Color, Type, Shape, Theme
│       │   └── res/                       # Vectors, launcher mipmaps, values
│       ├── debug/                         # Debug-only showcase activity and launcher
│       ├── release/                       # Release stubs (showcase excluded)
│       ├── test/                          # 180 JVM unit tests (100% pass across 19 suites)
│       └── androidTest/                   # Instrumented Compose UI tests (Navigation, Controller & BottomSheet)
├── firmware/
│   └── esp32-main/                        # PlatformIO C++ FreeRTOS robotics firmware (Dual-core, SoftAP, REST API)
└── docs/
    ├── architecture.md                    # Core architecture, layers, network security, and safety core
    ├── design-system.md                   # Tokens, WCAG contrast, component anatomy, accessibility
    ├── hardware-handover.md               # Complete hardware integration manual & operational guide
    ├── hardware-verified.md               # Bench testing invariants, electrical checklist & sign-off sheet
    ├── hardware-protocol.md               # Formal REST protocol specification v1.0.0 & schemas
    ├── hardware-wiring-checklist.md       # Pinout maps, candidate GPIOs, level shifter requirements
    └── roadmap.md                         # Master lifecycle, Track H completion, and Stages B–E schedule
```

---

## 5. Build & Test Instructions

Execute standard Gradle commands from the project root:

```bash
# Run all local JVM unit tests (180 tests across 19 suites)
./gradlew testDebugUnitTest

# Assemble the Debug APK
./gradlew assembleDebug

# Install and launch on an attached Android device (via ADB)
./gradlew installDebug
adb shell monkey -p com.farmvision.agrirover 1
```

The compiled debug APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Documentation Index

- [docs/architecture.md](docs/architecture.md): Presentation, Domain, Data, network security, and hardware safety core.
- [docs/design-system.md](docs/design-system.md): Color tokens, measured WCAG contrast ratios, typography scale, spacing grid, and Hardware Center component anatomy.
- [docs/hardware-handover.md](docs/hardware-handover.md): Complete engineering handover guide, pinout tables, bench tests, and protocol v1.0.0 reference.
- [docs/hardware-verified.md](docs/hardware-verified.md): Physical verification invariants (wheels off ground) and electrical sign-off checklist.
- [docs/hardware-protocol.md](docs/hardware-protocol.md): Protocol schema, message contracts, error codes, and validation rules.
- [docs/hardware-wiring-checklist.md](docs/hardware-wiring-checklist.md): Wiring checklists, level-shifter requirements, and pin definitions.
- [docs/roadmap.md](docs/roadmap.md): Complete lifecycle schedule covering Android application phases, Track H completion, and physical rover stages (B–E).
