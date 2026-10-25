# FarmVision AgriRover — Comprehensive Hardware Handover & Integration Guide

**Document Version:** 1.0.0  
**Status:** TRACK H FINAL HANDOVER & INTEGRATION MANUAL  
**Application ID:** `com.farmvision.agrirover`  
**Firmware Location:** `firmware/esp32-main/`  

---

## 1. System Overview & Architectural Integration

The FarmVision AgriRover platform integrates a modern, accessibility-compliant Android Jetpack Compose control application with an embedded ESP32 FreeRTOS robotics controller. The system enforces strict architectural boundaries:

```
+-----------------------------------------------------------------------------------+
|                           Android Jetpack Compose Client                          |
|  - Hardware Center (23 Catalog Items, 6 Categories, Live vs. Demo Mode)           |
|  - Rover Controller (Phase 6 Joystick & D-Pad, 20 Hz Rate Limiter, E-Stop)        |
|  - Scoped Cleartext Network Security (192.168.4.0/24 & Emulators Only)            |
|  - Wi-Fi SoftAP Process Network Binder (ConnectivityManager.bindProcessToNetwork) |
+-----------------------------------------------------------------------------------+
                                         |
                  Wi-Fi SoftAP ("AgriRover-AP" / 192.168.4.1)
                                         |
+-----------------------------------------------------------------------------------+
|                        ESP32 DevKit V1 FreeRTOS Controller                        |
|  - Core 0: Asynchronous REST Web Server (/capabilities, /telemetry, /command)     |
|  - Core 1: Sensor Engine (DHT22, Dual Soil Moisture, pH, Ultrasonic HC-SR04)      |
|  - Core 1: Actuator Engine (TB6612FNG Differential Drive, 3 Servos, Pump Relay)   |
|  - Safety Watchdog: 250ms Command Deadman, 10s Pump Cutoff, E-Stop Latching Core |
+-----------------------------------------------------------------------------------+
                                         |
                                (Planned Vision Link)
                                         v
+-----------------------------------------------------------------------------------+
|                         ESP32-CAM Video Stream Endpoint                           |
|  - OV2640 Image Sensor / MJPEG Video Stream (Draft IP: 192.168.4.2:80)            |
|  - Flashing Tool: ESP32-CAM-MB Micro-USB Daughterboard (Programming Only)         |
+-----------------------------------------------------------------------------------+
```

---

## 2. Operation Modes: Demo Simulation vs. Live Hardware

### 2.1 Demo Simulation Mode (Default on First Run)
- Operates locally inside the Android application without requiring hardware radio connections.
- Generates realistic, physically bounded agronomic telemetry (temperature, humidity, capacitive soil moisture, distance) for testing and UI evaluation.
- All 13 electronic components report `EndpointConnectionStatus.Unknown` or `SensorHealthStatus.Uncalibrated`; never falsely asserts physical connectivity.

