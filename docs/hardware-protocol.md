# FarmVision AgriRover — Microcontroller Communication Protocol Specification

**Document Version:** 1.0.0  
**Status:** DRAFT / PURE CODE SPECIFICATION (Milestone H3)  
**Target Controllers:** ESP32 DevKit V1 (Main Core), ESP32-CAM (Vision Endpoint)  
**Application ID:** `com.farmvision.agrirover`

---

## 1. Overview & Architectural Boundaries

This document defines the formal, versioned communication contract between the **FarmVision AgriRover Android Application** and the on-rover microcontrollers.

### Core Protocol Principles
1. **Versioned Contract:** All messages must carry a `protocolVersion` matching `"1.0.0"`. Major version mismatches must be rejected with `ERR_VERSION_MISMATCH`.
2. **Dynamic Capabilities Discovery:** The Android app never presumes physical components are connected. Microcontrollers report their active hardware via `GET /api/v1/capabilities`.
3. **Dual-Endpoint Topology:**
   - **Main ESP32 DevKit V1** (`http://192.168.4.1:80` by default): Controls sensors (soil moisture, DHT22, pH, ultrasonic), locomotion motors (TB6612FNG), servos, and irrigation relays.
   - **ESP32-CAM** (`http://192.168.4.2:81` by default): Dedicated vision endpoint providing OV2640 camera status and MJPEG stream (`/stream`). The camera endpoint **never** carries drive or actuation commands.
4. **Safety & Deadman Expiry:** Continuous drive commands carry a Time-To-Live (TTL) of **250 ms**. If no valid command packet arrives within 250 ms, the firmware automatically cuts locomotion power.
5. **Physical Honesty Rule:** Actuators without encoder or flow sensors report firmware acknowledgement only (`acknowledged`); they must **never** claim physical closed-loop feedback.

---

## 2. Authentication & Network Transport

### 2.1 Security & Authentication
- All control and actuation endpoints require the bearer authentication header:
  ```http
  X-AgriRover-Token: <token>
  ```
- If the token is missing or incorrect, the controller responds with HTTP `401 Unauthorized` and JSON error `ERR_AUTH_INVALID`.
- **Secret Handling Rule:** Bearer tokens must NEVER be logged to serial output, logcat, or analytics, and must be redacted in UI display strings.

### 2.2 Android Network Constraints
- **Cleartext Traffic Scope:** Android blocks cleartext HTTP by default. A strictly scoped `network-security-config.xml` permits cleartext traffic exclusively to the local subnet (`192.168.4.0/24`) and loopback (`10.0.2.2`). App-wide cleartext is prohibited.
- **Wi-Fi AP Network Binding:** The ESP32 SoftAP does not provide internet access. Android may attempt to route packets through mobile data. Android `ConnectivityManager` network binding (`bindProcessToNetwork`) must bind rover socket connections to the active rover Wi-Fi network.

---

## 3. REST Endpoints Specification

### 3.1 Capabilities Report: `GET /api/v1/capabilities`
Executed upon connection establishment to inspect what hardware is physically compiled and enabled on the rover.

**Request:**
```http
GET /api/v1/capabilities HTTP/1.1
Host: 192.168.4.1
X-AgriRover-Token: <ROVER_AUTH_BEARER_TOKEN>
Accept: application/json
```

