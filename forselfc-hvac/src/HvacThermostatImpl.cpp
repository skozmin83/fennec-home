//
// Created by sergey on 4/23/2017.
//

#include <Arduino.h>
#include "HvacThermostatImpl.h"

static const int CONTROL_CMD_INDICATION_TYPE_MS = 1000;
/**
 * for safety purpose we assume we should always receive a heartbeat from control center, if there is none -shut down
 */
static const int MIN_CONTROL_PUBLISH_INTERVAL = 60000;
//static const int MIN_FAN_AFTER_COMPRESSOR = 120000; // 2 mins
static const int MIN_FAN_AFTER_COMPRESSOR = 3000;
static const int PIN_ON = HIGH;
static const int PIN_OFF = LOW;
static const int LIGHT_ON = LOW;
static const int LIGHT_OFF = HIGH;

class HvacThermostatImpl : public IHvacThermostat {
private:
    long lastCommandReceivedTs = 0;
    bool lastCommandIndicationOn = false;
    bool delayedFan = false;
    uint8_t compressorPinState = LOW;
    long lastCompressorOnTs = 0;
    uint8_t currentLedState = HIGH;

public:
    void setup() override {
        pinMode(BUILTIN_LED, OUTPUT);
        ledOff();
        pinMode(FAN_PIN, OUTPUT);
        fanOff();
        pinMode(COMPRESSOR_PIN, OUTPUT);
        pinMode(REVERSE_VALVE_PIN, OUTPUT);
        compressorOff();
    }

    void onControlMessage(uint8_t *payload, unsigned int length) override {
        if (length < sizeof(ControlMessage)) {
            pf("Control message size is too small, ignore [%d]. ", length);
            printOut(payload, length);
            return;
        }
        auto *msg = (struct ControlMessage *) payload;
        switch (msg->mode) {
            case 'A':
                fanOn();
                compressorOff();
                break;
            case 'H':
                fanOn();
                heatingMode();
                compressorOn();
                break;
            case 'C':
                fanOn();
                coolingMode();
                compressorOn();
                break;
            case 'Z':
                delayedFanOff();
                compressorOff();
                break;
            default:
                p("Unexpected heating mode: ");
                pn(msg->mode);
                safeShutdown();
        }

        lastCommandIndicationOn = true;
        lastCommandReceivedTs = millis();
    }

    void loop() override {
        long currentTime = millis();
        // check if we ever heard any command
        // and if we heard last command within the safe interval
        if (lastCommandReceivedTs > 0
            && currentTime - lastCommandReceivedTs > MIN_CONTROL_PUBLISH_INTERVAL) {
            safeShutdown();
        }

        // TODO need to measure temperature function differentiated, should reach stability
        // if delayed fan off set
        // and compressor is off
        // and required time passed after it was off
        // then -> shut down fan
        if (delayedFan
            && compressorPinState == PIN_OFF
            && (currentTime - lastCompressorOnTs > MIN_FAN_AFTER_COMPRESSOR)) {
            fanOff();
        }

        // check if indication is on
        if (lastCommandIndicationOn) {
            if ((currentTime - lastCommandReceivedTs) < CONTROL_CMD_INDICATION_TYPE_MS) {
                ledOn();
            } else {
                // turn off and don't check again
                ledOff();
                lastCommandIndicationOn = false;
            }
        }
    }

private:
    void fanOn() {
        pn("FAN on");
        digitalWrite(FAN_PIN, PIN_ON);
    }
    void fanOff() {
        pn("FAN off");
        digitalWrite(FAN_PIN, PIN_OFF);
        delayedFan = false;
    }

    void delayedFanOff() {
        if (!delayedFan) {
            pfn("Set delayed FAN OFF");
            delayedFan = true;
        }
    }

    void safeShutdown() {
        compressorOff();
        delayedFanOff();
    }

    void coolingMode() {
        pn("COOLING mode/reverse valve ON");
        digitalWrite(REVERSE_VALVE_PIN, PIN_ON);
    }

    void heatingMode() {
        pn("HEATING mode/reverse valve OFF");
        digitalWrite(REVERSE_VALVE_PIN, PIN_OFF);
    }

    void compressorOn() {
        compressorPinState = PIN_ON;
        pn("COMPRESSOR on");
        digitalWrite(COMPRESSOR_PIN, PIN_ON);
    }

    void compressorOff() {
        if (compressorPinState == PIN_ON) {
            lastCompressorOnTs = millis();
            compressorPinState = PIN_OFF;
            heatingMode(); // makes no sense to have reverse valve pin on
            pn("COMPRESSOR off");
            digitalWrite(COMPRESSOR_PIN, PIN_OFF);
        }
    }

    void ledOn() {
        setLight(LIGHT_ON);
    }

    void ledOff() {
        setLight(LIGHT_OFF);
    }

    void setLight(uint8_t onOff) {
        if (onOff != currentLedState) {
//            p("led: ");
//            pn(onOff == LIGHT_OFF ? "OFF" : "ON");
            digitalWrite(BUILTIN_LED, onOff);
            currentLedState = onOff;
        }
    }

    void printOut(uint8_t *payload, unsigned int length) {
        p("Message [");
        for (int i = 0; i < length; i++) {
            pf("%x", payload[i]);
        }
        pn("]. ");
    }
};