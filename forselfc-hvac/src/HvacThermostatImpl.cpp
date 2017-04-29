//
// Created by sergey on 4/23/2017.
//

#include <Arduino.h>
#include "HvacThermostatImpl.h"

static const int CONTROL_CMD_INDICATION_TYPE_MS = 1000;
static const int CONTROL_ON = LOW;
static const int CONTROL_OFF = HIGH;

class HvacThermostatImpl : public IHvacThermostat {
private:
    volatile uint32_t lastCommandReceived = 0;
    volatile bool lastCommandIndicationOn = false;
    uint8 currentLedState = HIGH;
private:
    void controlOn() {
        Serial.println("control on");
        digitalWrite(CONTROL_PIN, CONTROL_ON);
    }

    void controlOff() {
        Serial.println("control off");
        digitalWrite(CONTROL_PIN, CONTROL_OFF);
    }

    void lightOn() {
        setLight(LOW);
    }

    void lightOff() {
        setLight(HIGH);
    }

    void setLight(uint8 onOff) {
        if (onOff != currentLedState) {
            Serial.print("led ");
            Serial.println(onOff ? "off" : "on");
            digitalWrite(BUILTIN_LED, onOff);
            currentLedState = onOff;
        }
    }

    void printOut(uint8_t *payload, unsigned int length) {
        Serial.print("Message [");
        for (int i = 0; i < length; i++) {
            Serial.print(payload[i], HEX);
        }
        Serial.print("]. ");
    }
public:
    void setup() {
        pinMode(BUILTIN_LED, OUTPUT);
        lightOff();
        pinMode(CONTROL_PIN, OUTPUT);
        controlOff();
    }

    void onControlMessage(uint8_t *payload, unsigned int length) {
        if (length < 2) {
            Serial.print("Control message size is too small, ignore [");
            Serial.print(length);
            Serial.print("]. ");
            printOut(payload, length);
            return;
        }
        Serial.print("Control message [");
        for (int i = 0; i < length; i++) {
            Serial.print(payload[i], HEX);
        }
        Serial.print("]. Control bit [");
        Serial.print(payload[1], HEX);
        Serial.println("]");

        if (payload[1] == '0') {
            controlOff();
        } else if (payload[1] >= '0') {
            controlOn();
        }
        lastCommandIndicationOn = true;
        lastCommandReceived = millis();
    }

    void onStatusRequestMessage(uint8_t *payload, unsigned int length) {
        Serial.print("Status request message [");
        for (int i = 0; i < length; i++) {
            Serial.print(payload[i], HEX);
        }
        Serial.println("]");
        lastCommandIndicationOn = true;
        lastCommandReceived = millis();
    }

    void loop() {
        if (lastCommandIndicationOn) { // check if indication is on
            uint32_t currentTime = millis();
//            Serial.print("current-time: ");
//            Serial.print(currentTime);
//            Serial.print(", lastcommand: ");
//            Serial.println(lastCommandReceived);
            if ((currentTime - lastCommandReceived) < CONTROL_CMD_INDICATION_TYPE_MS) {
                lightOn();
            } else {
                // turn off and don't check again
                lightOff();
                lastCommandIndicationOn = false;
            }
        }
    }
};