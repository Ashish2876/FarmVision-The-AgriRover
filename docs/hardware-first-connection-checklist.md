# FarmVision AgriRover — Hardware First-Connection & Commissioning Checklist

**Document Version:** 1.0.0  
**Status:** DRAFT / BENCH COMMISSIONING SPECIFICATION (Requires physical operator execution)  
**Safety Gate:** Physical bring-up checklist must be executed on a test bench before field deployment.

---

## 1. Safety Invariants & Pre-Power Verification

> [!CAUTION]
> **CRITICAL HARDWARE SAFETY RULES:**
> 1. **Elevated Stand Only:** During initial electrical bring-up, the rover chassis MUST be placed on an elevated workbench stand so all four drive wheels rotate freely in the air without contact with any surface.
> 2. **Physical Kill-Switch Primacy:** The high-current mechanical emergency cutoff switch is the primary safety disconnect. It must be within immediate reach of the testing engineer at all times during powered bench tests.
> 3. **Actuator Safe State:** Firmware must maintain `ACTUATORS_ENABLED = 0` until power rails and logic levels have passed all voltage tests.
> 4. **No Direct 5V Signals to ESP32:** HC-SR04 Echo outputs 5V. Connecting directly to ESP32 destroys the input buffer. Level shifting (1kΩ/2kΩ resistive divider or logic level converter) is mandatory.

---

## 2. Step-by-Step Bring-Up Procedure

### Step 1: Pre-Power Visual & Continuity Inspection (Power Disconnected)

- [ ] **1.1 Battery Disconnected:** Ensure the 12V Li-ion battery pack is completely disconnected and the mechanical kill-switch is OPEN (OFF).
- [ ] **1.2 Common Ground (Star Topology):** Verify unified electrical ground (GND) across all subsystems using a digital multimeter in continuity mode:
  - Battery negative lead
  - LM2596 DC-DC step-down buck converter GND
  - ESP32 DevKit V1 GND
  - ESP32-CAM GND
  - TB6612FNG dual motor driver GND
  - 2-channel relay module GND
  - All sensor GND connections
- [ ] **1.3 Short-Circuit Pre-Check:** Measure resistance between power rails and GND:
  - 12V rail to GND: must be $>10\text{ k}\Omega$ (no direct short).
  - 5V rail to GND: must be $>1\text{ k}\Omega$.
  - 3.3V rail to GND: must be $>500\ \Omega$.

---

### Step 2: Voltage Rail Pre-Check (Without Microcontrollers Plugged In)

*Do not install or power microcontrollers or sensitive sensor boards during this step.*

- [ ] **2.1 Connect 12V Battery:** Connect the 12V battery through the inline fuse (3A–5A fast-blow) and mechanical kill-switch.
- [ ] **2.2 Measure Battery Rail Voltage:** Close the kill-switch and measure voltage across the raw 12V rail:
  - Expected: **11.1V to 12.6V** (record exact value: `______ V`).
- [ ] **2.3 Calibrate LM2596 Buck Converter:**
  - Measure output voltage of the LM2596 module with a calibrated multimeter.
  - Adjust multi-turn potentiometer trimpot until the output reads strictly **5.05V ± 0.05V**.
  - Verify output stability under light load.
- [ ] **2.4 Measure Motor Driver VM Rail:**
  - Verify VM input on the TB6612FNG socket reads unregulated battery voltage (11.1V–12.6V).
- [ ] **2.5 Power Down:** Open the mechanical kill-switch. Disconnect battery.

---

### Step 3: Microcontroller Installation & SoftAP Connectivity

- [ ] **3.1 Install ESP32 DevKit V1:** Seat the microcontroller into its socket/breadboard with `ACTUATORS_ENABLED = 0` configured in firmware.
- [ ] **3.2 Power On:** Close the kill-switch. Confirm the ESP32 red power LED illuminates steadily.
- [ ] **3.3 Verify Boot Log:** Connect USB serial monitor (115,200 baud) and confirm clean boot:
  - No brownout detector resets.
  - SoftAP initialized with SSID `AgriRover-AP`.
  - Web server listening on port 80.
- [ ] **3.4 Android Device SoftAP Association:**
  - Open Wi-Fi settings on the Android test device.
  - Scan for and associate with `AgriRover-AP` (enter pre-shared WPA2 key).
  - Verify Android assigns an IP in the subnet (typically `192.168.4.2`).
- [ ] **3.5 Network Reachability:**
  - Execute ping from Android terminal or test suite: `ping 192.168.4.1`.
  - Confirm round-trip latency $<10\text{ ms}$.
- [ ] **3.6 Telemetry Endpoint Verification:**
  - In Android browser or test client, fetch `http://192.168.4.1/api/v1/telemetry`.
  - Confirm HTTP 200 OK and valid JSON envelope containing `sequence_number`, `uptime_ms`, `estop_engaged: false`, and `readings`.

---

### Step 4: Sensor Signal & Voltage Level Verification

- [ ] **4.1 HC-SR04 Echo Level Shifter:**
  - Connect oscilloscope or multimeter peak-detect to the voltage divider output going to ESP32 GPIO 14.
  - Trigger ultrasonic pulses and verify the Echo pulse voltage never exceeds **3.30V** (typically ~3.1V–3.3V).
