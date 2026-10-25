# FarmVision AgriRover — Microcontroller Firmware Guide

**Firmware Version:** `v1.0.0-f0-sensors`  
**Current Stage:** `Stage F0` (Sensors Only — Actuators Disabled)  
**Target Hardware:** ESP32 DevKit V1 (30-pin / 36-pin NodeMCU variant)  
**Safety Gate:** Physical flashing requires explicit command `AUTHORIZE FLASH`.

---

## 1. Overview & FreeRTOS Architecture

The FarmVision AgriRover main firmware coordinates environmental telemetry acquisition, real-time safety watchdogs, and authenticated REST communications over a local Wi-Fi SoftAP.

To prevent telemetry stalls or network contention from blocking safety-critical loops, tasks are partitioned across the ESP32's dual cores using FreeRTOS:

```
+-------------------------------------------------------------+
|                     ESP32 DevKit V1                         |
+------------------------------+------------------------------+
|            CORE 0            |            CORE 1            |
+------------------------------+------------------------------+
| • Wi-Fi SoftAP Stack         | • TaskSensing (10 Hz)        |
| • TaskComm (50 Hz)           |   - Soil Moisture ADC        |
|   - WebServer REST Routes    |   - DHT22 Temp / Humidity    |
|   - Auth Token Validation    |   - Analog Soil pH ADC       |
|   - JSON Serialization       |   - Ultrasonic Pulse Timing  |
|                              | • TaskSafetyWatchdog (20 Hz) |
|                              |   - 250ms Deadman Timer      |
|                              |   - Emergency Stop Latching  |
|                              |   - Hardware Cutoff Core     |
+------------------------------+------------------------------+
```

---

## 2. Wi-Fi SoftAP Network Configuration

The microcontroller broadcasts an isolated, high-speed 802.11b/g/n SoftAP:
- **SSID:** `AgriRover-AP`
- **Default Password:** `AgriRover2026`
- **Static IP Address:** `192.168.4.1`
- **Subnet Mask:** `255.255.255.0`
- **HTTP Port:** `80`
- **Default Authentication Token:** `rover-token-secret` (passed via `X-AgriRover-Token` header)

---

## 3. Candidate Pin Assignments & Electrical Checklist

*All pin assignments below match [`docs/hardware-wiring-checklist.md`](../docs/hardware-wiring-checklist.md) and remain tagged as draft until verified on physical rover hardware.*

### 3.1 Environmental Sensors (Active in Stage F0)
| Sensor | Signal | ESP32 GPIO | Electrical Requirements |
|---|---|---|---|
| **Capacitive Soil Moisture** | Probe 1 Analog | **GPIO 32** | ADC1 Channel 4 (3.3V safe) |
| | Probe 2 Analog | **GPIO 34** | ADC1 Channel 6 (Input only) |
| **DHT22** | Data | **GPIO 4** | 4.7kΩ pull-up to 3.3V |
| **Analog Soil pH** | Analog Voltage | **GPIO 35** | ADC1 Channel 7 (Input only) |
| **HC-SR04 Ultrasonic** | Trigger | **GPIO 13** | 3.3V pulse output |
| | Echo | **GPIO 14** | **Requires 1kΩ / 2kΩ voltage divider (5V -> 3.3V)!** |

### 3.2 Actuation Electronics (DISABLED in Stage F0)
> [!IMPORTANT]
> In Stage F0, all motor PWM, direction pins, servo lines, and pump relays are compiled in the disabled state (`ACTUATORS_ENABLED = 0`). If commanded over HTTP, the firmware returns `ERR_ACTUATOR_DISABLED`.

| Actuator | Signal | ESP32 GPIO | Status in F0 |
|---|---|---|---|
| **TB6612FNG Driver** | PWMA (Left PWM) | GPIO 25 | **DISABLED (Low)** |
| | AIN1, AIN2 | GPIO 26, 27 | **DISABLED (Low)** |
| | PWMB (Right PWM) | GPIO 16 | **DISABLED (Low)** |
| | BIN1, BIN2 | GPIO 17, 18 | **DISABLED (Low)** |
| | STBY | GPIO 19 | **DISABLED (Low)** |
| **Camera Tilt Servo** | PWM Signal | GPIO 21 | **DISABLED (Low)** |
| **Soil Probe Servo** | PWM Signal | GPIO 22 | **DISABLED (Low)** |
| **Ultrasonic Servo** | PWM Signal | GPIO 23 | **DISABLED (Low)** |
| **Irrigation Relay** | Pump Relay Ch 1 | GPIO 5 | **DISABLED (High / Cutoff)** |

---

## 4. Building the Firmware

### Option A: PlatformIO (Recommended)
1. Install [PlatformIO Core](https://platformio.org/) or the PlatformIO IDE extension in VS Code / Antigravity.
2. Open terminal in `firmware/esp32-main/`:
   ```bash
   cd firmware/esp32-main
   pio run
   ```
3. PlatformIO automatically downloads the Espressif32 platform toolchain, Arduino framework, `ArduinoJson`, and `DHT sensor library`.

### Option B: Arduino IDE 2.x
1. Install **ESP32 by Espressif Systems** via Board Manager (version $\ge 2.0.14$).
2. Install the following libraries via Library Manager:
   - `ArduinoJson` (v7.x by Benoit Blanchon)
   - `DHT sensor library` (by Adafruit)
   - `Adafruit Unified Sensor` (by Adafruit)
3. Select Board: **DOIT ESP32 DEVKIT V1**.
4. Open `firmware/esp32-main/src/main.cpp` and compile.

---

## 5. Physical Flashing Gate Protocol

> [!CAUTION]
> **FLASH AUTHORIZATION GATE:**
> The AI assistant is strictly prohibited from executing serial flashing (`pio run -t upload` or `esptool.py`) without the user's explicit authorization command:
> ```text
> AUTHORIZE FLASH
> ```
> Before granting flash authorization, ensure:
> 1. Battery positive lead is disconnected or physical kill-switch is OPEN.
> 2. HC-SR04 Echo voltage divider is verified with multimeter/oscilloscope.
> 3. LM2596 buck converter output is measured at 5.05V ± 0.05V before plugging into ESP32 VIN.
