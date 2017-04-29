//
// Created by sergey on 4/23/2017.
//

#ifndef FORSELFC_HVAC_IHVACTHERMOSTAT_H
#define FORSELFC_HVAC_IHVACTHERMOSTAT_H


static const int CONTROL_PIN = D6;

#include <stdint.h>

class IHvacThermostat {
public:
    virtual void setup() = 0;
    virtual void onControlMessage(uint8_t *payload, unsigned int length) = 0;
    virtual void onStatusRequestMessage(uint8_t *payload, unsigned int length) = 0;
    virtual void loop() = 0;
};


#endif //FORSELFC_HVAC_IHVACTHERMOSTAT_H
