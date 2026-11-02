# FarmVision — Phase 15: Production Readiness Specification

**Project:** FarmVision – The AgriRover  
**Application ID:** `com.farmvision.agrirover`  
**Version Name:** `1.0.0`  
**Version Code:** `1`  
**Target Platform:** Android 15 (API 35), Min SDK 26 (Android 8.0 Oreo)  
**Lead Developer:** Ashish Sampat Pawar  
**Date:** November 2026  
**Hardware Invariant:** `ACTUATORS_ENABLED = 0` (Strictly Interlocked)

---

## 1. Executive Summary

This document specifies the production readiness status of the FarmVision AgriRover platform at the conclusion of Phase 15. The application architecture, user interface, cloud AI gateway, security boundaries, and automated test suites have achieved release quality.

---

## 2. Production Baseline Verification

| Component | Specification | Verification Result | Status |
|:---|:---|:---|:---:|
| **App Shell & UI** | 5-Tab Material 3 Navigation (`Home`, `Rover`, `My Farm`, `Analytics`, `Settings`) | Verified on `emulator-5554` across all destinations and landscape cockpit mode. | **PASSED** |
| **Alert Overlay** | Universal Alert Center with push notification deep-linking | Verified via ADB intent `--es EXTRA_DESTINATION alerts`. | **PASSED** |
| **Cloud AI Gateway** | FastAPI proxy mediating Gemini 3.5 Flash Lite | Verified live chat with honest sensor disclaimers and `ACTUATORS_ENABLED = 0` reminders. | **PASSED** |
| **Privacy Boundaries** | Dual-layer client and backend telemetry exclusion | Tested in unit tests and payload serializers; disabled categories omitted. | **PASSED** |
| **R8 / Minification** | Code optimization, obfuscation, and resource shrinking | Built with AGP R8; shrunk APK from 18.2 MB to 5.85 MB (68% reduction). | **PASSED** |
| **Test Coverage** | 340 Android unit tests + 15 FastAPI pytests | 100% test pass rate across all modules. | **PASSED** |

---

## 3. Build & Artifact Matrix

- **Application ID:** `com.farmvision.agrirover`
- **Compile SDK:** 35 | **Target SDK:** 35 | **Min SDK:** 26
- **Debug APK:** `app/build/outputs/apk/debug/app-debug.apk` (18,193,233 bytes)
- **Release APK (Unsigned):** `app/build/outputs/apk/release/app-release-unsigned.apk` (5,853,808 bytes)
- **R8 Mapping File:** `app/build/outputs/mapping/release/mapping.txt` (23,160,132 bytes)
- **Signing Keystore:** Conditional signing via `local.signing.properties` or environment variables. Production key must be supplied by the repository owner.

---

## 4. Hardware Safety Declaration

`ACTUATORS_ENABLED = 0` remains strictly enforced. All physical actuators (motors, water pumps, sprayers, relays) remain disarmed. Software orientation changes emit neutral velocity vectors without hardware side-effects.

