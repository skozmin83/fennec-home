//
// Created by sergey on 3/4/2017.
//
#ifndef ICONTROL_CENTER
#define ICONTROL_CENTER

#include "IMessageListener.h"
#include "ITemperaturePublisher.h"

class IHvacControlCenter {
public:
    virtual void subscribe(IMessageListener *listener, char *subTopic) =0;
    virtual ITemperaturePublisher &getPublisher() =0;
    virtual boolean
    publish(char *baseTopic, char controllerId[18], char *sensorId, float humidity, float temperature, float voltage,
                uint32_t measureId) = 0;
    virtual boolean loop() =0;
};

#endif
