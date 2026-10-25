#include "web_server_handler.h"
#include "config.h"
#include "safety_core.h"
#include "sensor_manager.h"
#include "actuator_manager.h"
#include <ArduinoJson.h>

WebServerHandler::WebServerHandler()
    : _server(SERVER_PORT)
    , _lastEStopLogMs(0)
{
}

void WebServerHandler::init() {
    // Consolidated Custom Header Registration (Must precede _server.begin())
    const char* customHeaders[] = {"X-AgriRover-Token"};
    _server.collectHeaders(customHeaders, 1);

    // Configure HTTP routes
    _server.on("/api/v1/capabilities", HTTP_GET, [this]() { handleCapabilities(); });
    _server.on("/api/v1/telemetry", HTTP_GET, [this]() { handleTelemetry(); });
    _server.on("/api/v1/status", HTTP_GET, [this]() { handleStatus(); });
    _server.on("/api/v1/command", HTTP_POST, [this]() { handleCommand(); });
    _server.on("/api/v1/emergency-stop", HTTP_POST, [this]() { handleEmergencyStop(); });
    _server.on("/api/v1/emergency-stop/reset", HTTP_POST, [this]() { handleEmergencyStopReset(); });
    _server.onNotFound([this]() { handleNotFound(); });

    // Enable CORS headers for mobile and diagnostic clients
    _server.enableCORS(true);
    _server.begin();
    Serial.printf("[HTTP] REST WebServer started on port %d\n", SERVER_PORT);
}

void WebServerHandler::handleClient() {
    _server.handleClient();
}

void WebServerHandler::handleCapabilities() {
    JsonDocument doc;
    doc["protocolVersion"] = PROTOCOL_VERSION;
    doc["deviceId"] = DEVICE_ID;
    doc["firmwareVersion"] = FIRMWARE_VERSION;
    doc["firmwareStage"] = FIRMWARE_STAGE;
    doc["controllerTarget"] = CONTROLLER_TARGET;

    JsonArray sensors = doc["supportedSensors"].to<JsonArray>();

    JsonObject s1 = sensors.add<JsonObject>();
    s1["id"] = "hw-sensor-soil-moisture-probes";
    s1["type"] = "SoilMoistureCapacitive";
    s1["unit"] = "%";
    s1["requiresCalibration"] = true;
    s1["isAvailable"] = true;

    JsonObject s2 = sensors.add<JsonObject>();
    s2["id"] = "hw-sensor-dht22";
    s2["type"] = "AtmosphericDht22";
    s2["unit"] = "C_RH";
    s2["requiresCalibration"] = false;
    s2["isAvailable"] = true;

    JsonObject s3 = sensors.add<JsonObject>();
    s3["id"] = "hw-sensor-soil-ph";
    s3["type"] = "AnalogSoilPh";
    s3["unit"] = "pH";
    s3["requiresCalibration"] = true;
    s3["isAvailable"] = true;

    JsonObject s4 = sensors.add<JsonObject>();
    s4["id"] = "hw-sensor-ultrasonic";
    s4["type"] = "DistanceHcSr04";
    s4["unit"] = "cm";
    s4["requiresCalibration"] = false;
    s4["isAvailable"] = true;

    JsonArray actuators = doc["supportedActuators"].to<JsonArray>();

    JsonObject a1 = actuators.add<JsonObject>();
    a1["id"] = "hw-motors-bo-geared";
    a1["type"] = "DifferentialDriveMotors";
    a1["hasPhysicalFeedback"] = false;
    a1["isEnabled"] = (ACTUATORS_ENABLED == 1);

    JsonObject a2 = actuators.add<JsonObject>();
    a2["id"] = "hw-irrigation-pump";
    a2["type"] = "SubmersiblePumpRelay";
    a2["hasPhysicalFeedback"] = false;
    a2["isEnabled"] = (ACTUATORS_ENABLED == 1);
    a2["maxDurationMs"] = MAX_PUMP_DURATION_MS;

    JsonArray features = doc["features"].to<JsonArray>();
    features.add("TELEMETRY_STREAM");
    features.add("EMERGENCY_STOP");
#if ACTUATORS_ENABLED
    features.add("DRIVE_CONTROL");
    features.add("IRRIGATION_CONTROL");
#endif

    doc["isEmergencyStopSupported"] = true;

    String response;
    serializeJson(doc, response);
    _server.send(200, "application/json", response);
}

