//
// Created by sergey on 4/23/2017.
//

#ifndef FORSELFC_HVAC_IHVACTHERMOSTAT_H
#define FORSELFC_HVAC_IHVACTHERMOSTAT_H

#include <cstdint>
#include "FennecHelper.h"


typedef struct ControlMessage {
    uint8_t messageType;
    /**
     * could be: :
     *      A -> Fan ON,  Heating ON,  Cooling OFF, COMPRESSOR ON
     *      H -> Fan ON,  Heating ON,  Cooling OFF, COMPRESSOR ON
     *      C -> Fan ON,  Heating OFF, Cooling ON,  COMPRESSOR ON
     *      Z -> Fan OFF, Heating OFF, Cooling OFF, COMPRESSOR OFF
     */
    uint8_t mode;

} ControlMessage;

static const uint8_t FAN_PIN = D6;
static const uint8_t COMPRESSOR_PIN = D0;
static const uint8_t REVERSE_VALVE_PIN = D5;

class IHvacThermostat {
public:
    virtual void setup() = 0;
    virtual void onControlMessage(uint8_t *payload, unsigned int length) = 0;
    virtual void loop() = 0;
};


#endif //FORSELFC_HVAC_IHVACTHERMOSTAT_H
