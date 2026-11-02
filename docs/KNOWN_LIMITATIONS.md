# FarmVision — Known Limitations & Roadmapped Capabilities

This document lists known operational boundaries, deferred enhancements, and future architectural integrations for FarmVision – The AgriRover.

---

## 1. Physical Actuation & Hardware Interlocks
- **Status:** **DISARMED BY DESIGN (`ACTUATORS_ENABLED = 0`)**
- **Detail:** In accordance with safety policies, all actuator operations (motors, irrigation valves, sprayers) operate strictly in simulation. Hardware arming is deferred to physical field commissioning by the embedded hardware team.

---

## 2. Relational Database / PostgreSQL
- **Status:** **DEFERRED / OUT OF SCOPE FOR CURRENT STAGE**
- **Detail:** Data persistence on Android uses lightweight JSON file repositories (`JsonFileAlertRepository`, `JsonFileAssistantRepository`, `JsonFileTelemetryHistoryRepository`) and `SharedPreferences`. The FastAPI backend operates as a stateless AI proxy without a local relational database or ORM layer.

---

## 3. Plant Disease Computer Vision Models
- **Status:** **FUTURE ROADMAP ENHANCEMENT**
- **Detail:** Disease detection is currently assisted via conversational agronomic advisory through Gemini 3.5 Flash Lite. On-device MobileNetV2 / TensorFlow Lite plant leaf disease segmentation models remain deferred for future specialized vision phases.

---

## 4. Production Cloud Backend Hosting
- **Status:** **LOCAL DEVELOPMENT CONFIGURED**
- **Detail:** The FastAPI backend is tested locally on `http://0.0.0.0:8000`. Public cloud deployment with HTTPS / TLS certificates is the responsibility of the cloud infrastructure team and can be targeted dynamically via Android Settings.

