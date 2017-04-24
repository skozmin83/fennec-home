//
// Created by sergey on 3/4/2017.
//

#ifndef FORSELFC_TEMPERATUREPUBLISHER_H
#define FORSELFC_TEMPERATUREPUBLISHER_H

#include <Arduino.h>

class ITemperaturePublisher {
public:
    virtual boolean publish(char *baseTopic, char *controllerId, char *sensorId, float humidity, float temperature, float voltage,
                            uint32_t measureId) = 0;
private:
};

#endif //FORSELFC_TEMPERATUREPUBLISHER_H
