#ifndef WEB_SERVER_HANDLER_H
#define WEB_SERVER_HANDLER_H

#include <Arduino.h>
#include <WebServer.h>

class WebServerHandler {
public:
    static WebServerHandler& getInstance() {
        static WebServerHandler instance;
        return instance;
    }

    void init();
    void handleClient();

private:
    WebServerHandler();
    WebServer _server;

    // Route Handlers
    void handleCapabilities();
    void handleTelemetry();
    void handleStatus();
    void handleCommand();
    void handleEmergencyStop();
    void handleEmergencyStopReset();
    void handleNotFound();

    // Helper functions
    bool validateAuthToken();
    void sendJsonError(int httpStatus, const char* errorCode, const char* message, bool recoverable);

    uint32_t _lastEStopLogMs;
};

#endif // WEB_SERVER_HANDLER_H
