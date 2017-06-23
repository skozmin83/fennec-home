//
// Created by sergey on 4/23/2017.
//
#include "IMessageListener.h"
#include "HvacThermostatImpl.h"

typedef struct Message {
    /**
     * could be:
     *      C -> Control Message
     *      S -> StatusRequest Message
     */
    uint8_t messageType;
} Message;

class MessageDispatcher : public IMessageListener {
private:
    IHvacThermostat *thermostat;

    void printOut(char *topic, uint8_t *payload, unsigned int length) {
        pf("Message on topic [%s]. Content [", topic);
        for (int i = 0; i < length; i++) {
            p((char)payload[i]);
        }
        pn("]. ");
    }
public:
    explicit MessageDispatcher(IHvacThermostat *thermostat): thermostat(thermostat) {}

    void onMessage(char *topic, uint8_t *payload, unsigned int length) override {
        printOut(topic, payload, length);
        switch (payload[0]) {
            case 'C': thermostat->onControlMessage(payload, length); break;
            default:
                p("Ignore message of type: ");
                pn((char)payload[0]);
        }
    }
};