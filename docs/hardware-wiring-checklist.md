# FarmVision AgriRover — Hardware Wiring & Electrical Verification Checklist

**Document Version:** 1.0.0  
**Status:** DRAFT / PURE SPECIFICATION (Engineering Review Pending)  
**Safety Gate:** `WIRING CONFIRMED` required before any live motor, servo, or pump actuation (Milestone H6).

---

## 1. Physical Safety Warnings & Invariants

> [!CAUTION]
> **PHYSICAL SAFETY INVARIANTS — READ BEFORE POWERING THE ROVER:**
> 1. **No Live Actuation Without Confirmation:** Locomotion motors, kinematic servos, and irrigation relays must remain disabled in firmware (F0 Stage) until physical wiring is verified and the user grants `WIRING CONFIRMED`.
> 2. **Physical Kill-Switch Required:** A physical high-current emergency cutoff switch must be wired in series with the positive battery lead (`12V Li-ion`) before the buck converter and motor drivers. Software Emergency Stop is an operational convenience, not a substitute for an electrical cutoff.
> 3. **Level Shifting on HC-SR04 Echo:** The HC-SR04 Ultrasonic Sensor operates on 5.0V. Its `Echo` pin outputs 5.0V logic. Connecting this pin directly to an ESP32 GPIO (maximum 3.3V) risks permanent chip destruction. A 1kΩ / 2kΩ resistive voltage divider (or bidirectional logic level shifter) is **mandatory**.
> 4. **Strapping & SPI Flash Pin Restrictions:** GPIO 0, 2, 5, 12, and 15 are ESP32 boot strapping pins. GPIO 6–11 are connected directly to integrated SPI flash memory. These pins require extreme caution: GPIO 0, 2, 12, and 15 must never be used for actuator driving due to boot-mode failure risks. GPIO 5 is a boot strapping pin (`VSPI CS0`) that outputs PWM during boot and has an internal pull-up; its assignment to an active-low relay is strictly `DRAFT / UNVERIFIED` until physical bench verification of the relay input circuit.

---

## 2. Candidate Draft Pin Assignments (DRAFT / UNVERIFIED)

*All pin assignments below are engineering draft candidates and MUST be verified against physical rover wiring. Zero pins are physically verified.*

| Component | Signal Name | ESP32 DevKit V1 Pin | Electrical Constraints | Verification Status |
|---|---|---|---|---|
| **Soil Moisture Probes** | Probe 1 ADC | **GPIO 32** | ADC1 Channel 4 (3.3V max) | `DRAFT / UNVERIFIED` |
| | Probe 2 ADC | **GPIO 34** | ADC1 Channel 6 (Input only, 3.3V max, uncalibrated raw counts) | `DRAFT / UNVERIFIED` |
| **DHT22** | Single-Bus Data | **GPIO 4** | 4.7kΩ pull-up to 3.3V | `DRAFT / UNVERIFIED` |
| **Soil pH Sensor** | Analog Voltage | **GPIO 35** | ADC1 Channel 7 (Input only, 3.3V max) | `DRAFT / UNVERIFIED` |
| **HC-SR04 Ultrasonic** | Trigger Pin | **GPIO 13** | 3.3V Output pulse (10µs) | `DRAFT / UNVERIFIED` |
| | Echo Pin | **GPIO 14** | **Requires 5V -> 3.3V voltage divider!** | `DRAFT / UNVERIFIED` |
| **TB6612FNG Driver** | PWMA (Left Speed) | **GPIO 25** | Hardware PWM (LEDC channel 0) | `DRAFT / UNVERIFIED` |
| | AIN1 (Left Dir 1) | **GPIO 26** | Logic Level (3.3V) | `DRAFT / UNVERIFIED` |
| | AIN2 (Left Dir 2) | **GPIO 27** | Logic Level (3.3V) | `DRAFT / UNVERIFIED` |
| | PWMB (Right Speed)| **GPIO 16** | Hardware PWM (LEDC channel 1) | `DRAFT / UNVERIFIED` |
| | BIN1 (Right Dir 1)| **GPIO 17** | Logic Level (3.3V) | `DRAFT / UNVERIFIED` |
| | BIN2 (Right Dir 2)| **GPIO 18** | Logic Level (3.3V) | `DRAFT / UNVERIFIED` |
| | STBY (Standby) | **GPIO 19** | Active HIGH enable | `DRAFT / UNVERIFIED` |
| **Camera Tilt Servo** | PWM Signal | **GPIO 21** | 50 Hz PWM (LEDC channel 2) | `DRAFT / UNVERIFIED` |
| **Soil Probe Servo** | PWM Signal | **GPIO 22** | 50 Hz PWM (LEDC channel 3) | `DRAFT / UNVERIFIED` |
| **Ultrasonic Servo** | PWM Signal | **GPIO 23** | 50 Hz PWM (LEDC channel 4) | `DRAFT / UNVERIFIED` |
| **Irrigation Relay** | Channel 1 (Pump) | **GPIO 5** | Active LOW relay trigger (Boot strapping pin VSPI CS0; DRAFT / UNVERIFIED — see Section 5) | `DRAFT / UNVERIFIED` |
| | Channel 2 (Aux) | *Unassigned* | Spare auxiliary output | `UNASSIGNED` |

### 2.1 Pin Discrepancy Reconciliation History (Source of Truth: `config.h`)

