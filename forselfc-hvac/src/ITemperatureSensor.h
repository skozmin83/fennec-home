//
// Created by sergey on 3/4/2017.
//

#ifndef FORSELFC_TEMPERATURE_SENSOR_H
#define FORSELFC_TEMPERATURE_SENSOR_H

class ITemperatureResult {
public:
    virtual float getTemperature() = 0;
    virtual float getHumidity() = 0;
};

class ITemperatureSensor {
public:
    virtual void init() = 0;
    virtual ITemperatureResult& read() = 0;
    virtual char *getId() = 0;
};

class TemperatureResult : public ITemperatureResult {
private:
    float temperature = 0;
    float humidity = 0;
public:
    float getTemperature() {
        return temperature;
    }
    float getHumidity() {
        return humidity;
    }

    void setTemperature(float temperature) {
        TemperatureResult::temperature = temperature;
    }

    void setHumidity(float humidity) {
        TemperatureResult::humidity = humidity;
    }
};

#endif
