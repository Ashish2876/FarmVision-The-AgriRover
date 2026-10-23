# FarmVision AgriRover — Hardware Inventory & Configuration Data Model

**Document Version:** 1.0.0 (Phase 4 Foundation)  
**Status:** DRAFT / PROVISIONAL (Software domain representation; NOT validated electrical or wiring specifications)  
**Target Controllers:** ESP32 DevKit V1 (Main Telemetry & Actuation) + ESP32-CAM (Vision)  

---

> [!WARNING]
> **DRAFT HARDWARE SPECIFICATION ONLY**  
> All component definitions, pin assignments, and configurations documented herein are **software data models** representing the intended rover inventory. They have **NOT** been verified by physical hardware testing. Zero pin numbers or electrical voltage/current ratings are asserted as verified. Hardware configuration status remains `NotConfigured` or `Draft` across all items.

---

## 1. Actual Rover Hardware Inventory (Source of Truth)

The software component catalog is seeded directly from the master 23-item inventory:

| Item # | Category | Component Name | Qty | Model / Designation | Intended Role | Configuration Status |
|:---:|---|---|:---:|---|---|:---:|
| 1 | Controller | **ESP32 DevKit V1** | 1 | ESP32-WROOM-32 / DevKit V1 (30-pin) | Main controller: sensors, motors, servos, irrigation | `Draft` |
| 2 | Camera | **ESP32-CAM (OV2640)** | 1 | AI-Thinker ESP32-CAM with OV2640 2MP | Camera module (separate controller) | `Draft` |
| 3 | Programming tool | **ESP32-CAM-MB USB Programmer** | 1 | Micro-USB Daughterboard (CH340G) | Flashing the ESP32-CAM; not a runtime component | `Draft` |
| 4 | Sensor | **Capacitive Soil Moisture Sensor** | 2 | Capacitive Probe v1.2 / v2.0 | Soil moisture (two distinct probes: surface & deep) | `Draft` |
| 5 | Sensor | **DHT22** | 1 | Aosong AM2302 Digital Sensor | Air temperature and humidity (one component, two readings) | `Draft` |
| 6 | Sensor | **Soil pH Sensor Kit** | 1 | Analog Soil pH Probe & Signal Board | Soil pH measurement | `Draft` |
| 7 | Sensor | **HC-SR04 Ultrasonic Sensor** | 1 | 5V Sonar Transceiver Module | Distance / obstacle detection | `Draft` |
| 8 | Motor driver | **TB6612FNG Dual Motor Driver** | 1 | Dual MOSFET H-Bridge Breakout | Drives the four drive motors | `Draft` |
| 9 | Motors | **BO Geared DC Motor with Wheel** | 4 | Yellow BO 3-6V TT Gearbox Motor + Wheel | Locomotion drive motors | `Draft` |
| 10 | Servo | **SG90 Micro Servo (Camera Movement)** | 1 | TowerPro SG90 9g Micro Servo | Camera movement (movement axis unconfirmed) | `Draft` |
| 11 | Servo | **MG90S Metal Gear Micro Servo** | 1 | TowerPro MG90S 9g Metal Gear Servo | Soil probe positioning | `Draft` |
| 12 | Servo | **SG90 Micro Servo (Ultrasonic Scanning)** | 1 | TowerPro SG90 9g Micro Servo | Ultrasonic sensor scanning | `Draft` |
| 13 | Servo hardware | **Servo Mounting Brackets & Horns** | *Unspecified* | Plastic & Metal Horns / Linkages | Mechanical mounting and horns | `Draft` |
| 14 | Irrigation | **5–6 V Mini DC Water Pump** | 1 | Submersible Mini 5-6V DC Pump | Fluid delivery / watering | `Draft` |
| 15 | Irrigation | **2-Channel Relay Module** | 1 | 5V Optocoupler Isolated Relay Board | Switching pump (Channel 1) & spare (Channel 2) | `Draft` |
| 16 | Irrigation | **Water Tubing** | *Unspecified* | Flexible Silicone / PVC Drip Tubing | Plumbing fluid delivery line | `Draft` |
| 17 | Irrigation | **Water Reservoir** | 1 | Onboard Fluid Storage Container | Water storage tank | `Draft` |
| 18 | Power | **Protected Battery Pack** | 1 | Rechargeable Li-Ion Pack with BMS | Main power source | `Draft` |
| 19 | Power | **LM2596 DC-DC Buck Converter** | 1 | Step-Down Adjustable DC-DC Regulator | Voltage regulation to 5V rail | `Draft` |
| 20 | Power | **Main Power Switch** | 1 | High-Current SPST Physical Switch | Master electrical power cutoff | `Draft` |
| 21 | Power | **Inline Fuse & Fuse Holder** | *Unspecified* | Inline Blade / Glass Fuse Holder | Overcurrent protection | `Draft` |
| 22 | Assembly | **Jumper Wires, Brackets, Screws, Nuts, Ties** | *Unspecified* | M3 Hardware, DuPont Wires, Zip Ties | Mechanical assembly & electrical interconnect | `Draft` |
| 23 | Assembly | **Cardboard Chassis** | 1 | Laser/Hand-Cut Structural Frame | Body frame and mounting chassis | `Draft` |

