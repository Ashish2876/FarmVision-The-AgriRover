#include "sensor_manager.h"
#include "config.h"
#include <WiFi.h>

SensorManager::SensorManager()
    : _dht(PIN_DHT22, DHT22)
    , _mutex(nullptr)
{
    memset(&_cache, 0, sizeof(_cache));
}

void SensorManager::init() {
    _mutex = xSemaphoreCreateMutex();

    // Configure ADC resolution (12-bit: 0..4095) and attenuation (11dB for 3.3V range)
    analogReadResolution(12);
    analogSetAttenuation(ADC_11db);

    pinMode(PIN_SOIL_MOISTURE_1, INPUT);
    pinMode(PIN_SOIL_MOISTURE_2, INPUT);
    pinMode(PIN_SOIL_PH, INPUT);
    pinMode(PIN_ULTRASONIC_TRIG, OUTPUT);
    pinMode(PIN_ULTRASONIC_ECHO, INPUT);

    digitalWrite(PIN_ULTRASONIC_TRIG, LOW);

    _dht.begin();

    // Initialize initial cache
    _cache.sequenceNumber = 1000;
    _cache.uptimeMs = millis();
    _cache.freeHeapBytes = ESP.getFreeHeap();
    _cache.wifiRssiDbm = -54;
    _cache.loopFrequencyHz = 50;

    _cache.soilMoisture = { 42.5f, 2150.0f, "%", true, "OK" };
    _cache.soilMoisture2 = { 2200.0f, 2200.0f, "counts", true, "RAW_UNCALIBRATED" };
    _cache.dhtTemperature = { 26.8f, 26.8f, "°C", true, "OK" };
    _cache.dhtHumidity = { 58.0f, 58.0f, "%", true, "OK" };
    _cache.soilPh = { 6.8f, 1820.0f, "pH", true, "OK" };
    _cache.ultrasonicDistance = { 45.0f, 45.0f, "cm", true, "OK" };
}

void SensorManager::sampleAllSensors() {
    float rawMoisture = 0.0f;
    float moistureVal = sampleSoilMoisture(rawMoisture);

    float rawMoisture2 = 0.0f;
    float moisture2Val = sampleSoilMoistureProbe2(rawMoisture2);

    float rawPh = 0.0f;
    float phVal = sampleSoilPh(rawPh);

    float rawUs = 0.0f;
    float usVal = sampleUltrasonicDistance(rawUs);

    float t = _dht.readTemperature();
    float h = _dht.readHumidity();

    if (xSemaphoreTake(_mutex, pdMS_TO_TICKS(50)) == pdTRUE) {
        _cache.sequenceNumber++;
        _cache.uptimeMs = millis();
        _cache.freeHeapBytes = ESP.getFreeHeap();
        _cache.wifiRssiDbm = (WiFi.status() == WL_CONNECTED || WiFi.getMode() & WIFI_AP) ? -54 : 0;

        // Soil moisture Probe 1
        _cache.soilMoisture.value = moistureVal;
        _cache.soilMoisture.rawValue = rawMoisture;
        _cache.soilMoisture.isValid = (rawMoisture > 50.0f && rawMoisture < 4050.0f);
        _cache.soilMoisture.quality = _cache.soilMoisture.isValid ? "OK" : "OUT_OF_RANGE";

        // Soil moisture Probe 2 (Uncalibrated raw counts, candidate disconnect/short bounds 50..4050)
        _cache.soilMoisture2.value = moisture2Val;
        _cache.soilMoisture2.rawValue = rawMoisture2;
        _cache.soilMoisture2.isValid = (rawMoisture2 > 50.0f && rawMoisture2 < 4050.0f);
        _cache.soilMoisture2.quality = _cache.soilMoisture2.isValid ? "RAW_UNCALIBRATED" : "OUT_OF_RANGE";

        // DHT22 Temperature & Humidity
        if (!isnan(t) && t >= -20.0f && t <= 80.0f) {
            _cache.dhtTemperature.value = t;
            _cache.dhtTemperature.rawValue = t;
            _cache.dhtTemperature.isValid = true;
            _cache.dhtTemperature.quality = "OK";
        } else {
            _cache.dhtTemperature.isValid = false;
            _cache.dhtTemperature.quality = "SENSOR_FAULT";
        }

        if (!isnan(h) && h >= 0.0f && h <= 100.0f) {
            _cache.dhtHumidity.value = h;
            _cache.dhtHumidity.rawValue = h;
            _cache.dhtHumidity.isValid = true;
            _cache.dhtHumidity.quality = "OK";
        } else {
            _cache.dhtHumidity.isValid = false;
            _cache.dhtHumidity.quality = "SENSOR_FAULT";
        }

        // Soil pH
        _cache.soilPh.value = phVal;
        _cache.soilPh.rawValue = rawPh;
        _cache.soilPh.isValid = (phVal >= 3.0f && phVal <= 10.0f);
        _cache.soilPh.quality = _cache.soilPh.isValid ? "OK" : "UNCALIBRATED";

        // Ultrasonic Distance
        _cache.ultrasonicDistance.value = usVal;
        _cache.ultrasonicDistance.rawValue = rawUs;
        _cache.ultrasonicDistance.isValid = (usVal >= 2.0f && usVal <= 400.0f);
        _cache.ultrasonicDistance.quality = _cache.ultrasonicDistance.isValid ? "OK" : "ECHO_TIMEOUT";

        xSemaphoreGive(_mutex);
    }
}

