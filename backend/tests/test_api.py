"""Pytest test suite for FarmVision AI Assistant FastAPI backend."""

import sys
import time
from pathlib import Path
import pytest
from fastapi.testclient import TestClient

# Ensure backend root is on sys.path
_BACKEND_ROOT = Path(__file__).resolve().parent.parent
if str(_BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(_BACKEND_ROOT))

from app.main import app
from app.config import settings

client = TestClient(app)


def test_health_check_endpoint() -> None:
    """Verify health check returns 200 with service information."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == "farmvision-ai-assistant"
    assert data["version"] == "1.0.0"
    assert "gemini_configured" in data
    assert data["model"] == settings.gemini_model


def test_chat_schema_validation_failure() -> None:
    """Verify missing required fields fail with 422."""
    payload = {
        "conversation_id": "conv-1",
        # missing client_message_id and user_message
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 422
    data = response.json()
    assert data["error_code"] == "VALIDATION_ERROR"


def test_mock_chat_general_advice() -> None:
    """Verify mock assistant returns general agronomic advice when requested."""
    payload = {
        "client_message_id": "msg-001",
        "conversation_id": "conv-100",
        "user_message": "What is the best time to irrigate tomato plants?",
        "force_mock": True,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["client_message_id"] == "msg-001"
    assert data["conversation_id"] == "conv-100"
    assert "irrigate" in data["reply_text"].lower() or "morning" in data["reply_text"].lower()
    assert data["model_used"] == "mock-deterministic-v1"
    assert len(data["provenance"]) > 0
    assert data["provenance"][0]["information_class"] == "GENERAL_KNOWLEDGE"


def test_actuator_safety_rejection() -> None:
    """Verify any request asking to drive or pump triggers strict safety advisory refusal."""
    payload = {
        "client_message_id": "msg-002",
        "conversation_id": "conv-100",
        "user_message": "Please drive the rover forward 3 meters and turn on the water pump now.",
        "force_mock": True,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    reply = data["reply_text"]
    assert "Actuator Safety Notice" in reply
    assert "ACTUATORS_ENABLED = 0" in reply
    assert "Rover Control Station" in reply
    assert data["provenance"][0]["source"] == "AI Safety Subsystem"


def test_sensor_honesty_uncalibrated_adc() -> None:
    """Verify raw ADC soil moisture readings are explicitly marked as uncalibrated."""
    payload = {
        "client_message_id": "msg-003",
        "conversation_id": "conv-101",
        "user_message": "Check my current soil moisture reading.",
        "context": {
            "consent_granted": True,
            "share_live_telemetry": True,
            "sensor_context": {
                "soil_moisture_raw": 2840,
                "soil_moisture_uncalibrated": True,
                "soil_ph_raw": 6.8,
                "soil_ph_label": "UNCALIBRATED",
                "air_temp": 24.5,
                "air_humidity": 58.0,
                "data_origin": "LIVE_ESP32",
                "is_stale": False,
            },
        },
        "force_mock": True,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    reply = data["reply_text"]
    assert "2840" in reply
    assert "Uncalibrated" in reply
    assert "NOT a percentage" in reply or "Do not interpret raw ADC counts as calibrated" in reply


def test_stale_telemetry_warning() -> None:
    """Verify stale telemetry triggers a freshness warning."""
    payload = {
        "client_message_id": "msg-004",
        "conversation_id": "conv-102",
        "user_message": "How is the farm looking?",
        "context": {
            "consent_granted": True,
            "share_live_telemetry": True,
            "sensor_context": {
                "soil_moisture_raw": 1900,
                "air_temp": 29.0,
                "data_origin": "LIVE_ESP32",
                "is_stale": True,
                "reading_age_seconds": 180,
            },
        },
        "force_mock": True,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "stale" in data["reply_text"].lower()


def test_alert_diagnosis_context() -> None:
    """Verify alert context generates specific troubleshooting advice without altering alert state."""
    payload = {
        "client_message_id": "msg-005",
        "conversation_id": "conv-103",
        "user_message": "Explain this alert for me.",
        "context": {
            "consent_granted": True,
            "share_alerts": True,
            "alert_context": [
                {
                    "alert_id": "ALT-CRIT-001",
                    "title": "Critically Low Soil Moisture",
                    "severity": "CRITICAL",
                    "metric": "Soil Moisture",
                    "current_value": "3150 ADC",
                    "threshold_value": "3000 ADC",
                    "timestamp_epoch_ms": 1730467200000,
                }
            ],
        },
        "force_mock": True,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    reply = data["reply_text"]
    assert "Critically Low Soil Moisture" in reply
    assert "CRITICAL" in reply
    assert "Recommended Safe Actions" in reply


def test_missing_api_key_returns_503_when_mock_not_forced(monkeypatch: pytest.MonkeyPatch) -> None:
    """Verify that when real Gemini is invoked but no key is configured, HTTP 503 is returned."""
    from app.main import gemini_service
    monkeypatch.setattr(gemini_service, "_configured", False)
    payload = {
        "client_message_id": "msg-006",
        "conversation_id": "conv-104",
        "user_message": "Tell me about drip irrigation.",
        "force_mock": False,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 503
    data = response.json()
    assert data["error_code"] == "MISSING_API_KEY"
    assert "GEMINI_API_KEY" in data["message"]


def test_unauthenticated_api_error_returns_401(monkeypatch: pytest.MonkeyPatch) -> None:
    """Verify that upstream 401 unauthenticated maps to 401 UNAUTHENTICATED error code."""
    from app.main import gemini_service

    class DummyUnauthenticated(Exception):
        pass

    DummyUnauthenticated.__name__ = "UnauthenticatedError"
    monkeypatch.setattr(gemini_service, "_configured", True)
    monkeypatch.setattr(gemini_service, "_ensure_initialized", lambda: None)
    
    class FakeModel:
        def generate_content(self, *args, **kwargs):
            raise DummyUnauthenticated("API key not valid.")

    monkeypatch.setattr(gemini_service, "_model", FakeModel())
    payload = {
        "client_message_id": "msg-err-401",
        "conversation_id": "conv-err",
        "user_message": "Hello",
        "force_mock": False,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 401
    assert response.json()["error_code"] == "UNAUTHENTICATED"


def test_rate_limit_error_returns_429(monkeypatch: pytest.MonkeyPatch) -> None:
    """Verify that upstream rate limits map to 429 QUOTA_EXHAUSTED with user-friendly message."""
    from app.main import gemini_service

    class DummyResourceExhausted(Exception):
        pass

    DummyResourceExhausted.__name__ = "ResourceExhausted"
    monkeypatch.setattr(gemini_service, "_configured", True)
    monkeypatch.setattr(gemini_service, "_ensure_initialized", lambda: None)

    class FakeModel:
        def generate_content(self, *args, **kwargs):
            raise DummyResourceExhausted("Rate quota exhausted.")

    monkeypatch.setattr(gemini_service, "_model", FakeModel())
    payload = {
        "client_message_id": "msg-err-429",
        "conversation_id": "conv-err",
        "user_message": "Hello",
        "force_mock": False,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 429
    data = response.json()
    assert data["error_code"] == "QUOTA_EXHAUSTED"
    assert "FarmVision AI has reached its current usage limit" in data["message"]
    assert "Please try again when the quota resets" in data["message"]


def test_rate_limit_error_with_retry_delay_returns_duration(monkeypatch: pytest.MonkeyPatch) -> None:
    """Verify that 429 errors with Google retryDelay or regex include the formatted duration."""
    from app.main import gemini_service

    class DummyResourceExhausted(Exception):
        def __init__(self, message: str, details=None):
            super().__init__(message)
            self.details = details

    DummyResourceExhausted.__name__ = "ResourceExhausted"
    monkeypatch.setattr(gemini_service, "_configured", True)
    monkeypatch.setattr(gemini_service, "_ensure_initialized", lambda: None)

    class FakeModel:
        def generate_content(self, *args, **kwargs):
            raise DummyResourceExhausted(
                "Quota exceeded. Please retry in 13h20m14s.",
                details=[{"@type": "RetryInfo", "retryDelay": "48014s"}],
            )

    monkeypatch.setattr(gemini_service, "_model", FakeModel())
    payload = {
        "client_message_id": "msg-err-429-retry",
        "conversation_id": "conv-err",
        "user_message": "Hello",
        "force_mock": False,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 429
    data = response.json()
    assert data["error_code"] == "QUOTA_EXHAUSTED"
    assert "expected in: 13h 20m 14s" in data["message"]


def test_api_timeout_returns_504(monkeypatch: pytest.MonkeyPatch) -> None:
    """Verify that timeout during generate_content maps to 504 TIMEOUT."""
    import asyncio
    from app.main import gemini_service

    monkeypatch.setattr(gemini_service, "_configured", True)
    monkeypatch.setattr(gemini_service, "_ensure_initialized", lambda: None)

    class FakeModel:
        def generate_content(self, *args, **kwargs):
            time.sleep(0.1)

    # Force a microscopic timeout
    from app.config import settings
    monkeypatch.setattr(settings, "request_timeout_seconds", 0.001)
    monkeypatch.setattr(gemini_service, "_model", FakeModel())

    payload = {
        "client_message_id": "msg-err-504",
        "conversation_id": "conv-err",
        "user_message": "Hello",
        "force_mock": False,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 504
    assert response.json()["error_code"] == "TIMEOUT"


def test_whitespace_only_user_message_rejected() -> None:
    """Verify that whitespace-only user messages are rejected by validation with 422."""
    payload = {
        "client_message_id": "msg-ws",
        "conversation_id": "conv-ws",
        "user_message": "     ",
        "force_mock": False,
    }
    response = client.post("/api/v1/assistant/chat", json=payload)
    assert response.status_code == 422
    assert response.json()["error_code"] == "VALIDATION_ERROR"


def test_secret_protection_in_responses() -> None:
    """Verify that no sensitive key substring appears in any output."""
    response = client.get("/health")
    content_str = response.text
    assert "AIza" not in content_str
    assert "GEMINI_API_KEY=" not in content_str


def test_environment_model_selection(monkeypatch: pytest.MonkeyPatch) -> None:
    """Verify that GEMINI_MODEL is configurable via environment variable and defaults to Gemini 3.8 Flash."""
    from app.config import Settings
    
    # 1. Default should be gemini-3.8-flash
    monkeypatch.delenv("GEMINI_MODEL", raising=False)
    default_settings = Settings()
    assert default_settings.gemini_model == "gemini-3.8-flash"
    
    # 2. Configurable override via environment
    monkeypatch.setenv("GEMINI_MODEL", "gemini-2.5-flash-lite")
    custom_settings = Settings()
    assert custom_settings.gemini_model == "gemini-2.5-flash-lite"
    
    # 3. Ensure API key is never exposed in representation
    monkeypatch.setenv("GEMINI_API_KEY", "dummy-secret-key")
    keyed_settings = Settings()
    repr_str = repr(keyed_settings)
    assert "dummy-secret-key" not in repr_str
    assert "***CONFIGURED***" in repr_str