**Response (F0 Sensors-Only Firmware):**
```json
{
  "protocolVersion": "1.0.0",
  "deviceId": "agrirover-main-esp32",
  "firmwareVersion": "v1.0.0-f0-sensors",
  "firmwareStage": "F0",
  "controllerTarget": "Esp32DevKitV1",
  "supportedSensors": [
    {
      "id": "hw-sensor-soil-moisture-probes",
      "type": "SoilMoistureCapacitive",
      "unit": "%",
      "requiresCalibration": true,
      "isAvailable": true
    },
    {
      "id": "hw-sensor-dht22",
      "type": "AtmosphericDht22",
      "unit": "C_RH",
      "requiresCalibration": false,
      "isAvailable": true
    },
    {
      "id": "hw-sensor-soil-ph",
      "type": "AnalogSoilPh",
      "unit": "pH",
      "requiresCalibration": true,
      "isAvailable": true
    },
    {
      "id": "hw-sensor-ultrasonic",
      "type": "DistanceHcSr04",
      "unit": "cm",
      "requiresCalibration": false,
      "isAvailable": true
    }
  ],
  "supportedActuators": [
    {
      "id": "hw-motors-bo-geared",
      "type": "DifferentialDriveMotors",
      "hasPhysicalFeedback": false,
      "isEnabled": false
    },
    {
      "id": "hw-irrigation-pump",
      "type": "SubmersiblePumpRelay",
      "hasPhysicalFeedback": false,
      "isEnabled": false,
      "maxDurationMs": 10000
    }
  ],
  "features": ["TELEMETRY_STREAM", "EMERGENCY_STOP"],
  "isEmergencyStopSupported": true
}
```

---

### 3.2 Telemetry Ingestion: `GET /api/v1/telemetry`
Polled periodically (e.g. 2 Hz REST) or streamed over WebSocket (20 Hz).

**Response:**
```json
{
  "protocolVersion": "1.0.0",
  "deviceId": "agrirover-main-esp32",
  "firmwareVersion": "v1.0.0-f0-sensors",
  "sequenceNumber": 1042,
  "uptimeMs": 348500,
  "readings": {
    "hw-sensor-soil-moisture-probes": {
      "value": 42.5,
      "rawValue": 2150.0,
      "unit": "%",
      "isValid": true,
      "quality": "OK"
    },
    "hw-sensor-soil-moisture-probe-2": {
      "value": 2200.0,
      "rawValue": 2200.0,
      "unit": "counts",
      "isValid": true,
      "quality": "RAW_UNCALIBRATED"
    },
    "hw-sensor-dht22": {
      "value": 26.8,
      "rawValue": 26.8,
      "unit": "°C",
      "isValid": true,
      "quality": "OK"
    },
    "hw-sensor-soil-ph": {
      "value": 6.8,
      "rawValue": 1820.0,
      "unit": "pH",
      "isValid": true,
      "quality": "OK"
    },
    "hw-sensor-ultrasonic": {
      "value": 45.0,
      "rawValue": 45.0,
      "unit": "cm",
      "isValid": true,
      "quality": "OK"
    }
  },
  "diagnostics": {
    "freeHeapBytes": "142880",
    "wifiRssiDbm": "-54",
    "loopFrequencyHz": "50"
  }
}
```

---

### 3.3 Command Dispatch: `POST /api/v1/command`
Dispatches an actuation or motion command with an explicit sequence number and TTL.

**Request:**
```http
POST /api/v1/command HTTP/1.1
Host: 192.168.4.1
X-AgriRover-Token: <ROVER_AUTH_BEARER_TOKEN>
Content-Type: application/json

{
  "protocolVersion": "1.0.0",
  "cmdId": "cmd-170000-001",
  "sequence": 501,
  "commandType": "DRIVE_DIFFERENTIAL",
  "ttlMs": 250,
  "createdTimestampMs": 1700000000000,
  "priority": "NORMAL",
  "parameters": {
    "throttle": 0.65,
    "steering": 0.0,
    "driveMode": "Standard"
  }
}
```

**Acknowledgement Response:**
```json
{
  "protocolVersion": "1.0.0",
  "cmdId": "cmd-170000-001",
  "status": "ACCEPTED",
  "reasonCode": null,
  "latencyMs": 8,
  "timestampMs": 1700000000008
}
```

---

### 3.4 Emergency Stop: `POST /api/v1/emergency-stop`
Immediate high-priority safety cutoff. Halts all motor PWM and de-energizes the irrigation relay.
**Safety Rule:** Emergency Stop is processed **unconditionally**. An invalid or absent token will never block the transition to the de-energized, latched safe state.

**Request:**
```http
POST /api/v1/emergency-stop HTTP/1.1
Host: 192.168.4.1
X-AgriRover-Token: <OPTIONAL_BEARER_TOKEN>
Content-Type: application/json

{
  "protocolVersion": "1.0.0",
  "cmdId": "estop-170000-999",
  "sequence": 502,
  "commandType": "EMERGENCY_STOP",
  "ttlMs": 5000,
  "createdTimestampMs": 1700000000010,
  "priority": "CRITICAL",
  "parameters": {
    "reason": "OPERATOR_TRIGGERED"
  }
}
```

