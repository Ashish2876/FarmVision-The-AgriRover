#ifndef SENSOR_MANAGER_H
#define SENSOR_MANAGER_H

#include <Arduino.h>
#include <DHT.h>

struct SensorReading {
    float value;
    float rawValue;
    const char* unit;
    bool isValid;
    const char* quality;
};

struct RoverTelemetryData {
    uint64_t sequenceNumber;
    uint32_t uptimeMs;
    SensorReading soilMoisture;
    SensorReading soilMoisture2;
    SensorReading dhtTemperature;
    SensorReading dhtHumidity;
    SensorReading soilPh;
    SensorReading ultrasonicDistance;
    uint32_t freeHeapBytes;
    int8_t wifiRssiDbm;
    uint16_t loopFrequencyHz;
};

class SensorManager {
public:
    static SensorManager& getInstance() {
        static SensorManager instance;
        return instance;
    }

    void init();
    void sampleAllSensors();
    RoverTelemetryData getTelemetrySnapshot();

private:
    SensorManager();
    DHT _dht;
    RoverTelemetryData _cache;
    SemaphoreHandle_t _mutex;

    float sampleSoilMoisture(float& outRaw);
    float sampleSoilMoistureProbe2(float& outRaw);
    float sampleSoilPh(float& outRaw);
    float sampleUltrasonicDistance(float& outRaw);
};

#endif // SENSOR_MANAGER_H
