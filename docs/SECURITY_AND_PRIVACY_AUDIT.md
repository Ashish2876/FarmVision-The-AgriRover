# FarmVision — Security & Privacy Release Audit

**Audit Date:** November 2026  
**Auditor:** Automated Security & Architecture Review  
**Target:** FarmVision Android Client & FastAPI Backend  
**Application ID:** `com.farmvision.agrirover`  

---

## 1. Credential & Secret Protection

| Item | Status | Verification Detail |
|:---|:---:|:---|
| **Android Source Keys** | **PASS** | Confirmed 0 Gemini API keys, AWS credentials, or passwords exist in Android source files. |
| **Backend API Key** | **PASS** | Backend loads key from environment variable `GEMINI_API_KEY`. Never printed in logs or responses. |
| **Git Invariant** | **PASS** | `backend/.env` is strictly ignored by `.gitignore` (`!! backend/.env`). `.env.example` has dummy placeholders. |
| **Signing Keystore** | **PASS** | No `.jks` or `.keystore` committed. Keystores and `local.signing.properties` are ignored by Git. |

---

## 2. Network Security Configuration

- **Global Policy:** `cleartextTrafficPermitted="false"` across all public internet domains.
- **Scoped Subnets:** Cleartext HTTP is allowed strictly for the local rover SoftAP network (`192.168.4.0/24`) and local loopback addresses (`10.0.2.2`, `127.0.0.1`, `localhost`).
- **Production AI Communication:** Cloud deployments must use HTTPS with valid TLS certificates.

---

## 3. Privacy & User Consent Invariants

1. **Granular Switches:** The application provides distinct user toggles for:
   - Live Rover Telemetry
   - Active Farm Alerts
   - Historical Analytics Trends
   - User-defined Crop Profiles
2. **Dual-Layer Enforcement:**
   - **Client-Side:** `ContextBuilder.kt` strips data when toggles are disabled.
   - **Backend-Side:** `gemini_service.py` validates boolean flags before injecting text into Google Gemini prompt payloads.

---

## 4. Android Permissions Review

Declared in `AndroidManifest.xml`:
- `android.permission.INTERNET`: Required for AI communication.
- `android.permission.ACCESS_NETWORK_STATE`: Network connectivity checks.
- `android.permission.ACCESS_WIFI_STATE`: Wi-Fi SoftAP status checks.
- `android.permission.POST_NOTIFICATIONS`: Android 13+ alert notifications.

Zero dangerous or intrusive permissions (camera, location, microphone, storage) are requested in the manifest.

