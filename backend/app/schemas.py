"""Pydantic request and response schemas for FarmVision AI Assistant API."""

from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, field_validator


class InformationClass(str, Enum):
    """Categorization of information provenance."""
    LIVE_TELEMETRY = "LIVE_TELEMETRY"
    HISTORICAL_TELEMETRY = "HISTORICAL_TELEMETRY"
    DEMO_SIMULATED = "DEMO_SIMULATED"
    USER_PROVIDED = "USER_PROVIDED"
    GENERAL_KNOWLEDGE = "GENERAL_KNOWLEDGE"
    AI_SUGGESTION = "AI_SUGGESTION"


class ProvenanceItem(BaseModel):
    """Tracks the source and veracity class of information used in a reply."""
    source: str = Field(..., description="Name of the source component or sensor")
    information_class: InformationClass = Field(..., description="Veracity/provenance classification")
    description: str = Field(..., description="Human-readable explanation of origin and caveats")


class SensorContextPayload(BaseModel):
    """Sensor readings submitted with strict honesty caveats."""
    battery_percent: Optional[float] = Field(None, ge=0.0, le=100.0)
    solar_volts: Optional[float] = Field(None, ge=0.0)
    soil_moisture_raw: Optional[int] = Field(None, ge=0, description="Raw ADC count from capacitive sensor")
    soil_moisture_uncalibrated: bool = Field(True, description="Indicates moisture is an ADC count, not percentage")
    soil_moisture_label: Optional[str] = Field(None, description="Optional qualitative label if known")
    soil_temperature: Optional[float] = Field(None, description="Soil temp in Celsius")
    soil_ph_raw: Optional[float] = Field(None, description="Uncalibrated analog pH reading")
    soil_ph_label: Optional[str] = Field("UNCALIBRATED", description="Calibration disclaimer for pH")
    air_temp: Optional[float] = Field(None, description="Ambient air temperature in Celsius")
    air_humidity: Optional[float] = Field(None, ge=0.0, le=100.0, description="Air relative humidity percentage")
    data_origin: Optional[str] = Field("UNKNOWN", description="LIVE_ESP32, MOCK_HARDWARE, or DEMO")
    is_stale: bool = Field(False, description="Whether reading exceeds freshness threshold")
    reading_age_seconds: Optional[int] = Field(None, description="Seconds elapsed since reading was taken")


class AlertContextPayload(BaseModel):
    """Alert event context for diagnosis."""
    alert_id: str
    title: str
    severity: str = Field(..., description="INFO, WARNING, or CRITICAL")
    metric: str
    current_value: str
    threshold_value: str
    timestamp_epoch_ms: int


class HistoricalAnalyticsPayload(BaseModel):
    """Historical telemetry trend summary."""
    metric_name: str
    range_label: str
    min_value: Optional[float] = None
    max_value: Optional[float] = None
    avg_value: Optional[float] = None
    data_points_count: int = 0
    insufficient_data: bool = False


class CropProfilePayload(BaseModel):
    """User-provided crop profile."""
    crop_type: Optional[str] = None
    growth_stage: Optional[str] = None
    soil_type: Optional[str] = None
    planting_date: Optional[str] = None


class AssistantContextPayload(BaseModel):
    """Complete contextual payload attached to a user message."""
    consent_granted: bool = Field(True, description="User privacy consent status")
    share_live_telemetry: bool = Field(False, description="Granular consent for live telemetry")
    share_alerts: bool = Field(False, description="Granular consent for active alerts")
    share_historical: bool = Field(False, description="Granular consent for historical analytics")
    share_crop_profile: bool = Field(False, description="Granular consent for crop profile")
    sensor_context: Optional[SensorContextPayload] = None
    alert_context: Optional[List[AlertContextPayload]] = None
    historical_context: Optional[List[HistoricalAnalyticsPayload]] = None
    crop_profile: Optional[CropProfilePayload] = None


class ChatRequest(BaseModel):
    """User message request sent to the Assistant API."""
    client_message_id: str = Field(..., min_length=1, max_length=128, description="Client idempotency key")
    conversation_id: str = Field(..., min_length=1, max_length=128, description="Conversation session ID")
    user_message: str = Field(..., min_length=1, max_length=4000, description="User prompt text")
    context: Optional[AssistantContextPayload] = None
    force_mock: bool = Field(False, description="Force mock response for tests or offline demo")

    @field_validator("user_message", "conversation_id", "client_message_id")
    @classmethod
    def validate_non_blank(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Field cannot be empty or whitespace only.")
        return v.strip()


class ChatResponse(BaseModel):
    """Response returned by the Assistant API."""
    client_message_id: str
    conversation_id: str
    reply_text: str
    provenance: List[ProvenanceItem] = Field(default_factory=list)
    model_used: str
    disclaimer: str = (
        "FarmVision AI is an advisory tool. Sensor values are uncalibrated unless certified. "
        "Physical rover controls must be operated manually."
    )


class HealthResponse(BaseModel):
    """Service health response."""
    status: str = "ok"
    service: str = "farmvision-ai-assistant"
    version: str = "1.0.0"
    gemini_configured: bool
    model: str


class ErrorResponse(BaseModel):
    """Standardized API error response."""
    error_code: str
    message: str
    details: Optional[Dict[str, Any]] = None