*Quantities listed as "Unspecified" are strictly stored as `null` in the domain data model. No quantities, current ratings, or electrical limits are guessed.*

---

## 2. Inventory Discrepancies & Resolutions (Section 7)

1. **Light Intensity Reading:**
   - **Discrepancy:** The application UI displays a solar light reading (`48,500 lux`), but the physical inventory contains **no light sensor** (e.g. no BH1750 or LDR).
   - **Resolution:** The UI card and reading type remain active for presentation continuity. In the hardware model, it is recorded that no light sensor exists in the inventory; the reading is marked as purely simulated (`DataOrigin.Simulated`) with no physical hardware source.
2. **Battery Voltage / Percentage Sensing:**
   - **Discrepancy:** The UI displays battery telemetry (`84% 12.4V`), but the inventory contains **no battery-voltage sensing component** (no analog voltage divider or I2C fuel gauge).
   - **Resolution:** The UI HUD continues to display simulated battery metrics. The power configuration model records `hasHardwareVoltageDivider = false`, noting that real hardware battery telemetry cannot be gathered without physical sensing additions.
3. **Soil pH & Ultrasonic Distance Readings:**
   - **Discrepancy:** Present in the physical inventory (pH Kit and HC-SR04), but absent from current UI telemetry cards.
   - **Resolution:** Domain types `SensorType.SoilPh` (0.0–14.0 pH) and `SensorType.UltrasonicDistance` (2.0–400.0 cm) are added with full range validators and mock generation. No UI cards were added or removed in Phase 4 (reserved for future phases).
4. **Dual Capacitive Soil Moisture Probes:**
   - **Discrepancy:** The inventory contains two distinct capacitive moisture probes, whereas the app displays a single rootzone moisture percentage.
   - **Resolution:** Both probes are modeled independently (`sensor-soil-moisture-1` and `sensor-soil-moisture-2`). The domain layer provides `SoilMoistureDualProbeMapping` (60% surface probe + 40% deep substrate probe) to supply the legacy single-value field without hiding the multi-sensor reality.
5. **DHT22 Dual Telemetry:**
   - **Discrepancy:** Separate Temperature and Humidity readings appear in the UI.
   - **Resolution:** Correctly modeled as **one** physical hardware sensor supporting two canonical reading types (`AirTemperature` and `Humidity`).

---

## 3. Provisional Architecture Assumptions (To Be Verified by Hardware Team)

1. **Four Drive Motors on TB6612FNG:**
   - The inventory lists 4 BO geared DC motors and 1 dual-channel TB6612FNG motor driver.
   - **Provisional Software Model:** Motors are grouped into two parallel pairs:
     - Left Channel (A): Front Left + Rear Left motors.
     - Right Channel (B): Front Right + Rear Right motors.
   - **Item for Hardware Team:** The hardware team must verify total motor stall current versus the 1.2A continuous / 3.2A peak rating of the TB6612FNG to ensure the driver does not overheat or trip thermal protection.
2. **Camera Servo Axis:**
   - The inventory includes 1x SG90 servo for camera movement.
   - **Provisional Software Model:** Modeled as single-axis tilt actuation (`isDualAxis = false`). Pan-and-tilt capability is **not** claimed.
3. **ESP32 ADC Line & Wi-Fi Conflict Note:**
   - Analog sensors (2x Soil Moisture probes, Soil pH sensor) connect to the ESP32 SAR ADC.
   - **Hardware Team Warning:** In the ESP32 architecture, **ADC2 channels cannot be used when Wi-Fi is actively transmitting**. All analog sensors must be assigned strictly to **ADC1 pins** (GPIO 32, 33, 34, 35, 36, 39).
4. **HC-SR04 5V Logic Level Warning:**
   - The HC-SR04 operates at 5V and outputs a 5V logic high on its Echo pin.
   - Connecting Echo directly to a 3.3V ESP32 GPIO will damage the microcontroller. A voltage divider (e.g. 1kΩ / 2kΩ) or bidirectional logic level shifter must be placed on the Echo line.