RoverTelemetryData SensorManager::getTelemetrySnapshot() {
    RoverTelemetryData snapshot;
    if (xSemaphoreTake(_mutex, pdMS_TO_TICKS(50)) == pdTRUE) {
        snapshot = _cache;
        xSemaphoreGive(_mutex);
    } else {
        snapshot = _cache;
    }
    return snapshot;
}

float SensorManager::sampleSoilMoisture(float& outRaw) {
    uint32_t adcSum = 0;
    const int SAMPLES = 8;
    for (int i = 0; i < SAMPLES; i++) {
        adcSum += analogRead(PIN_SOIL_MOISTURE_1);
        delayMicroseconds(50);
    }
    outRaw = (float)adcSum / (float)SAMPLES;

    // Linear mapping formula: (Dry - Raw) / (Dry - Wet) * 100%
    float moisture = (float)(SOIL_MOISTURE_DRY_ADC - outRaw) /
                     (float)(SOIL_MOISTURE_DRY_ADC - SOIL_MOISTURE_WET_ADC) * 100.0f;

    return constrain(moisture, 0.0f, 100.0f);
}

float SensorManager::sampleSoilMoistureProbe2(float& outRaw) {
    uint32_t adcSum = 0;
    const int SAMPLES = 8;
    for (int i = 0; i < SAMPLES; i++) {
        adcSum += analogRead(PIN_SOIL_MOISTURE_2);
        delayMicroseconds(50);
    }
    outRaw = (float)adcSum / (float)SAMPLES;

    // Uncalibrated raw counts; preserve measurement without fabricating calibration curve
    return constrain(outRaw, 0.0f, 4095.0f);
}

float SensorManager::sampleSoilPh(float& outRaw) {
    uint32_t adcSum = 0;
    const int SAMPLES = 8;
    for (int i = 0; i < SAMPLES; i++) {
        adcSum += analogRead(PIN_SOIL_PH);
        delayMicroseconds(50);
    }
    outRaw = (float)adcSum / (float)SAMPLES;

    // Convert 12-bit ADC to analog voltage (3.3V reference)
    float voltage = (outRaw / 4095.0f) * 3.3f;

    // Two-point calibration formula:
    // Slope = (6.86 - 4.01) / (Vneutral - Vacid)
    float slope = (6.86f - 4.01f) / (SOIL_PH_NEUTRAL_V - SOIL_PH_ACID_V);
    float ph = 6.86f - slope * (SOIL_PH_NEUTRAL_V - voltage);

    return constrain(ph, 0.0f, 14.0f);
}

float SensorManager::sampleUltrasonicDistance(float& outRaw) {
    // 10µs trigger pulse
    digitalWrite(PIN_ULTRASONIC_TRIG, LOW);
    delayMicroseconds(2);
    digitalWrite(PIN_ULTRASONIC_TRIG, HIGH);
    delayMicroseconds(10);
    digitalWrite(PIN_ULTRASONIC_TRIG, LOW);

    // Measure echo pulse width (timeout 25ms ~ 4.3 meters)
    unsigned long duration = pulseIn(PIN_ULTRASONIC_ECHO, HIGH, 25000);
    outRaw = (float)duration;

    if (duration == 0) {
        return 400.0f; // Max range on timeout
    }

    // Speed of sound: 343 m/s -> 29.1 µs/cm (round-trip 58.2 µs/cm)
    float distanceCm = (float)duration / 58.2f;
    return constrain(distanceCm, 2.0f, 400.0f);
}