void WebServerHandler::handleTelemetry() {
    RoverTelemetryData telemetry = SensorManager::getInstance().getTelemetrySnapshot();

    JsonDocument doc;
    doc["protocolVersion"] = PROTOCOL_VERSION;
    doc["deviceId"] = DEVICE_ID;
    doc["firmwareVersion"] = FIRMWARE_VERSION;
    doc["sequenceNumber"] = (uint32_t)telemetry.sequenceNumber;
    doc["uptimeMs"] = telemetry.uptimeMs;

    JsonObject readings = doc["readings"].to<JsonObject>();

    // Soil moisture Probe 1
    JsonObject m = readings["hw-sensor-soil-moisture-probes"].to<JsonObject>();
    m["value"] = serialized(String(telemetry.soilMoisture.value, 1));
    m["rawValue"] = serialized(String(telemetry.soilMoisture.rawValue, 1));
    m["unit"] = telemetry.soilMoisture.unit;
    m["isValid"] = telemetry.soilMoisture.isValid;
    m["quality"] = telemetry.soilMoisture.quality;

    // Soil moisture Probe 2 (Uncalibrated raw counts)
    JsonObject m2 = readings["hw-sensor-soil-moisture-probe-2"].to<JsonObject>();
    m2["value"] = serialized(String(telemetry.soilMoisture2.value, 1));
    m2["rawValue"] = serialized(String(telemetry.soilMoisture2.rawValue, 1));
    m2["unit"] = telemetry.soilMoisture2.unit;
    m2["isValid"] = telemetry.soilMoisture2.isValid;
    m2["quality"] = telemetry.soilMoisture2.quality;

    // DHT22
    JsonObject d = readings["hw-sensor-dht22"].to<JsonObject>();
    d["value"] = serialized(String(telemetry.dhtTemperature.value, 1));
    d["rawValue"] = serialized(String(telemetry.dhtTemperature.rawValue, 1));
    d["unit"] = telemetry.dhtTemperature.unit;
    d["isValid"] = telemetry.dhtTemperature.isValid;
    d["quality"] = telemetry.dhtTemperature.quality;

    // Soil pH
    JsonObject p = readings["hw-sensor-soil-ph"].to<JsonObject>();
    p["value"] = serialized(String(telemetry.soilPh.value, 1));
    p["rawValue"] = serialized(String(telemetry.soilPh.rawValue, 1));
    p["unit"] = telemetry.soilPh.unit;
    p["isValid"] = telemetry.soilPh.isValid;
    p["quality"] = telemetry.soilPh.quality;

    // Ultrasonic
    JsonObject u = readings["hw-sensor-ultrasonic"].to<JsonObject>();
    u["value"] = serialized(String(telemetry.ultrasonicDistance.value, 1));
    u["rawValue"] = serialized(String(telemetry.ultrasonicDistance.rawValue, 1));
    u["unit"] = telemetry.ultrasonicDistance.unit;
    u["isValid"] = telemetry.ultrasonicDistance.isValid;
    u["quality"] = telemetry.ultrasonicDistance.quality;

    JsonObject diag = doc["diagnostics"].to<JsonObject>();
    diag["freeHeapBytes"] = String(telemetry.freeHeapBytes);
    diag["wifiRssiDbm"] = String(telemetry.wifiRssiDbm);
    diag["loopFrequencyHz"] = String(telemetry.loopFrequencyHz);

    String response;
    serializeJson(doc, response);
    _server.send(200, "application/json", response);
}

void WebServerHandler::handleStatus() {
    JsonDocument doc;
    doc["protocolVersion"] = PROTOCOL_VERSION;
    doc["deviceId"] = DEVICE_ID;
    doc["firmwareStage"] = FIRMWARE_STAGE;
    doc["uptimeMs"] = millis();
    doc["isEmergencyStopActive"] = SafetyCore::getInstance().isEmergencyStopActive();
    doc["freeHeapBytes"] = ESP.getFreeHeap();

    String response;
    serializeJson(doc, response);
    _server.send(200, "application/json", response);
}

void WebServerHandler::handleCommand() {
    if (!validateAuthToken()) return;

    if (SafetyCore::getInstance().isEmergencyStopActive()) {
        sendJsonError(423, "ERR_EMERGENCY_STOP_ACTIVE", "Motion command rejected: Emergency Stop is currently latched active.", true);
        return;
    }

    if (_server.hasArg("plain")) {
        String body = _server.arg("plain");
        JsonDocument doc;
        DeserializationError err = deserializeJson(doc, body);
        if (err) {
            sendJsonError(400, "ERR_COMMAND_REJECTED", "Malformed JSON command payload.", true);
            return;
        }

        const char* cmdType = doc["commandType"] | "UNKNOWN";
        const char* cmdId = doc["cmdId"] | "cmd-unknown";

#if (ACTUATORS_ENABLED == 0)
        // Stage F0 Invariant: Actuators strictly disabled
        if (strcmp(cmdType, "DRIVE_DIFFERENTIAL") == 0 ||
            strcmp(cmdType, "SET_PUMP") == 0 ||
            strcmp(cmdType, "SERVO_POSITION") == 0) {
            sendJsonError(403, "ERR_ACTUATOR_DISABLED", "Actuator commanded while in Stage F0 sensors-only firmware until wiring is confirmed.", true);
            return;
        }
#else
        // Stage F1: Actuators enabled with safety constraints
        if (strcmp(cmdType, "DRIVE_DIFFERENTIAL") == 0) {
            float throttle = doc["parameters"]["throttle"] | 0.0f;
            float steering = doc["parameters"]["steering"] | 0.0f;
            ActuatorManager::getInstance().setDrive(throttle, steering);
        } else if (strcmp(cmdType, "SET_PUMP") == 0) {
            uint32_t durationMs = doc["parameters"]["durationMs"] | 1000;
            ActuatorManager::getInstance().startPump(durationMs);
        } else if (strcmp(cmdType, "SERVO_POSITION") == 0) {
            const char* sId = doc["parameters"]["servoId"] | "hw-servo-camera";
            float angle = doc["parameters"]["angle"] | 90.0f;
            ActuatorManager::getInstance().setServo(sId, angle);
        } else if (strcmp(cmdType, "STOP") == 0) {
            ActuatorManager::getInstance().stopAllActuators();
        }
#endif

        // Feed deadman watchdog upon receiving valid command packet
        SafetyCore::getInstance().feedDeadmanWatchdog();

        JsonDocument ackDoc;
        ackDoc["protocolVersion"] = PROTOCOL_VERSION;
        ackDoc["cmdId"] = cmdId;
        ackDoc["status"] = "ACCEPTED";
        ackDoc["reasonCode"] = nullptr;
        ackDoc["latencyMs"] = 2;
        ackDoc["timestampMs"] = millis();

        String response;
        serializeJson(ackDoc, response);
        _server.send(200, "application/json", response);
    } else {
        sendJsonError(400, "ERR_COMMAND_REJECTED", "Empty command body.", true);
    }
}

