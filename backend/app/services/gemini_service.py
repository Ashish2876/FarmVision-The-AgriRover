"""Google Gemini Provider Integration for FarmVision AI Assistant."""

import asyncio
import re
from typing import List, Optional, Any
from fastapi import HTTPException

from app.config import settings
from app.schemas import (
    AssistantContextPayload,
    ChatRequest,
    ChatResponse,
    InformationClass,
    ProvenanceItem,
)
from app.services.provider_interface import AssistantServiceBase

SYSTEM_INSTRUCTION = """You are FarmVision AI, an expert agricultural advisor assisting a farmer who operates the FarmVision AgriRover robotic farming platform.

SAFETY AND ACTUATOR CONSTRAINTS (CRITICAL):
1. You are strictly an ADVISORY assistant. You have ZERO capability to actuate hardware, drive motors, open water valves, spray chemicals, or toggle relays. Rover hardware has ACTUATORS_ENABLED = 0.
2. If the user asks you to move the rover, drive, turn, stop, water crops, spray, or operate any physical equipment, explicitly inform them that you cannot control actuators and they must use the manual Rover Control Station or Hardware Center interface.
3. SENSOR HONESTY AND ACCURACY:
   - Soil moisture raw readings are raw analog ADC counts from a capacitive sensor, NOT calibrated percentages. Never claim a soil is dry or soaked based solely on raw ADC counts without soil-texture calibration curves.
   - Soil pH probe reading is an uncalibrated analog signal. Remind the user to verify with chemical test strips or calibrated meters for critical pH adjustments.
   - Stale readings (exceeding freshness window) must be identified as potentially outdated.
   - Missing readings must be acknowledged as unavailable, never invented or assumed to be zero.
   - Demo readings must be acknowledged as simulated demonstration data.
4. AGRONOMIC GUIDANCE:
   - Provide clear, practical, sustainable agronomic recommendations (irrigation scheduling, soil aeration, nutrient management, pest prevention) using simple, respectful language.
   - Include standard safety disclaimers for chemical or pesticide handling. Recommend consulting certified local agricultural extension services.
"""


def _format_retry_delay(seconds: int) -> str:
    """Format duration in seconds into human-readable hours, minutes, and seconds."""
    hours = seconds // 3600
    minutes = (seconds % 3600) // 60
    secs = seconds % 60
    parts = []
    if hours > 0:
        parts.append(f"{hours}h")
    if minutes > 0:
        parts.append(f"{minutes}m")
    if secs > 0 or not parts:
        parts.append(f"{secs}s")
    return " ".join(parts)


def _extract_quota_reset_duration(exc: Exception) -> Optional[str]:
    """Extract and format retry duration from Google API exception if available."""
    details = getattr(exc, "details", None)
    if details and isinstance(details, (list, tuple)):
        for item in details:
            if isinstance(item, dict) and "retryDelay" in item:
                delay_str = str(item["retryDelay"]).strip()
                m = re.match(r"^(\d+)(?:\.\d+)?s?$", delay_str)
                if m:
                    sec_val = int(m.group(1))
                    return _format_retry_delay(sec_val)

    exc_text = str(exc)
    match = re.search(r"Please retry in\s+([0-9a-zA-Z\.]+)", exc_text)
    if match:
        raw_dur = match.group(1).rstrip(".")
        clean_dur = re.sub(r"\.\d+s", "s", raw_dur)
        formatted = re.sub(r"([a-z])([0-9])", r"\1 \2", clean_dur)
        return formatted

    return None


