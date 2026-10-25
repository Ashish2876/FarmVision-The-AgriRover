#include "actuator_manager.h"
#include "config.h"
#include "safety_core.h"
#include <math.h>

ActuatorManager::ActuatorManager()
    : _currentLeftSpeed(0.0f)
    , _currentRightSpeed(0.0f)
    , _isPumpActive(false)
    , _pumpExpiryMs(0)
    , _actuatorMux(portMUX_INITIALIZER_UNLOCKED)
{
}

void ActuatorManager::init() {
#if ACTUATORS_ENABLED
    // 1. TB6612FNG Direction & Standby Pins
    pinMode(PIN_MOTOR_AIN1, OUTPUT);
    pinMode(PIN_MOTOR_AIN2, OUTPUT);
    pinMode(PIN_MOTOR_BIN1, OUTPUT);
    pinMode(PIN_MOTOR_BIN2, OUTPUT);
    pinMode(PIN_MOTOR_STBY, OUTPUT);

    digitalWrite(PIN_MOTOR_AIN1, LOW);
    digitalWrite(PIN_MOTOR_AIN2, LOW);
    digitalWrite(PIN_MOTOR_BIN1, LOW);
    digitalWrite(PIN_MOTOR_BIN2, LOW);
    digitalWrite(PIN_MOTOR_STBY, LOW);

    // 2. TB6612FNG Hardware PWM Channels
    ledcSetup(PWM_CHANNEL_MOTOR_LEFT, MOTOR_PWM_FREQUENCY_HZ, MOTOR_PWM_RESOLUTION_BITS);
    ledcAttachPin(PIN_MOTOR_PWMA, PWM_CHANNEL_MOTOR_LEFT);
    ledcWrite(PWM_CHANNEL_MOTOR_LEFT, 0);

    ledcSetup(PWM_CHANNEL_MOTOR_RIGHT, MOTOR_PWM_FREQUENCY_HZ, MOTOR_PWM_RESOLUTION_BITS);
    ledcAttachPin(PIN_MOTOR_PWMB, PWM_CHANNEL_MOTOR_RIGHT);
    ledcWrite(PWM_CHANNEL_MOTOR_RIGHT, 0);

    // 3. Servos Hardware PWM Channels (50 Hz, 16-bit)
    ledcSetup(PWM_CHANNEL_SERVO_CAM, SERVO_PWM_FREQUENCY_HZ, SERVO_PWM_RESOLUTION_BITS);
    ledcAttachPin(PIN_SERVO_CAM, PWM_CHANNEL_SERVO_CAM);

    ledcSetup(PWM_CHANNEL_SERVO_PROBE, SERVO_PWM_FREQUENCY_HZ, SERVO_PWM_RESOLUTION_BITS);
    ledcAttachPin(PIN_SERVO_PROBE, PWM_CHANNEL_SERVO_PROBE);

    ledcSetup(PWM_CHANNEL_SERVO_US, SERVO_PWM_FREQUENCY_HZ, SERVO_PWM_RESOLUTION_BITS);
    ledcAttachPin(PIN_SERVO_US, PWM_CHANNEL_SERVO_US);

    // Center servos on boot
    setServo("hw-servo-camera", 90.0f);
    setServo("hw-servo-soil-probe", 0.0f); // Retracted
    setServo("hw-servo-ultrasonic", 90.0f);

    // 4. Irrigation Pump Relay (Active LOW -> HIGH = OFF / Contacts open)
    pinMode(PIN_PUMP_RELAY, OUTPUT);
    digitalWrite(PIN_PUMP_RELAY, HIGH);

    Serial.println("[ACTUATOR] Stage F1 Actuators initialized (TB6612FNG, 3 Servos, Pump Relay).");
#else
    Serial.println("[ACTUATOR] Stage F0: Actuators compiled disabled.");
#endif
}

bool ActuatorManager::setDrive(float throttle, float steering) {
#if (ACTUATORS_ENABLED == 0)
    return false; // Rejected in F0
#else
    if (SafetyCore::getInstance().isEmergencyStopActive()) {
        stopAllActuators();
        return false;
    }

    portENTER_CRITICAL(&_actuatorMux);

    // Differential Drive Kinematics:
    // Left = throttle + steering, Right = throttle - steering
    float targetLeft = constrain(throttle + steering, -1.0f, 1.0f);
    float targetRight = constrain(throttle - steering, -1.0f, 1.0f);

    // Deadband threshold (5%)
    if (fabs(targetLeft) < 0.05f) targetLeft = 0.0f;
    if (fabs(targetRight) < 0.05f) targetRight = 0.0f;

    // Slew rate limiting to prevent battery voltage sag
    float deltaLeft = targetLeft - _currentLeftSpeed;
    float deltaRight = targetRight - _currentRightSpeed;

    _currentLeftSpeed += constrain(deltaLeft, -MOTOR_SLEW_RATE_LIMIT, MOTOR_SLEW_RATE_LIMIT);
    _currentRightSpeed += constrain(deltaRight, -MOTOR_SLEW_RATE_LIMIT, MOTOR_SLEW_RATE_LIMIT);

    // Enable motor driver if active, or enter standby
    if (_currentLeftSpeed != 0.0f || _currentRightSpeed != 0.0f) {
        digitalWrite(PIN_MOTOR_STBY, HIGH);
    } else {
        digitalWrite(PIN_MOTOR_STBY, LOW);
    }

    // Left Motor Direction & PWM
    if (_currentLeftSpeed > 0.0f) {
        digitalWrite(PIN_MOTOR_AIN1, HIGH);
        digitalWrite(PIN_MOTOR_AIN2, LOW);
    } else if (_currentLeftSpeed < 0.0f) {
        digitalWrite(PIN_MOTOR_AIN1, LOW);
        digitalWrite(PIN_MOTOR_AIN2, HIGH);
    } else {
        digitalWrite(PIN_MOTOR_AIN1, LOW);
        digitalWrite(PIN_MOTOR_AIN2, LOW);
    }
    uint32_t leftDuty = (uint32_t)(fabs(_currentLeftSpeed) * 255.0f);
    ledcWrite(PWM_CHANNEL_MOTOR_LEFT, leftDuty);

    // Right Motor Direction & PWM
    if (_currentRightSpeed > 0.0f) {
        digitalWrite(PIN_MOTOR_BIN1, HIGH);
        digitalWrite(PIN_MOTOR_BIN2, LOW);
    } else if (_currentRightSpeed < 0.0f) {
        digitalWrite(PIN_MOTOR_BIN1, LOW);
        digitalWrite(PIN_MOTOR_BIN2, HIGH);
    } else {
        digitalWrite(PIN_MOTOR_BIN1, LOW);
        digitalWrite(PIN_MOTOR_BIN2, LOW);
    }
    uint32_t rightDuty = (uint32_t)(fabs(_currentRightSpeed) * 255.0f);
    ledcWrite(PWM_CHANNEL_MOTOR_RIGHT, rightDuty);

    portEXIT_CRITICAL(&_actuatorMux);
    return true;
#endif
}

