# FarmVision — Hardware Integration & Handover Specification

**Target Audience:** AgriRover Hardware, Robotics, & Embedded Firmware Engineering Teams  
**Safety Status:** `ACTUATORS_ENABLED = 0` (Confirmed Software Interlock)  
**Main Microcontroller:** Espressif ESP32 DevKit V1  
**Vision Subsystem:** ESP32-CAM  

---

## 1. Hardware Boundary & Scope

The current software distribution is delivered with **physical actuators strictly disabled**:
- `ACTUATORS_ENABLED = 0`
- No real DC motors, stepper motors, solenoid valves, or chemical pumps are actuated.
- All driving, joystick, and actuation controls in the Android UI operate in **high-fidelity software simulation mode**.

---

## 2. Rover Network & Communication Protocols

### Wi-Fi SoftAP Configuration
- **SSID:** `AgriRover-AP`
- **Password:** `FarmVision2026`
- **Subnet:** `192.168.4.0/24`
- **Main ESP32 IP:** `192.168.4.1` (Port 80)
- **ESP32-CAM IP:** `192.168.4.2` (Port 81)

### Telemetry JSON Specification (`GET /api/v1/telemetry`)
```json
{
  "rover_id": "AgriRover-01",
  "battery_pct": 84,
  "battery_voltage": 12.4,
  "drive_mode": "MANUAL",
  "speed_kmh": 0.0,
  "motor_temp_c": 34.2,
  "air_temp_c": 26.8,
  "air_humidity_pct": 62.3,
  "soil_moisture_raw_adc": 2180,
  "soil_ph_raw": 6.8,
  "timestamp_epoch_ms": 1730592000000
}
```

---

## 3. Sensor Honesty Guidelines for Hardware Teams

1. **Capacitive Soil Moisture:** Plotted as raw 12-bit ADC counts (`0–4095 counts`). DO NOT convert directly to percentage without field soil calibration curves.
2. **Soil pH:** Plotted as raw uncalibrated analog probe values.
3. **Actuator Arming:** Before enabling physical hardware in `firmware/esp32-main/src/main.cpp` (`ACTUATORS_ENABLED = 1`), ensure mechanical E-stop hardware relays are verified.