| Subsystem | Signal | Canonical (`config.h`) | Older Handover Draft | Reconciliation Rationale |
|---|---|---|---|---|
| TB6612FNG | PWMB | **GPIO 16** | GPIO 14 | Avoids conflict with HC-SR04 Echo (GPIO 14) |
| TB6612FNG | BIN1 | **GPIO 17** | GPIO 12 | **Critical:** GPIO 12 is a boot strapping pin (MTDI; sets flash voltage). Reconciled to GPIO 17 to prevent flash corruption |
| TB6612FNG | BIN2 | **GPIO 18** | GPIO 13 | Avoids conflict with HC-SR04 Trig (GPIO 13) |
| HC-SR04 | TRIG | **GPIO 13** | GPIO 5 | Avoids strapping pin GPIO 5 |
| HC-SR04 | ECHO | **GPIO 14** | GPIO 18 | Avoids conflict with motor BIN2 (GPIO 18) |
| Soil Moisture | Probe 1 | **GPIO 32** | GPIO 34 | Assigned to ADC1_CH4 (Input/Output capable) |
| Soil Moisture | Probe 2 | **GPIO 34** | GPIO 35 | Assigned to ADC1_CH6 (Input Only, 3.3V safe) |
| Soil pH | AOUT | **GPIO 35** | GPIO 32 | Assigned to ADC1_CH7 (Input Only, 3.3V safe) |
| Relay | CH1 IN | **GPIO 5** | GPIO 15 | GPIO 15 is a boot strapping pin (MTDO; suppresses boot log). Reconciled to GPIO 5 (under review vs candidate GPIO 33) |

---

## 3. Physical Verification Checklist for the Hardware Team

Before authorizing live testing or flashing firmware, the hardware team must complete and sign off:

### 3.1 Power Supply & Rails
- [ ] 12V Li-ion battery pack output voltage verified between 11.1V (nominal) and 12.6V (fully charged).
- [ ] Main physical toggle/emergency kill-switch installed and breaks the battery positive circuit reliably.
- [ ] Inline fuse installed (recommended 3A–5A fast-blow) before the buck converter and motor drivers.
- [ ] LM2596 DC-DC step-down buck converter calibrated to **5.05V ± 0.05V** before connecting microcontrollers.
- [ ] Ground reference (GND) is unified between battery, buck converter, ESP32, ESP32-CAM, and motor driver.

### 3.2 Sensor Signals & Protection
- [ ] HC-SR04 Echo pin passes through a voltage divider (e.g. 1kΩ series + 2kΩ to ground) measuring ≤ 3.3V with oscilloscope or multimeter.
- [ ] Capacitive soil moisture probes VCC powered from 3.3V (to guarantee analog output does not exceed ADC limits).
- [ ] DHT22 data line has 4.7kΩ–10kΩ pull-up resistor to 3.3V rail.
- [ ] Soil pH analog output verified within 0.0V–3.3V range across test water samples.

### 3.3 Actuation & Mechanical
- [ ] TB6612FNG VM pin connected to unregulated 12V motor rail; VCC connected to regulated 5V or 3.3V logic rail.
- [ ] BO geared motors spin freely with no mechanical binding or chassis rub.
- [ ] Servos mounted with mechanical travel limits confirmed (avoid driving servos past mechanical stops).
- [ ] Submersible pump fluid lines clamped securely; relay contacts break pump power cleanly.

---

## 4. Two-Point Sensor Calibration Procedures

### 4.1 Capacitive Soil Moisture Probes
1. **Dry-Air Baseline (0% Volumetric Water Content):**
   - Suspend probe in clean, dry ambient air.
   - Record raw ADC reading: $ADC_{\text{dry}}$ (typically ~2,800–3,200 counts on 12-bit ADC).
2. **Submerged Baseline (100% Volumetric Water Content):**
   - Submerge probe blade into a beaker of water up to the white stop line (keep top electronics dry!).
   - Record raw ADC reading: $ADC_{\text{wet}}$ (typically ~1,200–1,600 counts).
3. **Linear Mapping Formula:**
   $$\text{Moisture \%} = \frac{ADC_{\text{dry}} - ADC_{\text{raw}}}{ADC_{\text{dry}} - ADC_{\text{wet}}} \times 100\%$$
   *(Clamped strictly to $[0.0\%, 100.0\%]$)*.

### 4.2 Soil pH Electrode
1. **Neutral Buffer (pH 7.00 / 6.86):**
   - Immerse probe in standard pH 6.86 buffer solution.
   - Record stable analog voltage $V_{\text{neutral}}$.
2. **Acidic Buffer (pH 4.01):**
   - Rinse probe with distilled water; immerse in standard pH 4.01 buffer solution.
   - Record stable analog voltage $V_{\text{acid}}$.
3. **Slope & Offset Derivation:**
   $$\text{Slope} = \frac{6.86 - 4.01}{V_{\text{neutral}} - V_{\text{acid}}}$$
   $$\text{pH} = 6.86 - \text{Slope} \times (V_{\text{neutral}} - V_{\text{raw}})$$

---

## 5. Open Questions for the Hardware Team

1. **TB6612FNG Standby (STBY) Pin:** Is STBY pulled HIGH in hardware via a pull-up resistor, or does it require continuous active-high drive from ESP32 GPIO 19?
2. **Submersible Pump Voltage Rail:** Does the pump operate at 5V from the LM2596 buck converter or 12V directly from the battery pack?
3. **ESP32-CAM Power Isolation:** During motor stall or transient load, 12V battery sag can cause ESP32-CAM brownout resets. Is there a dedicated capacitor bank (≥470µF) on the camera 5V rail?
4. **GPIO 5 Strapping & Relay Boot Behavior:** GPIO 5 is an ESP32 strapping pin (`VSPI CS0`) with boot-time clock output. Review the actual relay module input circuit: does it glitch or trigger during reset? Engineering proposal under review: assess whether an external 10 kΩ pull-up to 3.3V is needed, or assess reassigning `PIN_PUMP_RELAY` to **GPIO 33** (ADC1 Channel 5, standard digital output, no bootloader PWM/strapping functions). No pin changes will be made without explicit user authorization.