**Acknowledgement Response:**
```json
{
  "protocolVersion": "1.0.0",
  "cmdId": "estop-engaged",
  "status": "ACCEPTED",
  "reasonCode": "ESTOP_ENGAGED",
  "latencyMs": 1,
  "timestampMs": 1700000000012
}
```

---

### 3.5 Emergency Stop Reset: `POST /api/v1/emergency-stop/reset`
Re-arms the actuator engine after physical obstruction clearance.
**Authentication Rule:** Unlike the stop route, reset **strictly requires valid authentication** via `X-AgriRover-Token`. Resetting leaves all motor and pump outputs completely de-energized until a new motion packet is dispatched.

**Request:**
```http
POST /api/v1/emergency-stop/reset HTTP/1.1
Host: 192.168.4.1
X-AgriRover-Token: <ROVER_AUTH_BEARER_TOKEN>
Accept: application/json
```

**Acknowledgement Response:**
```json
{
  "protocolVersion": "1.0.0",
  "cmdId": "estop-reset",
  "status": "ACCEPTED",
  "reasonCode": "ESTOP_RESET",
  "latencyMs": 1,
  "timestampMs": 1700000000020
}
```

---

## 4. Structured Error Codes

All HTTP 4xx and 5xx responses must return standard JSON formatted errors:

```json
{
  "protocolVersion": "1.0.0",
  "errorCode": "ERR_COMMAND_EXPIRED",
  "message": "Command TTL exceeded (expired by 84ms). Actuation dropped.",
  "recoverable": true,
  "timestampMs": 1700000000090
}
```

| Error Code | HTTP Status | Description | Recoverable |
|---|---|---|---|
| `ERR_AUTH_MISSING` | 401 | `X-AgriRover-Token` header was omitted. | Yes |
| `ERR_AUTH_INVALID` | 401 | Provided bearer token does not match firmware secret. | Yes |
| `ERR_COMMAND_EXPIRED` | 408 | Command arrived after `createdTimestampMs + ttlMs`. | Yes |
| `ERR_COMMAND_REJECTED` | 400 | Command syntax, parameter range, or type unrecognized. | Yes |
| `ERR_PAYLOAD_TOO_LARGE` | 413 | Payload exceeded `MAX_PAYLOAD_BYTES` (4096 bytes). | Yes |
| `ERR_INVALID_PARAMETER` | 422 | NaN, infinity, or out-of-range sensor/actuator value. | Yes |
| `ERR_ACTUATOR_DISABLED` | 403 | Actuator commanded while in F0 stage or unconfirmed wiring. | Yes |
| `ERR_EMERGENCY_STOP_ACTIVE` | 423 | Motion command rejected because Emergency Stop is latched. | Yes |
| `ERR_DEVICE_BUSY` | 503 | ESP32 RTOS task queue full; throttle request rate. | Yes |
| `ERR_VERSION_MISMATCH` | 426 | Incompatible protocol version; requires client update. | No |

---

## 5. Strict Data Validation Rules

Both the Android application and ESP32 firmware must strictly reject invalid payloads:
1. **Numeric Sanitization:** Reject any payload containing JSON `NaN`, `Infinity`, or `-Infinity`.
2. **Payload Size Guard:** Any HTTP request payload exceeding **4,096 bytes** must be immediately aborted.
3. **Monotonic Sequences:** Telemetry and command sequences must strictly increase. Out-of-order packets must be dropped and recorded in diagnostics.
4. **Throttle & Steering Constraints:** Throttle must be in range `[-1.0, 1.0]`; steering must be in range `[-1.0, 1.0]`. Values outside this range are rejected with `ERR_INVALID_PARAMETER`.
5. **Pump Timeout Guard:** The irrigation pump relay carries a hard firmware timeout of **10,000 ms** (10s). Continuous on-commands beyond 10s will automatically trigger relay cutoff.
