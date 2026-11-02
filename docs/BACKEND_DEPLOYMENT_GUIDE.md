# FarmVision — FastAPI AI Assistant Backend Deployment Guide

This guide describes how to configure, run, and deploy the FarmVision Cloud AI Gateway mediating Google Gemini for the Android application.

---

## 1. Architecture Overview

The FastAPI backend is a secure proxy service that:
- Holds the Google Gemini API key securely on the server side.
- Accepts chat requests from the Android app (`POST /api/v1/assistant/chat`).
- Enforces sensor honesty invariants, privacy filtering, and actuator safety reminders.
- Returns agronomic guidance with detailed provenance and disclaimers.
- Exposes health diagnostics on `GET /health` and `GET /api/v1/health`.

---

## 2. Environment Configuration

Copy `backend/.env.example` to `backend/.env` and configure:

```ini
GEMINI_API_KEY=your_actual_gemini_api_key
GEMINI_MODEL=gemini-3.5-flash-lite
APP_ENV=production
HOST=0.0.0.0
PORT=8000
CORS_ORIGINS=https://farmvision.yourdomain.com
REQUEST_TIMEOUT_SECONDS=180.0
```

---

## 3. Running Locally (Development / Emulator)

```bash
cd backend
.venv\Scripts\python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```
- For Android Emulator, access via: `http://10.0.2.2:8000`
- For physical devices on the same Wi-Fi, access via: `http://<HOST_LOCAL_IP>:8000`

---

## 4. Production Deployment (Cloud / VPS)

### Production URL Status
- **Current Status:** **LOCAL DEVELOPMENT / NOT CONFIGURED IN CLOUD**
- The Android application does **not** hardcode a production cloud URL. In the Android app, go to **Settings > Rover & Device Settings > Backend Host URL** to enter your cloud endpoint (e.g. `https://api.farmvision.example.com`).

### Containerized Deployment (Docker)
```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```
Ensure TLS termination (HTTPS) is handled via a reverse proxy (e.g. NGINX or Caddy) with valid Let's Encrypt certificates.