### 2.2 Live Hardware Mode
- Direct communication with the physical rover over Wi-Fi SoftAP (`SSID: AgriRover-AP`, `IP: 192.168.4.1:80`).
- **Android SoftAP Routing Invariant:** In Android 9+, internetless Wi-Fi connections cause Android to drop packets or route traffic over cellular data. The Android app implements [`RoverNetworkBinder`](file:///c:/PROJECT/FarmVision/app/src/main/java/com/farmvision/agrirover/data/network/RoverNetworkBinder.kt), which invokes `connectivityManager.bindProcessToNetwork(roverWifiNetwork)` using the Android `Network` object to route HTTP traffic cleanly to the rover.
- Standard endpoints require bearer token authentication (`X-AgriRover-Token: <ROVER_AUTH_BEARER_TOKEN>`). Note: Emergency stop (`POST /api/v1/emergency-stop`) is accepted unconditionally without authentication so safety cutoffs are never blocked, while resetting the latch (`POST /api/v1/emergency-stop/reset`) strictly requires valid authentication.

---

## 3. Microcontroller Pinout & Wiring Specification

> [!WARNING]
> **UNVERIFIED CANDIDATE PINOUTS:** All GPIO pin assignments listed below are candidate draft assignments based on canonical `firmware/esp32-main/include/config.h` and must be verified on the physical breadboard / perfboard before energizing motor or power rails.

### 3.1 ESP32 DevKit V1 (Main Controller) Pin Map

| Subsystem / Component | Component ID | Signal Pin | ESP32 GPIO | Voltage Level | Notes / Protection |
|---|---|---|---|---|---|
| **TB6612FNG Motor Driver** | `hw-driver-tb6612fng` | PWMA (Left Speed) | **GPIO 25** | 3.3V Logic | 20 kHz Ultrasonic PWM (Ch 0) |
| | | AIN1 (Left Dir A) | **GPIO 26** | 3.3V Logic | Direction logic |
| | | AIN2 (Left Dir B) | **GPIO 27** | 3.3V Logic | Direction logic |
| | | PWMB (Right Speed)| **GPIO 16** | 3.3V Logic | 20 kHz Ultrasonic PWM (Ch 1) |
| | | BIN1 (Right Dir A)| **GPIO 17** | 3.3V Logic | Direction logic |
| | | BIN2 (Right Dir B)| **GPIO 18** | 3.3V Logic | Direction logic |
| | | STBY (Standby) | **GPIO 19** | 3.3V Logic | Active HIGH, pulled LOW on halt |
| **DHT22 Climate Sensor** | `hw-sensor-dht22` | DATA | **GPIO 4** | 3.3V Logic | 4.7kΩ pull-up resistor required |
| **Soil Moisture Probe 1** | `hw-sensor-soil-moisture-probes`| AOUT 1 | **GPIO 32** | Analog (0-3.3V) | ADC1_CH4, 2-point % VWC calibration |
| **Soil Moisture Probe 2** | `hw-sensor-soil-moisture-probes`| AOUT 2 | **GPIO 34** | Analog (0-3.3V) | ADC1_CH6 (Input Only), Raw counts |
| **Soil pH Electrode** | `hw-sensor-soil-ph` | AOUT | **GPIO 35** | Analog (0-3.3V) | ADC1_CH7 (Input Only), Buffer Cal Required |
| **HC-SR04 Ultrasonic** | `hw-sensor-ultrasonic` | TRIG | **GPIO 13** | 3.3V Logic | 10 µs pulse trigger output |
| | | ECHO | **GPIO 14** | **5.0V Logic** | **MANDATORY: 1kΩ / 2kΩ resistive divider to 3.3V** |
| **Camera Tilt Servo** | `hw-servo-camera` | PWM | **GPIO 21** | 3.3V / 50 Hz | 5V external servo rail (Ch 2) |
| **Soil Probe Servo** | `hw-servo-soil-probe` | PWM | **GPIO 22** | 3.3V / 50 Hz | 5V external servo rail (Ch 3) |
| **Ultrasonic Pan Servo** | `hw-servo-ultrasonic` | PWM | **GPIO 23** | 3.3V / 50 Hz | 5V external servo rail (Ch 4) |
| **Irrigation Pump Relay** | `hw-irrigation-relay` | CH1 IN | **GPIO 5** | 3.3V Logic | **DRAFT / UNVERIFIED**: Boot strapping pin (VSPI CS0); may pulse LOW on reset. Review external pull-up or candidate **GPIO 33** before hardware energization. Active LOW trigger (10s max). |

---

## 4. Safety Architecture & Failsafe Watchdogs

1. **Mechanical Emergency Cutoff:** An emergency knife switch or high-current toggle switch must be wired in series with the 12V Li-ion battery positive lead.
2. **250 ms Locomotion Deadman:** Continuous drive commands carry a 250 ms TTL. If command packets cease (due to Wi-Fi dropout, app pause, or radio interference), `ActuatorManager::stopAllActuators()` sets PWM to 0 and pulls STBY LOW within 50 ms.
3. **10-Second Irrigation Auto-Cutoff:** Relay actuation duration is clamped to $\le 10,000\text{ ms}$. The FreeRTOS safety watchdog task opens the relay contacts automatically upon timer expiration.
4. **Emergency Stop Latching Core:** Dispatched via `POST /api/v1/emergency-stop`. Sets `_isEStopLatched = true`, halts locomotion, de-energizes the relay, and rejects subsequent motion commands with HTTP 423 `ERR_EMERGENCY_STOP_ACTIVE` until explicit reset via `POST /api/v1/emergency-stop/reset`.
5. **Bench Test Invariant (Wheels Off Ground):** Mandated that all initial electrical and actuation tests must be performed with the chassis elevated and wheels completely suspended in the air.

---

## 5. Firmware Flashing Procedure (Safety Gated)

> [!CAUTION]
> **FLASHING AUTHORIZATION GATE:** Serial flashing to physical hardware is strictly locked until the user explicitly issues the command: `AUTHORIZE FLASH <PORT>`.

### Toolchain Prerequisites:
- PlatformIO Core CLI or VS Code PlatformIO extension.
- CP2102 or CH340 USB-UART bridge drivers installed.

### Build and Upload Commands:
```bash
# 1. Compile firmware in isolation
cd firmware/esp32-main
pio run

# 2. Upload to physical ESP32 (Only after AUTHORIZE FLASH)
pio run -t upload --upload-port COM_PORT

# 3. Serial Monitor for boot logs (115,200 baud)
pio device monitor -b 115200
```

---

## 6. Two-Stage Hardware Enabling Protocol

```
Stage F0 (Sensors Only — Default Baseline)
• Actuators compiled disabled in firmware (ACTUATORS_ENABLED = 0)
• HTTP commands return HTTP 403 ERR_ACTUATOR_DISABLED
• Environmental sensors and SoftAP fully operational
                 |
                 | 1. Bench Electrical Sign-Off Completed
                 | 2. User confirms: "WIRING CONFIRMED"
                 v
Stage F1 (Sensors + Verified Live Actuators)
• TB6612FNG differential drive active with 250ms deadman
• Kinematic servos active with mechanical limits
• Submersible pump active with 10s auto-cutoff
• Emergency Stop active with latching cutoff
```

---

## 7. Protocol Reference (v1.0.0)

| Endpoint | Method | Headers | Description |
|---|---|---|---|
| `/api/v1/capabilities` | GET | `X-AgriRover-Token` | Hardware capabilities, active firmware stage, supported features (serves as connection heartbeat) |
| `/api/v1/telemetry` | GET | `X-AgriRover-Token` | Fresh sensor telemetry packet with monotonic sequence (DHT22, Dual Moisture, pH, Ultrasonic) |
| `/api/v1/command` | POST | `X-AgriRover-Token` | Actuator command dispatch (drive, servo, pump) |
| `/api/v1/emergency-stop` | POST | None (or optional `X-AgriRover-Token`) | Unconditional Emergency Stop trigger; immediately halts locomotion, de-energizes pump relay, latches safe state. Throttled token-free UART log. |
| `/api/v1/emergency-stop/reset` | POST | `X-AgriRover-Token` (Required) | Authenticated E-Stop reset; re-arms actuator engine after deliberate verification; leaves all outputs de-energized. |

---

## 8. Physical Feedback Honesty Rules

In compliance with Track H Section 8 counting rules:
- **Open-Loop Actuators:** BO geared motors, hobby servos, and the DC relay pump have no closed-loop encoders or pressure sensors. They strictly report `PhysicalFeedbackState.NotAvailable` with explicit honesty warnings.
- **Buffer Calibration:** Soil pH probe readings are uncalibrated raw millivolts until two-point calibration using standard buffer solutions (pH 4.01 / 6.86) is performed.
- **Two-Point Moisture Calibration:** Capacitive soil probes require dry-air and submerged-water calibration to convert raw analog counts to percentage volumetric water content.
- **Flashing Tool Distinction:** `ESP32-CAM-MB` is cataloged strictly as a programming adapter, not an onboard rover runtime module.