bool ActuatorManager::setServo(const String& servoId, float angleDegrees) {
#if (ACTUATORS_ENABLED == 0)
    return false;
#else
    if (SafetyCore::getInstance().isEmergencyStopActive()) {
        return false;
    }

    float angle = constrain(angleDegrees, 0.0f, 180.0f);
    // Standard 50Hz (20ms): 0.5ms (1638) to 2.5ms (8192) in 16-bit counts
    uint32_t duty = 1638 + (uint32_t)((angle / 180.0f) * (8192 - 1638));

    if (servoId == "hw-servo-camera" || servoId == "hw-servo-camera-tilt") {
        ledcWrite(PWM_CHANNEL_SERVO_CAM, duty);
        return true;
    } else if (servoId == "hw-servo-soil-probe") {
        ledcWrite(PWM_CHANNEL_SERVO_PROBE, duty);
        return true;
    } else if (servoId == "hw-servo-ultrasonic") {
        ledcWrite(PWM_CHANNEL_SERVO_US, duty);
        return true;
    }

    return false;
#endif
}

bool ActuatorManager::startPump(uint32_t durationMs) {
#if (ACTUATORS_ENABLED == 0)
    return false;
#else
    if (SafetyCore::getInstance().isEmergencyStopActive()) {
        stopPump();
        return false;
    }

    uint32_t safeDuration = min(durationMs, (uint32_t)MAX_PUMP_DURATION_MS);
    if (safeDuration == 0) {
        stopPump();
        return true;
    }

    portENTER_CRITICAL(&_actuatorMux);
    _isPumpActive = true;
    _pumpExpiryMs = millis() + safeDuration;
    // Active LOW relay -> write LOW to close contacts and energize pump
    digitalWrite(PIN_PUMP_RELAY, LOW);
    portEXIT_CRITICAL(&_actuatorMux);

    Serial.printf("[PUMP] Pump relay energized for %d ms (Auto-cutoff enabled)\n", safeDuration);
    return true;
#endif
}

void ActuatorManager::stopPump() {
#if ACTUATORS_ENABLED
    portENTER_CRITICAL(&_actuatorMux);
    // Active LOW relay -> write HIGH to open contacts
    digitalWrite(PIN_PUMP_RELAY, HIGH);
    _isPumpActive = false;
    _pumpExpiryMs = 0;
    portEXIT_CRITICAL(&_actuatorMux);
    Serial.println("[PUMP] Pump relay de-energized (Contacts open).");
#endif
}

void ActuatorManager::updatePumpSafety(uint32_t nowMs) {
#if ACTUATORS_ENABLED
    if (_isPumpActive && nowMs >= _pumpExpiryMs) {
        Serial.println("[SAFETY] Pump auto-cutoff timer expired. Cutting pump relay power.");
        stopPump();
    }
#endif
}

void ActuatorManager::stopAllActuators() {
#if ACTUATORS_ENABLED
    portENTER_CRITICAL(&_actuatorMux);
    _currentLeftSpeed = 0.0f;
    _currentRightSpeed = 0.0f;

    digitalWrite(PIN_MOTOR_STBY, LOW);
    digitalWrite(PIN_MOTOR_AIN1, LOW);
    digitalWrite(PIN_MOTOR_AIN2, LOW);
    digitalWrite(PIN_MOTOR_BIN1, LOW);
    digitalWrite(PIN_MOTOR_BIN2, LOW);

    ledcWrite(PWM_CHANNEL_MOTOR_LEFT, 0);
    ledcWrite(PWM_CHANNEL_MOTOR_RIGHT, 0);

    digitalWrite(PIN_PUMP_RELAY, HIGH);
    _isPumpActive = false;
    portEXIT_CRITICAL(&_actuatorMux);
#endif
}

void ActuatorManager::emergencyCutoff() {
    stopAllActuators();
}
