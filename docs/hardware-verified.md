# FarmVision AgriRover — Physical Hardware Verification & Sign-Off Guide

**Document Version:** 1.0.0  
**Status:** BENCH VERIFICATION & ELECTRICAL SIGN-OFF SPECIFICATION  
**Safety Gate:** `WIRING CONFIRMED` is strictly required before enabling live actuators (Stage F1).  
**Flashing Gate:** `AUTHORIZE FLASH` is strictly required before flashing firmware to microcontrollers.

---

## 1. Safety Invariants & Bench Testing Rules

> [!CAUTION]
> **MANDATORY BENCH TEST INVARIANT — WHEELS OFF THE GROUND:**
> During all initial actuation and electrical verification tests, the rover chassis must be placed on an elevated mechanical stand with all four wheels completely suspended in the air. Never power the motor drivers with the rover resting on the floor or work table.

### Primary Physical Controls:
1. **Mechanical Emergency Cutoff:** An emergency knife switch or high-current toggle must be wired directly in series with the positive lead of the 12V Li-ion battery pack. Software Emergency Stop is an operational convenience, not an electrical disconnect.
2. **Logic Level Shifting:** The HC-SR04 Echo output (5.0V) must pass through a 1kΩ / 2kΩ resistive voltage divider (or bidirectional level shifter). Supplying 5.0V logic into ESP32 GPIO 14 will destroy the internal input buffer.
3. **Common Ground Reference:** The battery negative, buck converter ground, ESP32 ground, ESP32-CAM ground, and TB6612FNG ground must be bonded together into a star ground point.

---

## 2. Electrical Verification Sign-Off Checklist

Before granting `WIRING CONFIRMED`, the hardware engineering team must physically test, verify, and check off every item:

### 2.1 Power Distribution & Voltage Rails
- [ ] **Battery Pack Output:** 12V 3S Li-ion battery measured between 11.1V (nominal) and 12.6V (fully charged) with digital multimeter.
- [ ] **Main Kill Switch:** Verified to break the battery positive circuit cleanly under load.
- [ ] **LM2596 Buck Converter:** Calibrated with precision trim potentiometer to **5.05V ± 0.05V** before connecting to ESP32 VIN or servo rails.
- [ ] **Ground Continuity:** Star ground point verified with $\le 0.1\Omega$ resistance between all subsystem ground pins.

### 2.2 Environmental Sensors
- [ ] **HC-SR04 Ultrasonic Echo Divider:** Oscilloscope or multimeter confirms Echo signal peak voltage $\le 3.3\text{V}$ during active pinging.
- [ ] **Capacitive Soil Moisture Probes:** Powered strictly from 3.3V rail. Output voltage stays within $0.0\text{V} - 3.0\text{V}$ between dry air and submerged states.
- [ ] **DHT22 Atmospheric Sensor:** 4.7kΩ pull-up resistor verified between Data pin (GPIO 4) and 3.3V rail.
- [ ] **Soil pH Electrode:** BNC signal conditioning board outputs $0.0\text{V} - 3.3\text{V}$ across buffer calibration solutions (pH 4.01 and pH 6.86).

### 2.3 Locomotion & Kinematic Actuators
- [ ] **TB6612FNG Standby (STBY):** Verified to hold motor driver in high-Z sleep when GPIO 19 is LOW.
- [ ] **Differential BO Motors:** Wheels rotate in correct directions (Throttle $> 0 \implies$ forward; Steering $> 0 \implies$ clockwise yaw pivot).
- [ ] **Slew Rate Limiting:** Motor acceleration is smoothed to prevent instantaneous inrush current and battery voltage sag.
- [ ] **250 ms Deadman Failsafe:** Turning off Wi-Fi or pausing command packets halts locomotion within 250 ms.
- [ ] **Kinematic Servos:** Mechanical linkages do not hit physical hard stops at 0° or 180° limits.
- [ ] **Irrigation Pump Relay:** Active LOW relay opens contacts (cuts pump power) after exactly 10,000 ms (10s) maximum duration.

---

## 3. Two-Stage Safety Gating Protocol

```
+-----------------------------------------------------------------+
| Stage F0 (Sensors Only — Current Baseline)                       |
| • Actuators compiled disabled in firmware (ACTUATORS_ENABLED = 0) |
| • HTTP commands return ERR_ACTUATOR_DISABLED                     |
| • Environmental sensors and camera stream fully operational      |
+-----------------------------------------------------------------+
                                 |
                                 | Hardware Team signs off Checklist
                                 | User inputs: "WIRING CONFIRMED"
                                 v
+-----------------------------------------------------------------+
| Stage F1 (Sensors + Verified Live Actuators)                     |
| • TB6612FNG differential drive active with 250ms deadman         |
| • Kinematic servos active with mechanical limits                 |
| • Submersible pump active with 10s auto-cutoff                   |
| • Software Emergency Stop active with latching core              |
+-----------------------------------------------------------------+
```

---

## 4. Hardware Verification Sign-Off Record

| Subsystem | Verified By | Date | Multimeter / Scope Evidence | Verdict |
|---|---|---|---|---|
| **12V Li-ion Battery & Kill Switch** | Hardware Team | Pending | Measured: ______ V | `DRAFT / UNVERIFIED` |
| **LM2596 5V Buck Converter** | Hardware Team | Pending | Measured: ______ V | `DRAFT / UNVERIFIED` |
| **HC-SR04 Echo Level Shifter** | Hardware Team | Pending | Measured: ______ V | `DRAFT / UNVERIFIED` |
| **TB6612FNG Motor Driver** | Hardware Team | Pending | Bench test wheels off ground | `DRAFT / UNVERIFIED` |
| **Kinematic Servos (Cam/Probe/US)**| Hardware Team | Pending | Mechanical travel verified | `DRAFT / UNVERIFIED` |
| **Submersible Pump & 10s Cutoff** | Hardware Team | Pending | Auto-cutoff timer verified | `DRAFT / UNVERIFIED` |
