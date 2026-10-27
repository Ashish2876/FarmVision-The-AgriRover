package com.farmvision.agrirover.data.analytics

import com.farmvision.agrirover.data.model.CalibrationState
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.HistoricalMetric
import com.farmvision.agrirover.data.model.TelemetrySample
import com.farmvision.agrirover.domain.protocol.RoverTelemetryPacket
import com.farmvision.agrirover.domain.repository.TelemetryHistoryRepository

/**
 * Pipeline component that maps and stores validated live Phase 8 telemetry packets
 * into the [TelemetryHistoryRepository] (Default D3 & Milestone N3).
 *
 * Invariants:
 * - Only fresh, validated samples that pass physical/electrical bounds are stored.
 * - Out-of-bounds, NaN, infinity, or saturated raw ADC counts (<= 50 or >= 4050) are rejected.
 * - Soil Probe 1 and Probe 2 are stored strictly as uncalibrated raw ADC counts on separate series.
 * - Every sample is tagged with [DataOrigin.LiveHardware].
 * - Connection failures, stale packets, or unpolled readings are never stored as zero or healthy.
 */
object LiveTelemetryIngestor {

    suspend fun ingestPacket(
        packet: RoverTelemetryPacket,
        historyRepository: TelemetryHistoryRepository,
        receiptEpochMillis: Long
    ): Int {
        val validSamples = mutableListOf<TelemetrySample>()

        // 1. Soil Moisture Probe 1 (Raw ADC counts, uncalibrated)
        val p1Payload = packet.readings["hw-sensor-soil-moisture-probes"]
        if (p1Payload != null && p1Payload.isValid) {
            val rawAdc = p1Payload.rawValue.toFloat()
            if (HistoricalMetric.SoilMoistureProbe1Raw.isWithinValidRange(rawAdc)) {
                validSamples.add(
                    TelemetrySample(
                        id = "live-p1-${packet.sequenceNumber}-$receiptEpochMillis",
                        metric = HistoricalMetric.SoilMoistureProbe1Raw,
                        canonicalValue = rawAdc,
                        rawValue = rawAdc,
                        timestampEpochMillis = receiptEpochMillis,
                        origin = DataOrigin.LiveHardware,
                        calibrationState = CalibrationState.Uncalibrated
                    )
                )
            }
        }

        // 2. Soil Moisture Probe 2 (Raw ADC counts, uncalibrated, separate series)
        val p2Payload = packet.readings["hw-sensor-soil-moisture-probe-2"]
        if (p2Payload != null && p2Payload.isValid) {
            val rawAdc = p2Payload.rawValue.toFloat()
            if (HistoricalMetric.SoilMoistureProbe2Raw.isWithinValidRange(rawAdc)) {
                validSamples.add(
                    TelemetrySample(
                        id = "live-p2-${packet.sequenceNumber}-$receiptEpochMillis",
                        metric = HistoricalMetric.SoilMoistureProbe2Raw,
                        canonicalValue = rawAdc,
                        rawValue = rawAdc,
                        timestampEpochMillis = receiptEpochMillis,
                        origin = DataOrigin.LiveHardware,
                        calibrationState = CalibrationState.Uncalibrated
                    )
                )
            }
        }

        // 3. Air Temperature (DHT22, Celsius)
        val tempPayload = packet.readings["hw-sensor-dht22"]
        if (tempPayload != null && tempPayload.isValid) {
            val tempC = tempPayload.value.toFloat()
            if (HistoricalMetric.AirTemperature.isWithinValidRange(tempC)) {
                validSamples.add(
                    TelemetrySample(
                        id = "live-temp-${packet.sequenceNumber}-$receiptEpochMillis",
                        metric = HistoricalMetric.AirTemperature,
                        canonicalValue = tempC,
                        rawValue = null,
                        timestampEpochMillis = receiptEpochMillis,
                        origin = DataOrigin.LiveHardware,
                        calibrationState = CalibrationState.FactoryDefault
                    )
                )
            }
        }

        // 4. Relative Humidity (DHT22, %)
        val humPayload = packet.readings["hw-sensor-dht22-humidity"] ?: packet.readings["hw-sensor-humidity"]
        if (humPayload != null && humPayload.isValid) {
            val humVal = humPayload.value.toFloat()
            if (HistoricalMetric.Humidity.isWithinValidRange(humVal)) {
                validSamples.add(
                    TelemetrySample(
                        id = "live-hum-${packet.sequenceNumber}-$receiptEpochMillis",
                        metric = HistoricalMetric.Humidity,
                        canonicalValue = humVal,
                        rawValue = null,
                        timestampEpochMillis = receiptEpochMillis,
                        origin = DataOrigin.LiveHardware,
                        calibrationState = CalibrationState.FactoryDefault
                    )
                )
            }
        }

        // 5. Soil pH
        val phPayload = packet.readings["hw-sensor-soil-ph"]
        if (phPayload != null && phPayload.isValid) {
            val phVal = phPayload.value.toFloat()
            if (HistoricalMetric.SoilPh.isWithinValidRange(phVal)) {
                validSamples.add(
                    TelemetrySample(
                        id = "live-ph-${packet.sequenceNumber}-$receiptEpochMillis",
                        metric = HistoricalMetric.SoilPh,
                        canonicalValue = phVal,
                        rawValue = null,
                        timestampEpochMillis = receiptEpochMillis,
                        origin = DataOrigin.LiveHardware,
                        calibrationState = CalibrationState.Uncalibrated
                    )
                )
            }
        }

        // 6. Ultrasonic Distance
        val sonarPayload = packet.readings["hw-sensor-ultrasonic"]
        if (sonarPayload != null && sonarPayload.isValid) {
            val distVal = sonarPayload.value.toFloat()
            if (HistoricalMetric.UltrasonicDistance.isWithinValidRange(distVal)) {
                validSamples.add(
                    TelemetrySample(
                        id = "live-sonar-${packet.sequenceNumber}-$receiptEpochMillis",
                        metric = HistoricalMetric.UltrasonicDistance,
                        canonicalValue = distVal,
                        rawValue = null,
                        timestampEpochMillis = receiptEpochMillis,
                        origin = DataOrigin.LiveHardware,
                        calibrationState = CalibrationState.FactoryDefault
                    )
                )
            }
        }

        return historyRepository.recordSamples(validSamples)
    }
}
