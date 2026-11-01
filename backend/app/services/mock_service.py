"""Deterministic Mock Assistant Service for testing, offline use, and demo mode."""

import re
from typing import List
from app.schemas import (
    ChatRequest,
    ChatResponse,
    InformationClass,
    ProvenanceItem,
)
from app.services.provider_interface import AssistantServiceBase

# Actuator keywords that trigger strict safety rejection
ACTUATOR_PATTERN = re.compile(
    r"\b(drive|move|forward|backward|reverse|turn|steer|pump|water|spray|relay|motor|servo|actuate|speed)\b",
    re.IGNORECASE,
)


class MockAssistantService(AssistantServiceBase):
    """Deterministic mock provider delivering honest agricultural guidance without external API calls."""

    async def generate_response(self, request: ChatRequest) -> ChatResponse:
        user_text = request.user_message.strip()
        context = request.context
        provenance: List[ProvenanceItem] = []

        # 1. STRICT ACTUATOR SAFETY CHECK
        if ACTUATOR_PATTERN.search(user_text):
            reply = (
                "⚠️ **Actuator Safety Notice**: The FarmVision AI Assistant operates strictly in ADVISORY mode "
                "with zero actuator capability (`ACTUATORS_ENABLED = 0`).\n\n"
                "I cannot command motors, drive the rover, activate pumps, or toggle relays. "
                "To operate rover hardware or adjust irrigation equipment, please use the manual "
                "Rover Control Station or the Hardware Center interface."
            )
            provenance.append(
                ProvenanceItem(
                    source="AI Safety Subsystem",
                    information_class=InformationClass.AI_SUGGESTION,
                    description="Actuator isolation enforcement - command execution blocked.",
                )
            )
            return ChatResponse(
                client_message_id=request.client_message_id,
                conversation_id=request.conversation_id,
                reply_text=reply,
                provenance=provenance,
                model_used="mock-deterministic-v1",
            )

        # 2. ALERT EXPLANATION
        if context and context.share_alerts and context.alert_context:
            alert = context.alert_context[0]
            reply = (
                f"### Alert Diagnosis: {alert.title}\n"
                f"- **Severity**: {alert.severity}\n"
                f"- **Metric**: {alert.metric} (Current: {alert.current_value}, Threshold: {alert.threshold_value})\n\n"
                f"**Agronomic Meaning**: This alert indicates that {alert.metric} has crossed defined safety margins. "
                f"For {alert.metric}, prolonged deviation can stress crop roots and reduce yield.\n\n"
                f"**Recommended Safe Actions**:\n"
                f"1. Visually inspect the sensor probe in the field for loose wiring or soil detachment.\n"
                f"2. Confirm ambient conditions using secondary manual measurements.\n"
                f"3. Check root zone aeration before applying corrective amendments."
            )
            provenance.append(
                ProvenanceItem(
                    source=f"AlertRepository ({alert.alert_id})",
                    information_class=InformationClass.LIVE_TELEMETRY if "DEMO" not in alert.alert_id else InformationClass.DEMO_SIMULATED,
                    description="Active farm alert context provided with user consent.",
                )
            )
            return ChatResponse(
                client_message_id=request.client_message_id,
                conversation_id=request.conversation_id,
                reply_text=reply,
                provenance=provenance,
                model_used="mock-deterministic-v1",
            )

        # 3. SENSOR READING INTERPRETATION
        if context and context.share_live_telemetry and context.sensor_context:
            sensor = context.sensor_context
            moisture_desc = (
                f"Raw ADC {sensor.soil_moisture_raw} (Uncalibrated analog count; requires soil texture calibration)"
                if sensor.soil_moisture_raw is not None
                else "Sensor reading unavailable"
            )
            ph_desc = (
                f"{sensor.soil_ph_raw:.1f} (Uncalibrated probe; use chemical test strips for verification)"
                if sensor.soil_ph_raw is not None
                else "Sensor reading unavailable"
            )
            temp_desc = f"{sensor.air_temp:.1f} °C" if sensor.air_temp is not None else "Unavailable"
            hum_desc = f"{sensor.air_humidity:.1f} %" if sensor.air_humidity is not None else "Unavailable"
            stale_warning = "\n> ⚠️ *Notice: Telemetry data is stale and may not reflect current field conditions.*" if sensor.is_stale else ""
            demo_notice = f" [{sensor.data_origin} Mode]" if sensor.data_origin else ""

            reply = (
                f"### Current Sensor Observations{demo_notice}{stale_warning}\n\n"
                f"- **Soil Moisture**: {moisture_desc}\n"
                f"- **Soil pH**: {ph_desc}\n"
                f"- **Air Temperature**: {temp_desc}\n"
                f"- **Relative Humidity**: {hum_desc}\n\n"
                f"**Data Honesty Note**: Raw capacitive soil moisture readings vary by soil compaction and salinity. "
                f"Do not interpret raw ADC counts as calibrated moisture percentages. "
                f"Ensure the probe is securely seated at root depth."
            )
            provenance.append(
                ProvenanceItem(
                    source="FarmVision Telemetry Subsystem",
                    information_class=InformationClass.DEMO_SIMULATED if sensor.data_origin == "DEMO" else InformationClass.LIVE_TELEMETRY,
                    description=f"Sensor readings origin: {sensor.data_origin or 'UNKNOWN'}, stale={sensor.is_stale}.",
                )
            )
            return ChatResponse(
                client_message_id=request.client_message_id,
                conversation_id=request.conversation_id,
                reply_text=reply,
                provenance=provenance,
                model_used="mock-deterministic-v1",
            )

        # 4. HISTORICAL TRENDS
        if context and context.share_historical and context.historical_context:
            trend = context.historical_context[0]
            reply = (
                f"### Historical Trend: {trend.metric_name} ({trend.range_label})\n\n"
                f"- Data points analyzed: {trend.data_points_count}\n"
                f"- Average: {trend.avg_value:.2f if trend.avg_value is not None else 'N/A'}\n"
                f"- Range: [{trend.min_value:.2f if trend.min_value is not None else 'N/A'} to {trend.max_value:.2f if trend.max_value is not None else 'N/A'}]\n\n"
                f"**Observation**: Trends indicate diurnal fluctuation consistent with solar cycles. "
                f"Continue monitoring stability across multiple irrigation intervals."
            )
            provenance.append(
                ProvenanceItem(
                    source="TelemetryHistoryRepository",
                    information_class=InformationClass.HISTORICAL_TELEMETRY,
                    description=f"Historical telemetry aggregate over {trend.range_label}.",
                )
            )
            return ChatResponse(
                client_message_id=request.client_message_id,
                conversation_id=request.conversation_id,
                reply_text=reply,
                provenance=provenance,
                model_used="mock-deterministic-v1",
            )

        # 5. GENERAL AGRICULTURAL ADVICE
        reply = (
            f"Thank you for reaching out to FarmVision AI Assistant.\n\n"
            f"Regarding your inquiry about: *\"{user_text}\"*\n\n"
            f"**Agronomic Guidance**:\n"
            f"- **Irrigation Management**: Schedule watering during early morning hours (6:00 AM – 8:00 AM) to minimize evaporative loss and fungal leaf spot risk.\n"
            f"- **Soil Health**: Check root aeration and maintain organic mulch (5–7 cm layer) to buffer soil temperature against extreme thermal fluctuations.\n"
            f"- **Nutrient Balance**: Monitor nitrogen and potassium ratios during active vegetative growth. For pH balancing, always conduct an authorized soil test before adding lime or sulfur.\n\n"
            f"*(Advisory note: Recommendations are based on general agronomic principles. Verify with your local extension service for region-specific practices.)*"
        )
        provenance.append(
            ProvenanceItem(
                source="General Agronomy Knowledge Base",
                information_class=InformationClass.GENERAL_KNOWLEDGE,
                description="Standard agricultural practice guidelines.",
            )
        )
        return ChatResponse(
            client_message_id=request.client_message_id,
            conversation_id=request.conversation_id,
            reply_text=reply,
            provenance=provenance,
            model_used="mock-deterministic-v1",
        )