class GeminiAssistantService(AssistantServiceBase):
    """Google Gemini AI service implementation with lazy module loading."""

    def __init__(self) -> None:
        self._configured: bool = settings.is_gemini_configured()
        self._model: Optional[Any] = None

    def _ensure_initialized(self) -> None:
        if not self._configured:
            return
        if self._model is None:
            import google.generativeai as genai
            genai.configure(api_key=settings.gemini_api_key, transport="rest")
            self._model = genai.GenerativeModel(
                model_name=settings.gemini_model,
                system_instruction=SYSTEM_INSTRUCTION,
            )

    def is_configured(self) -> bool:
        return self._configured

    def _build_context_prompt(self, context: Optional[AssistantContextPayload]) -> str:
        """Construct deterministic context description with explicit veracity markers."""
        if not context or not context.consent_granted:
            return "No farm sensor or telemetry data was shared for this inquiry."

        parts: List[str] = []

        # Crop Profile
        if context.share_crop_profile and context.crop_profile:
            cp = context.crop_profile
            parts.append(
                f"[USER-PROVIDED CROP PROFILE]\n"
                f"- Crop: {cp.crop_type or 'Unspecified'}\n"
                f"- Growth Stage: {cp.growth_stage or 'Unspecified'}\n"
                f"- Soil Type: {cp.soil_type or 'Unspecified'}\n"
                f"- Planting Date: {cp.planting_date or 'Unspecified'}"
            )

        # Live / Telemetry Context
        if context.share_live_telemetry and context.sensor_context:
            sc = context.sensor_context
            origin_tag = f"[{sc.data_origin or 'UNKNOWN'} ORIGIN]"
            stale_tag = " [STALE READING]" if sc.is_stale else ""
            parts.append(
                f"{origin_tag}{stale_tag} TELEMETRY CONTEXT:\n"
                f"- Battery: {sc.battery_percent:.1f}%" if sc.battery_percent is not None else "- Battery: Unavailable"
            )
            if sc.solar_volts is not None:
                parts.append(f"- Solar Panel Voltage: {sc.solar_volts:.2f} V")
            if sc.soil_moisture_raw is not None:
                parts.append(
                    f"- Soil Moisture ADC Count: {sc.soil_moisture_raw} "
                    f"(UNCALIBRATED raw capacitive count; NOT a percentage; status={sc.soil_moisture_label or 'raw'})"
                )
            if sc.soil_ph_raw is not None:
                parts.append(f"- Soil pH: {sc.soil_ph_raw:.1f} (UNCALIBRATED probe reading)")
            if sc.soil_temperature is not None:
                parts.append(f"- Soil Temperature: {sc.soil_temperature:.1f} °C")
            if sc.air_temp is not None:
                parts.append(f"- Air Temperature: {sc.air_temp:.1f} °C")
            if sc.air_humidity is not None:
                parts.append(f"- Air Relative Humidity: {sc.air_humidity:.1f} %")

        # Alerts Context
        if context.share_alerts and context.alert_context:
            alerts_text = "\n".join(
                f"- [{a.severity}] {a.title}: Metric={a.metric}, Current={a.current_value}, Threshold={a.threshold_value}"
                for a in context.alert_context
            )
            parts.append(f"[ACTIVE FARM ALERTS]\n{alerts_text}")

        # Historical Analytics Context
        if context.share_historical and context.historical_context:
            hist_text = "\n".join(
                f"- {h.metric_name} ({h.range_label}): Avg={h.avg_value}, Min={h.min_value}, Max={h.max_value}, Count={h.data_points_count}"
                for h in context.historical_context
            )
            parts.append(f"[HISTORICAL TELEMETRY TRENDS]\n{hist_text}")

        return "\n\n".join(parts) if parts else "No farm sensor data was shared."

    async def generate_response(self, request: ChatRequest) -> ChatResponse:
        if not self._configured:
            raise HTTPException(
                status_code=503,
                detail=(
                    "Gemini AI provider is not configured on the backend. "
                    "Configure a valid GEMINI_API_KEY in backend/.env or your environment."
                ),
            )

        self._ensure_initialized()
        if self._model is None:
            raise HTTPException(
                status_code=503,
                detail="Gemini AI model failed to initialize.",
            )

        context_prompt = self._build_context_prompt(request.context)
        user_prompt = (
            f"FARM CONTEXT:\n{context_prompt}\n\n"
            f"FARMER QUESTION:\n{request.user_message}"
        )

        try:
            loop = asyncio.get_event_loop()
            response = await asyncio.wait_for(
                loop.run_in_executor(None, lambda: self._model.generate_content(user_prompt)),
                timeout=settings.request_timeout_seconds,
            )

            reply_text = response.text if response and response.text else "No response generated by model."

            # Construct provenance
            provenance: List[ProvenanceItem] = []
            if request.context and request.context.consent_granted:
                if request.context.share_live_telemetry and request.context.sensor_context:
                    sc = request.context.sensor_context
                    info_class = (
                        InformationClass.DEMO_SIMULATED
                        if sc.data_origin == "DEMO"
                        else InformationClass.LIVE_TELEMETRY
                    )
                    provenance.append(
                        ProvenanceItem(
                            source="FarmVision Telemetry Subsystem",
                            information_class=info_class,
                            description=f"Sensor readings (origin: {sc.data_origin}, stale: {sc.is_stale}).",
                        )
                    )
                if request.context.share_alerts and request.context.alert_context:
                    provenance.append(
                        ProvenanceItem(
                            source="AlertRepository",
                            information_class=InformationClass.LIVE_TELEMETRY,
                            description=f"{len(request.context.alert_context)} active farm alerts.",
                        )
                    )
                if request.context.share_historical and request.context.historical_context:
                    provenance.append(
                        ProvenanceItem(
                            source="TelemetryHistoryRepository",
                            information_class=InformationClass.HISTORICAL_TELEMETRY,
                            description="Historical aggregates for telemetry metrics.",
                        )
                    )
                if request.context.share_crop_profile and request.context.crop_profile:
                    provenance.append(
                        ProvenanceItem(
                            source="User Crop Profile",
                            information_class=InformationClass.USER_PROVIDED,
                            description="User-supplied crop and soil information.",
                        )
                    )

            provenance.append(
                ProvenanceItem(
                    source=f"Google Gemini ({settings.gemini_model})",
                    information_class=InformationClass.AI_SUGGESTION,
                    description="Cloud generative model reasoning and agronomic synthesis.",
                )
            )

            return ChatResponse(
                client_message_id=request.client_message_id,
                conversation_id=request.conversation_id,
                reply_text=reply_text,
                provenance=provenance,
                model_used=settings.gemini_model,
            )

        except asyncio.TimeoutError:
            raise HTTPException(
                status_code=504,
                detail=f"Gemini API request timed out after {settings.request_timeout_seconds} seconds.",
            )
        except Exception as e:
            err_name = type(e).__name__
            if "ResourceExhausted" in err_name or "TooManyRequests" in err_name:
                retry_duration = _extract_quota_reset_duration(e)
                if retry_duration:
                    detail_msg = (
                        f"FarmVision AI has reached its current usage limit. "
                        f"Please try again when the quota resets (expected in: {retry_duration})."
                    )
                else:
                    detail_msg = (
                        "FarmVision AI has reached its current usage limit. "
                        "Please try again when the quota resets."
                    )
                raise HTTPException(
                    status_code=429,
                    detail=detail_msg,
                )
            if "Unauthenticated" in err_name or "PermissionDenied" in err_name:
                raise HTTPException(
                    status_code=401,
                    detail="Gemini API key is invalid or unauthenticated.",
                )
            if "InvalidArgument" in err_name:
                raise HTTPException(
                    status_code=400,
                    detail=f"Invalid request sent to Gemini: {str(e)}",
                )
            raise HTTPException(
                status_code=502,
                detail=f"Gemini provider error: {err_name}",
            )
