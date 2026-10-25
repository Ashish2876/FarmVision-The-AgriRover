#include "safety_core.h"
#include "config.h"
#include "actuator_manager.h"

SafetyCore::SafetyCore()
    : _isEStopLatched(false)
    , _eStopReason("NORMAL_STANDBY")
    , _lastCommandTimestampMs(0)
    , _safetyMux(portMUX_INITIALIZER_UNLOCKED)
{
}

void SafetyCore::init() {
    portENTER_CRITICAL(&_safetyMux);
    _isEStopLatched = false;
    _eStopReason = "NORMAL_STANDBY";
    _lastCommandTimestampMs = millis();
    portEXIT_CRITICAL(&_safetyMux);

    executePhysicalCutoff();
}

bool SafetyCore::isEmergencyStopActive() const {
    return _isEStopLatched;
}

void SafetyCore::triggerEmergencyStop(const String& reason) {
    portENTER_CRITICAL(&_safetyMux);
    _isEStopLatched = true;
    _eStopReason = reason;
    portEXIT_CRITICAL(&_safetyMux);

    executePhysicalCutoff();
    Serial.printf("[SAFETY] EMERGENCY STOP ENGAGED: %s\n", reason.c_str());
}

bool SafetyCore::resetEmergencyStop() {
    portENTER_CRITICAL(&_safetyMux);
    _isEStopLatched = false;
    _eStopReason = "ESTOP_RESET";
    _lastCommandTimestampMs = millis();
    portEXIT_CRITICAL(&_safetyMux);

    Serial.println("[SAFETY] Emergency Stop RESET by operator command.");
    return true;
}

String SafetyCore::getEmergencyStopReason() const {
    return _eStopReason;
}

void SafetyCore::feedDeadmanWatchdog() {
    portENTER_CRITICAL(&_safetyMux);
    _lastCommandTimestampMs = millis();
    portEXIT_CRITICAL(&_safetyMux);
}

bool SafetyCore::isDeadmanExpired(uint32_t nowMs) const {
    uint32_t last = _lastCommandTimestampMs;
    return (nowMs - last) > DRIVE_DEADMAN_TTL_MS;
}

uint32_t SafetyCore::getLastCommandTimestamp() const {
    return _lastCommandTimestampMs;
}

void SafetyCore::executePhysicalCutoff() {
    ActuatorManager::getInstance().emergencyCutoff();
}