namespace {
bool constantTimeEquals(const String& a, const char* b) {
    size_t lenA = a.length();
    size_t lenB = strlen(b);
    volatile uint8_t diff = (lenA ^ lenB);
    for (size_t i = 0; i < lenA; ++i) {
        char cB = (i < lenB) ? b[i] : 0;
        diff |= (a[i] ^ cB);
    }
    return diff == 0;
}
}

void WebServerHandler::handleEmergencyStop() {
    // Safety Invariant: Emergency Stop must NEVER be blocked by authentication failure.
    // The safety core is triggered unconditionally to transition actuators to de-energized state.
    bool hasValidAuth = (_server.hasHeader("X-AgriRover-Token") &&
                         constantTimeEquals(_server.header("X-AgriRover-Token"), DEFAULT_AUTH_TOKEN));

    String stopReason = hasValidAuth ? "OPERATOR_TRIGGERED" : "UNAUTHENTICATED_OPERATOR_TRIGGERED";
    SafetyCore::getInstance().triggerEmergencyStop(stopReason);

    // Throttled serial logging to protect FreeRTOS UART task from buffer flooding (max 1 log / sec)
    uint32_t nowMs = millis();
    if (nowMs - _lastEStopLogMs >= 1000) {
        _lastEStopLogMs = nowMs;
        Serial.printf("[SAFETY] Emergency Stop ENGAGED (Auth: %s)\n", hasValidAuth ? "VALID" : "NONE/INVALID");
    }

    JsonDocument doc;
    doc["protocolVersion"] = PROTOCOL_VERSION;
    doc["cmdId"] = "estop-engaged";
    doc["status"] = "ACCEPTED";
    doc["reasonCode"] = "ESTOP_ENGAGED";
    doc["latencyMs"] = 1;
    doc["timestampMs"] = millis();

    String response;
    serializeJson(doc, response);
    _server.send(200, "application/json", response);
}

void WebServerHandler::handleEmergencyStopReset() {
    // Resetting Emergency Stop requires explicit, valid bearer token authentication
    if (!validateAuthToken()) return;

    SafetyCore::getInstance().resetEmergencyStop();

    // Invariant: reset leaves all actuator outputs de-energized
    ActuatorManager::getInstance().stopAllActuators();

    Serial.println("[SAFETY] Emergency Stop RESET by authenticated operator.");

    JsonDocument doc;
    doc["protocolVersion"] = PROTOCOL_VERSION;
    doc["cmdId"] = "estop-reset";
    doc["status"] = "ACCEPTED";
    doc["reasonCode"] = "ESTOP_RESET";
    doc["latencyMs"] = 1;
    doc["timestampMs"] = millis();

    String response;
    serializeJson(doc, response);
    _server.send(200, "application/json", response);
}

void WebServerHandler::handleNotFound() {
    sendJsonError(404, "ERR_COMMAND_REJECTED", "Endpoint not found on this controller.", false);
}

bool WebServerHandler::validateAuthToken() {
    if (!_server.hasHeader("X-AgriRover-Token")) {
        sendJsonError(401, "ERR_AUTH_MISSING", "X-AgriRover-Token header was omitted.", true);
        return false;
    }

    String token = _server.header("X-AgriRover-Token");
    if (!constantTimeEquals(token, DEFAULT_AUTH_TOKEN)) {
        sendJsonError(401, "ERR_AUTH_INVALID", "Provided bearer token does not match firmware secret.", true);
        return false;
    }

    return true;
}

void WebServerHandler::sendJsonError(int httpStatus, const char* errorCode, const char* message, bool recoverable) {
    JsonDocument doc;
    doc["protocolVersion"] = PROTOCOL_VERSION;
    doc["errorCode"] = errorCode;
    doc["message"] = message;
    doc["recoverable"] = recoverable;
    doc["timestampMs"] = millis();

    String response;
    serializeJson(doc, response);
    _server.send(httpStatus, "application/json", response);
}
