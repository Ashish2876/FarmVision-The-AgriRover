#ifndef CONFIG_H
#define CONFIG_H

#include <Arduino.h>

// =============================================================================
// 1. Protocol & Firmware Identity
// =============================================================================
#define PROTOCOL_VERSION            "1.0.0"
#define DEVICE_ID                   "agrirover-main-esp32"
#define CONTROLLER_TARGET           "Esp32DevKitV1"
#define DEFAULT_AUTH_TOKEN          "rover-token-secret"

// Stage Control: In F0, actuators compiled disabled (default)
// In F1, ACTUATORS_ENABLED is set to 1 after physical wiring confirmation
#ifndef ACTUATORS_ENABLED
#define ACTUATORS_ENABLED           0
#endif

#if ACTUATORS_ENABLED
#define FIRMWARE_STAGE              "F1"
#define FIRMWARE_VERSION            "v1.0.0-f1-actuators"
#else
#define FIRMWARE_STAGE              "F0"
#define FIRMWARE_VERSION            "v1.0.0-f0-sensors"
#endif

// =============================================================================
// 2. Wi-Fi SoftAP Configuration
// =============================================================================
#define SOFTAP_SSID                 "AgriRover-AP"
#define SOFTAP_PASSWORD             "AgriRover2026"
#define SOFTAP_CHANNEL              6
#define SOFTAP_MAX_CLIENTS          4
#define SERVER_PORT                 80

// Static SoftAP IP addressing (192.168.4.1/24)
const IPAddress SOFTAP_LOCAL_IP(192, 168, 4, 1);
const IPAddress SOFTAP_GATEWAY(192, 168, 4, 1);
const IPAddress SOFTAP_SUBNET(255, 255, 255, 0);

// =============================================================================
// 3. Sensor Pin Assignments (Candidate Draft — docs/hardware-wiring-checklist.md)
// =============================================================================
// Capacitive Soil Moisture Probes (ADC1 pins only, 3.3V safe)
#define PIN_SOIL_MOISTURE_1         32  // ADC1 Channel 4
#define PIN_SOIL_MOISTURE_2         34  // ADC1 Channel 6 (Input Only)

// DHT22 Atmospheric Sensor (Single bus, 4.7k pull-up to 3.3V)
#define PIN_DHT22                   4

// Analog Soil pH Probe (ADC1 pin, Input Only)
#define PIN_SOIL_PH                 35  // ADC1 Channel 7 (Input Only)

// HC-SR04 Ultrasonic Obstacle Sensor
#define PIN_ULTRASONIC_TRIG         13  // 3.3V Trigger Pulse Output
#define PIN_ULTRASONIC_ECHO         14  // 5V Echo input - MUST HAVE 1k/2k VOLTAGE DIVIDER!

// =============================================================================
// 4. Actuator Pin Assignments (TB6612FNG, Servos, Pump Relay)
// =============================================================================
// TB6612FNG Dual H-Bridge Motor Driver
#define PIN_MOTOR_PWMA              25  // Left Speed (Hardware PWM)
#define PIN_MOTOR_AIN1              26  // Left Dir 1
#define PIN_MOTOR_AIN2              27  // Left Dir 2
#define PIN_MOTOR_PWMB              16  // Right Speed (Hardware PWM)
#define PIN_MOTOR_BIN1              17  // Right Dir 1
#define PIN_MOTOR_BIN2              18  // Right Dir 2
#define PIN_MOTOR_STBY              19  // Standby (Active HIGH)

// Kinematic Servos (50 Hz PWM)
#define PIN_SERVO_CAM               21  // Camera Tilt Servo
#define PIN_SERVO_PROBE             22  // Soil Probe Insertion Servo
#define PIN_SERVO_US                23  // Ultrasonic Pan Servo

// Fluid Delivery / Irrigation Relay
#define PIN_PUMP_RELAY              5   // Relay Ch 1 (Active LOW trigger)

// Hardware PWM Channels & Frequency
#define PWM_CHANNEL_MOTOR_LEFT      0
#define PWM_CHANNEL_MOTOR_RIGHT     1
#define PWM_CHANNEL_SERVO_CAM       2
#define PWM_CHANNEL_SERVO_PROBE     3
#define PWM_CHANNEL_SERVO_US        4

#define MOTOR_PWM_FREQUENCY_HZ      20000 // 20 kHz ultrasonic (silent operation)
#define MOTOR_PWM_RESOLUTION_BITS   8     // 0..255 duty cycle
#define SERVO_PWM_FREQUENCY_HZ      50    // 50 Hz standard hobby servo
#define SERVO_PWM_RESOLUTION_BITS   16    // 16-bit resolution

// =============================================================================
// 5. Calibration & Timing Constants
// =============================================================================
// Capacitive moisture 2-point calibration (12-bit ADC 0..4095)
#define SOIL_MOISTURE_DRY_ADC       3000
#define SOIL_MOISTURE_WET_ADC       1400

// Soil pH 2-point calibration (volts)
#define SOIL_PH_NEUTRAL_V           2.50f
#define SOIL_PH_ACID_V              1.80f

// Safety Watchdog Limits
#define DRIVE_DEADMAN_TTL_MS        250   // Continuous drive packet timeout
#define MAX_PUMP_DURATION_MS        10000 // 10s auto-cutoff on irrigation relay
#define MOTOR_SLEW_RATE_LIMIT       0.20f // Max duty cycle step per loop (voltage sag prevention)

#endif // CONFIG_H
