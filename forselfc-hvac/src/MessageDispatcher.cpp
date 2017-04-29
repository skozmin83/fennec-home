//
// Created by sergey on 4/23/2017.
//
#include "IMessageListener.h"
#include "HvacThermostatImpl.h"

enum MessageTypes {
    CONTROL=0, STATUS_REQUEST=1
};

class MessageDispatcher : public IMessageListener {
private:
    IHvacThermostat *thermostat;

    void printOut(char *topic, uint8_t *payload, unsigned int length) {
        Serial.print("Unknown message arrived, topic [");
        Serial.print(topic);
        Serial.print("]. Control message [");
        for (int i = 0; i < length; i++) {
            Serial.print(payload[i]);
        }
    }
public:
    MessageDispatcher(IHvacThermostat *thermostat): thermostat(thermostat) {}

    void onMessage(char *topic, uint8_t *payload, unsigned int length) {
        Serial.print("Dispatching: ");
        Serial.println(payload[0]);
        switch (payload[0]) {
            case '0': thermostat->onControlMessage(payload, length); break;
            case '1': thermostat->onStatusRequestMessage(payload, length); break;
            default:
                printOut(topic, payload, length);
        }
    }
};