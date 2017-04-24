//
// Created by sergey on 3/4/2017.
//
#ifndef MQTT_TEMPERATURE_PUBLISHER
#define MQTT_TEMPERATURE_PUBLISHER

#include <Arduino.h>
#include <PubSubClient.h>
#include "ITemperaturePublisher.h"

class MqttTemperatureJsonPublisher : public ITemperaturePublisher {
private:
    PubSubClient &mqttClient;
    char topicBuf[256];
    char payloadBuf[256];
    char float_temp[8]; // consider - sign
    char float_hum[8];
    char float_voltage[8];
public:
    MqttTemperatureJsonPublisher(PubSubClient &client) : mqttClient(client) {}

    boolean publish(char *baseTopic, char *controllerId, char *sensorId, float humidity, float temperature, float voltage,
                uint32_t measureId) {
        memset(payloadBuf, '\0', sizeof(payloadBuf));
        memset(topicBuf, '\0', sizeof(topicBuf));

        strcpy(topicBuf, baseTopic);
        strcat(topicBuf, "/");
        strcat(topicBuf, controllerId);
        strcat(topicBuf, "/");
        strcat(topicBuf, sensorId);

        sprintf(payloadBuf, "{\"t\":%s,\"h\":%s,\"v\":%s,\"sid\":\"%s\"}",
//        sprintf(payloadBuf, "{\"t\":%s,\"h\":%s,\"v\":%s,\"cid\":\"%s\",\"sid\":\"%s\",\"mid\":\"%u\"}",
//        sprintf(payloadBuf, "{\"t\":%s,\"h\":%s,\"v\":%s}",
                dtostrf((isnan(temperature) ? -274 : temperature), 4, 2, float_temp),
                dtostrf((isnan(humidity) ? -1 : humidity), 4, 2, float_hum),
                dtostrf(voltage, 4, 2, float_voltage),
//                controllerId,
                sensorId,
                measureId
        );
        Serial.print(topicBuf);
        Serial.print(": ");
        Serial.println(payloadBuf);
        return mqttClient.publish(topicBuf, payloadBuf);
//        mqttClient.loop();
//        return (boolean)true;
    }
};

#endif
