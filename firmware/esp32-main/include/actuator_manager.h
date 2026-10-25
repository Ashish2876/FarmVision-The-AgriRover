#ifndef ACTUATOR_MANAGER_H
#define ACTUATOR_MANAGER_H

#include <Arduino.h>

/**
 * Manages locomotion motors, kinematic servos, and fluid delivery relay.
 * Enforces hardware deadband, slew rate limits, 10s pump auto-cutoff, and emergency cutoff.
 */
class ActuatorManager {
public:
    static ActuatorManager& getInstance() {
        static ActuatorManager instance;
        return instance;
    }

    void init();

    // Locomotion Differential Drive
    bool setDrive(float throttle, float steering);

    // Kinematic Servos
    bool setServo(const String& servoId, float angleDegrees);

    // Fluid Delivery Relay (Max 10s timeout)
    bool startPump(uint32_t durationMs);
    void stopPump();
    void updatePumpSafety(uint32_t nowMs);
    bool isPumpActive() const { return _isPumpActive; }

    // Failsafe Stops
    void stopAllActuators();
    void emergencyCutoff();

private:
    ActuatorManager();

    float _currentLeftSpeed;
    float _currentRightSpeed;

    bool _isPumpActive;
    uint32_t _pumpExpiryMs;

    portMUX_TYPE _actuatorMux;
};

#endif // ACTUATOR_MANAGER_H