- [ ] **4.2 Capacitive Soil Moisture Probes (GPIO 32, GPIO 34):**
  - Verify VCC rail to probes is powered from the **3.3V** regulated supply.
  - Measure analog output voltage:
    - In dry air: ~2.4V–3.0V (raw ADC counts ~2,800–3,300).
    - In tap water: ~1.0V–1.5V (raw ADC counts ~1,200–1,800).
  - Verify Probe 2 (GPIO 34) is NOT saturated: raw ADC reading must remain strictly between **51 and 4049 counts**.
- [ ] **4.3 DHT22 Sensor (GPIO 4):**
  - Verify 4.7kΩ pull-up resistor between GPIO 4 and 3.3V rail.
  - Verify telemetry payload reports plausible temperature (15.0°C–35.0°C) and humidity (20.0%–90.0%).
- [ ] **4.4 Soil pH Sensor (GPIO 35):**
  - Measure analog output voltage with pH 7.0 buffer solution: verify output is within 0.0V–3.3V range.

---

### Step 5: Dry-Wheel Locomotion Test (Elevated Stand Only)

*Perform only after passing Steps 1–4. Chassis wheels must be off the ground.*

- [ ] **5.1 Enable Actuator Mode:** In firmware, set test mode with speed clamping to Low preset (duty cycle $\le 30\%$).
- [ ] **5.2 Forward Drive Command:**
  - In Android app Rover Controller, tap FORWARD (or drag joystick forward).
  - Verify all 4 wheels rotate forward.
  - Confirm TB6612FNG STBY pin is HIGH, AIN1=HIGH, AIN2=LOW, BIN1=HIGH, BIN2=LOW.
- [ ] **5.3 Reverse Drive Command:**
  - Tap REVERSE. Confirm all 4 wheels rotate in reverse.
- [ ] **5.4 Tank Steer Left:**
  - Tap TURN LEFT. Confirm left wheels rotate backward, right wheels rotate forward.
- [ ] **5.5 Tank Steer Right:**
  - Tap TURN RIGHT. Confirm left wheels rotate forward, right wheels rotate backward.
- [ ] **5.6 Center STOP:**
  - Release joystick / tap STOP. Confirm all 4 motors brake and come to an immediate complete stop within $<50\text{ ms}$.

---

### Step 6: Emergency Stop & Fail-Safe Cutoff Verification

- [ ] **6.1 In-Motion E-Stop Trigger:**
  - While motors are spinning forward on the bench, tap the red **EMERGENCY STOP** button in the Android app.
  - Verify within $<50\text{ ms}$:
    - All motor PWM signals immediately drop to 0% duty cycle.
    - TB6612FNG STBY pin drops to LOW (high-impedance motor coast/brake).
    - Irrigation relay immediately de-energizes.
    - Rover telemetry reports `estop_engaged: true` and `rover_mode: "E-STOPPED"`.
- [ ] **6.2 Command Rejection During E-Stop:**
  - Attempt to send Forward, Reverse, Turn, or Pump actuation commands while E-stop is latched.
  - Verify all motion commands are rejected (HTTP 423 or ignored); motors remain completely motionless.
- [ ] **6.3 Authenticated Reset Verification:**
  - Send unauthenticated reset request: verify rejected with HTTP 401.
  - Provide valid Bearer token and execute deliberate E-Stop Reset.
  - Confirm response reasonCode: `"ESTOP_RESET"`.
  - **Critical Check:** Confirm that upon reset, all motors, servos, and pumps **remain fully OFF** until a fresh explicit command is dispatched.
- [ ] **6.4 Mechanical Kill-Switch Test:**
  - While motors are spinning, toggle the mechanical kill-switch to OFF.
  - Verify instantaneous electrical disconnection of the battery rail.

---

## 3. Commissioning Verification Sign-Off Sheet

| Test Item | Specification | Measured / Observed Value | Pass / Fail |
|---|---|---|---|
| Raw Battery Voltage | 11.1V – 12.6V | `______ V` | [ ] Pass [ ] Fail |
| Buck Converter Output | 5.00V – 5.10V | `______ V` | [ ] Pass [ ] Fail |
| Ground Loop Continuity | $< 0.5\ \Omega$ between all GNDs | `______ Ω` | [ ] Pass [ ] Fail |
| HC-SR04 Echo Max Voltage | $\le 3.30\text{ V}$ | `______ V` | [ ] Pass [ ] Fail |
| Probe 1 ADC Range (Air / Water) | ~3000 / ~1400 counts | `____ / ____` | [ ] Pass [ ] Fail |
| Probe 2 ADC Counts (Uncalibrated) | 51 to 4049 counts (unsat) | `______ counts` | [ ] Pass [ ] Fail |
| DHT22 Telemetry | Plausible ambient values | `____ °C / ____ %` | [ ] Pass [ ] Fail |
| Dry-Wheel 4-Direction Motion | Correct rotation directions | Observed | [ ] Pass [ ] Fail |
| Android E-Stop Latency | $< 50\text{ ms}$ actuator cutoff | `______ ms` | [ ] Pass [ ] Fail |
| Post-Reset Zero-Actuation | All outputs OFF after reset | Confirmed OFF | [ ] Pass [ ] Fail |
| Mechanical Kill-Switch | Instant total power break | Confirmed | [ ] Pass [ ] Fail |

---

### Operator Sign-off

- **Testing Engineer Name:** `________________________________________`
- **Multimeter Serial Number / Model:** `________________________________`
- **Date & Time of Test:** `____________________`
- **Overall Commissioning Status:** [ ] PASSED FOR FIELD TRIALS  [ ] REJECTED / RE-TEST REQUIRED
- **Signature:** `________________________________________`
