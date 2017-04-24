//
// Created by sergey on 3/4/2017.
//

#ifndef FORSELFC_TEMPERATURE_SENSOR_DH11
#define FORSELFC_TEMPERATURE_SENSOR_DH11

#include "ITemperatureSensor.h"
#include <DHT.h>

#define DHTTYPE DHT11

static const int MIN_TEMP = -255;
static const int AVG_ARRAY_SIZE = 5;
static const int MAX_MEASURE_NUM = 20000;

class Dh11NoiselessTemperatureSensor : public ITemperatureSensor {
private:
    DHT dht;
    /**
     * since the biggest noise seen is 3C we make it 5, so in case of the temperature change it accumulates
     */
    float tempHistory[AVG_ARRAY_SIZE] = {MIN_TEMP, MIN_TEMP, MIN_TEMP, MIN_TEMP, MIN_TEMP};
    int measureNum = 0;
    float prevTemp = MIN_TEMP;
    TemperatureResult result = TemperatureResult();
private:

    float readTemperature() {
        if(++measureNum > MAX_MEASURE_NUM) { // don't want to overflow to negative values
            measureNum = MAX_MEASURE_NUM % AVG_ARRAY_SIZE; // get the the same index as it was
        }
        float curTemp = dht.readTemperature();
        tempHistory[measureNum % AVG_ARRAY_SIZE] = curTemp;
        float avgTemp = getTempAverage(tempHistory);
        float ret;
        if (abs(avgTemp - curTemp) > abs(avgTemp - prevTemp) || isnan(curTemp)) {// stick to the previous temp
            ret = prevTemp;
        } else {// new temp is closer to the avg temp for the past 5 measures
            ret = curTemp;
            prevTemp = curTemp;
        }
        //Serial.println("curTemp: " + String(curTemp) + ", avtTemp: " + String(avgTemp) + ", prevTemp: "+ String(prevTemp) + ", result: " + String(ret));
        return ret;
    }

    float readHumidity() {
        return dht.readHumidity();
    }

    float getTempAverage(float *temperatures) {
        float ret = 0;
        int distSize = 0;
        for (auto &&temperature : tempHistory) {
            if ((temperature > MIN_TEMP) && !isnan(temperature)) {
                distSize++;
                ret += temperature;
            }
        }
        return distSize > 0 ? ret / distSize : -256;
    }
public:
    Dh11NoiselessTemperatureSensor(uint8_t pin) : dht(pin, DHTTYPE) {}

    ITemperatureResult &read() {
        result.setHumidity(readHumidity());
        result.setTemperature(readTemperature());
        return result;
    }

    void init() {
        dht.begin();
    }
};

#endif
