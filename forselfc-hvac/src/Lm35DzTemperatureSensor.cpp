//
// Created by sergey on 3/4/2017.
//

#ifndef FORSELFC_TEMPERATURE_SENSOR_LM35DZ
#define FORSELFC_TEMPERATURE_SENSOR_LM35DZ

#include "Arduino.h"
#include "ITemperatureSensor.h"

class Lm35DzTemperatureSensor : public ITemperatureSensor {
private:
    char *id;
    uint8_t pin;
    TemperatureResult result = TemperatureResult();
public:
    Lm35DzTemperatureSensor(uint8_t pin, char *id) : pin(pin), id(id) {}

    ITemperatureResult &read() {
        int pinReading = analogRead(pin);
        float curTemp = (3.3 * analogRead(pin) * 100.0) / 1024;
//        Serial.println(pinReading);
        result.setTemperature(curTemp);
        return result;
    }

    void init() {}

    char *getId() {
        return id;
    }
};

#endif
