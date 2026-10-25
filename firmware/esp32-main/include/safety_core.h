#ifndef SAFETY_CORE_H
#define SAFETY_CORE_H

#include <Arduino.h>

/**
 * High-priority safety watchdog and Emergency Stop latching core.
 */
class SafetyCore {
public:
    static SafetyCore& getInstance() {
        static SafetyCore instance;
        return instance;
    }

    void init();

    // Emergency Stop
    bool isEmergencyStopActive() const;
    void triggerEmergencyStop(const String& reason);
    bool resetEmergencyStop();
    String getEmergencyStopReason() const;

    // Deadman Failsafe (250 ms TTL)
    void feedDeadmanWatchdog();
    bool isDeadmanExpired(uint32_t nowMs) const;
    uint32_t getLastCommandTimestamp() const;

    // Cutoff execution
    void executePhysicalCutoff();

private:
    SafetyCore();
    volatile bool _isEStopLatched;
    String _eStopReason;
    volatile uint32_t _lastCommandTimestampMs;
    portMUX_TYPE _safetyMux;
};

#endif // SAFETY_CORE_H
