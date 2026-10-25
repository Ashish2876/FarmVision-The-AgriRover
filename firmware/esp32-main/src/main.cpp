#include <Arduino.h>
#include <WiFi.h>
#include "config.h"
#include "safety_core.h"
#include "sensor_manager.h"
#include "actuator_manager.h"
#include "web_server_handler.h"

// =============================================================================
// FreeRTOS Task Handles
// =============================================================================
TaskHandle_t TaskHandle_Sensing = nullptr;
TaskHandle_t TaskHandle_Comm = nullptr;
TaskHandle_t TaskHandle_SafetyWatchdog = nullptr;

// =============================================================================
// Task Definitions
// =============================================================================

/**
 * Task 1: Sensor Acquisition Task (Core 1, 10 Hz)
 * Periodically samples analog capacitive soil moisture, DHT22, soil pH, and ultrasonic distance.
 */
void TaskSensing(void* pvParameters) {
    TickType_t xLastWakeTime = xTaskGetTickCount();
    const TickType_t xFrequency = pdMS_TO_TICKS(100); // 100ms = 10 Hz

    for (;;) {
        SensorManager::getInstance().sampleAllSensors();
        vTaskDelayUntil(&xLastWakeTime, xFrequency);
    }
}

/**
 * Task 2: Safety & Deadman Watchdog Task (Core 1, 20 Hz)
 * Monitors 250ms deadman timer, updates 10s pump auto-cutoff, and executes failsafe shutdown if packet stream is lost.
 */
void TaskSafetyWatchdog(void* pvParameters) {
    TickType_t xLastWakeTime = xTaskGetTickCount();
    const TickType_t xFrequency = pdMS_TO_TICKS(50); // 50ms = 20 Hz

    for (;;) {
        uint32_t now = millis();
        // 1. Pump relay auto-cutoff check
        ActuatorManager::getInstance().updatePumpSafety(now);

        // 2. Deadman failsafe (250 ms)
        if (SafetyCore::getInstance().isDeadmanExpired(now)) {
            // Cut all actuator power if deadman expires
            ActuatorManager::getInstance().stopAllActuators();
        }
        vTaskDelayUntil(&xLastWakeTime, xFrequency);
    }
}

/**
 * Task 3: Network & REST Communications Task (Core 0, 50 Hz)
 * Dispatches client HTTP requests over the Wi-Fi SoftAP interface.
 */
void TaskComm(void* pvParameters) {
    TickType_t xLastWakeTime = xTaskGetTickCount();
    const TickType_t xFrequency = pdMS_TO_TICKS(20); // 20ms = 50 Hz

    for (;;) {
        WebServerHandler::getInstance().handleClient();
        vTaskDelayUntil(&xLastWakeTime, xFrequency);
    }
}

// =============================================================================
// Arduino Setup & Main Loop
// =============================================================================

void setup() {
    Serial.begin(115200);
    delay(500);

    Serial.println("\n========================================================");
    Serial.println("  FarmVision AgriRover — Main ESP32 DevKit V1 Firmware  ");
    Serial.printf("  Firmware: %s | Stage: %s | Target: %s\n", FIRMWARE_VERSION, FIRMWARE_STAGE, CONTROLLER_TARGET);
    Serial.println("========================================================");

    // 1. Initialize Safety Core
    SafetyCore::getInstance().init();
    Serial.println("[INIT] Safety core initialized. Emergency Stop STANDBY.");

    // 2. Initialize Environmental Sensors
    SensorManager::getInstance().init();
    Serial.println("[INIT] Sensor manager initialized.");

    // 3. Initialize Actuator Manager (Stage F0/F1)
    ActuatorManager::getInstance().init();

    // 4. Configure Wi-Fi SoftAP
    Serial.println("[INIT] Configuring Wi-Fi SoftAP...");
    WiFi.mode(WIFI_AP);
    WiFi.softAPConfig(SOFTAP_LOCAL_IP, SOFTAP_GATEWAY, SOFTAP_SUBNET);
    bool apStarted = WiFi.softAP(SOFTAP_SSID, SOFTAP_PASSWORD, SOFTAP_CHANNEL, 0, SOFTAP_MAX_CLIENTS);

    if (apStarted) {
        Serial.printf("[WIFI] SoftAP Active! SSID: '%s', IP: %s\n",
                      SOFTAP_SSID, WiFi.softAPIP().toString().c_str());
    } else {
        Serial.println("[ERROR] Failed to start SoftAP!");
    }

    // 4. Initialize REST WebServer
    WebServerHandler::getInstance().init();

    // 5. Spawn FreeRTOS Multitasking Threads
    Serial.println("[RTOS] Spawning FreeRTOS tasks across ESP32 cores...");

    // Core 1: Sensing Task (Priority 2)
    xTaskCreatePinnedToCore(
        TaskSensing,
        "TaskSensing",
        4096,
        nullptr,
        2,
        &TaskHandle_Sensing,
        1
    );

    // Core 1: Safety Watchdog (Priority 3 - High)
    xTaskCreatePinnedToCore(
        TaskSafetyWatchdog,
        "TaskSafety",
        2048,
        nullptr,
        3,
        &TaskHandle_SafetyWatchdog,
        1
    );

    // Core 0: Comm & HTTP Server (Priority 1)
    xTaskCreatePinnedToCore(
        TaskComm,
        "TaskComm",
        8192,
        nullptr,
        1,
        &TaskHandle_Comm,
        0
    );

    Serial.println("[READY] AgriRover ESP32 firmware running. Awaiting client connections.");
}

void loop() {
    // Arduino loop task remains idle; FreeRTOS handles multitasking
    vTaskDelay(pdMS_TO_TICKS(1000));
}
