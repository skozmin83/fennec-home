//
// Created by sergey on 3/4/2017.
//
#ifndef MQTT_CONTROL_CENTER
#define MQTT_CONTROL_CENTER

#define MQTT_MAX_PACKET_SIZE 256
//MQTT_KEEPALIVE
//Sets the keepalive interval, in seconds, the client will use. This is used to maintain the connection when no other packets are being sent or received.
//Default: 15 seconds
//
//MQTT_VERSION
//Sets the version of the MQTT protocol to use.
//Default: MQTT 3.1.1
//
//MQTT_MAX_TRANSFER_SIZE
//        Sets the maximum number of bytes passed to the network client in each write call. Some hardware has a limit to how much data can be passed to them in one go, such as the Arduino Wifi Shield.
//Default: undefined (complete packet passed in each write call)
//
//MQTT_SOCKET_TIMEOUT
//        Sets the timeout when reading from the network. This also applies as the timeout for calls to connect.
//Default: 15 seconds

#include <WiFiClient.h>
#include <ESP8266WiFi.h>
#include "IHvacControlCenter.h"
#include "SerialTemperaturePublisher.cpp"
#include "MqttTemperatureJsonPublisher.cpp"
#include <stdlib.h> // for malloc and free

class MqttHvacControlCenter : public IHvacControlCenter {
private:
    const char *connectionId;
    const char *ssid;
    const char *wifiPassword;
    const char *mqttUsername;
    const char *mqttPassword;

    WiFiClient wifiClient;
    PubSubClient mqttClient;
    MqttTemperatureJsonPublisher mqttTemperaturePublisher;

    char mqttSubTopic[256];
//    IMessageListener *listener;

    char *topic;
    char *controllerId;
    char *sensorId;
    float humidity;
    float temperature;
    float voltage;
    uint32_t measureId;
public:
    MqttHvacControlCenter(const char *connectionId,
                      const char *ssid,
                      const char *wifiPassword,
                      const char *mqttServer,
                      uint16_t mqttPort,
                      const char *mqttUsername,
                      const char *mqttPassword
    ) :
            connectionId(connectionId),
            ssid(ssid),
            wifiPassword(wifiPassword),
            mqttUsername(mqttUsername),
            mqttPassword(mqttPassword),
            wifiClient(WiFiClient()),
            mqttClient(PubSubClient(wifiClient)),
            mqttTemperaturePublisher(MqttTemperatureJsonPublisher(mqttClient)) {
        mqttClient.setServer(mqttServer, mqttPort);
    }

    void* operator new(size_t size) { return malloc(size); }
    void operator delete(void* ptr) { free(ptr); }

    void subscribe(IMessageListener *inListener, char *mqttSubTopicIn) {
//        this->listener = inListener;
        memset(this->mqttSubTopic, '\0', sizeof(this->mqttSubTopic));
        strcpy(this->mqttSubTopic, mqttSubTopicIn);
        strcat(this->mqttSubTopic, "/");
        strcat(this->mqttSubTopic, connectionId);
        
        Serial.print("Subscribe to: ");
        Serial.println(this->mqttSubTopic);

        if (!mqttClient.connected()) {
            reconnect();
        }
        mqttClient.setCallback([inListener](char *topic, byte *payload, unsigned int length) {
            inListener->onMessage(topic, payload, length);
        });
        mqttClient.subscribe(this->mqttSubTopic);
    }

    ITemperaturePublisher &getPublisher() {
        if (!mqttClient.connected()) {
            reconnect();
        }
//        publishTemp();
//        disconnect();
//        Serial.println( "Sleeping for a minute");
//        delay(SLEEP_DELAY_IN_SECONDS * 1000);
        return (ITemperaturePublisher &) mqttTemperaturePublisher;
    };

    boolean
    publish(char *baseTopic, char controllerId[18], char *sensorId, float humidity, float temperature, float voltage,
            uint32_t measureId) {
        // todo use command pattern here
        this->topic = baseTopic;
        this->controllerId = controllerId;
        this->sensorId = sensorId;
        this->humidity = humidity;
        this->temperature = temperature;
        this->voltage = voltage;
        this->measureId = measureId;
        return retry(&MqttHvacControlCenter::publishInternal, 10, "mqtt client publish call");
    }

    boolean loop() {
        return retry(&MqttHvacControlCenter::loopInternal, 10, "mqtt client loop call");
    }

    boolean loopInternal() {
        return mqttClient.loop();
    }

    boolean publishInternal() {
        return mqttTemperaturePublisher.publish(topic, controllerId, sensorId, humidity, temperature, voltage,
                                                measureId);
    }

    boolean retry(boolean(MqttHvacControlCenter::*callback)(), int retryCount, const char *description) {
        boolean status = (boolean) false;
        for (int i = 0; i < retryCount; i++) {
            status = (this->*callback)();
            if (status) {
                break;
            } else {
                Serial.print("Issues with [");
                Serial.print(description);
                Serial.print("], ret code [");
                Serial.print(status);
                Serial.print("], state [");
                Serial.print(mqttClient.state());
                Serial.print("], retry: ");
                Serial.println(i);
                if (!mqttClient.connected()) {
                    reconnect();
                }
            }
        }
        return status;
    }

    void reconnect() {
        while (WiFi.status() != WL_CONNECTED) {
            digitalWrite(LED_BUILTIN, LOW);
            delay(500);
            digitalWrite(LED_BUILTIN, HIGH);
            setup_wifi();
        }

        while (!mqttClient.connected()) {
            Serial.print("Attempting MQTT connection...");
            //if (client.connect("ESP8266_Client", mqtt_username, mqtt_password)) {
            //if (client.connect("ESP8266_Client")) {
            //mqttClient.connect("myClientID", willTopic, willQoS, willRetain, willMessage);
            //if (mqttClient.connect("client")) {
            if (mqttClient.connect(connectionId, mqttUsername, mqttPassword)) {
                Serial.println("connected");
                mqttClient.subscribe(mqttSubTopic);
            } else {
                Serial.print("MQTT connection failed, mqtt client state: ");
                Serial.print(mqttClient.state());
                Serial.println(", try again in 5 seconds");
                delay(5000);
            }
        }
    }

    void setup_wifi() {
        delay(10);
        Serial.println();
        Serial.print("Connecting to ");
        Serial.println(ssid);

        WiFi.begin(ssid, wifiPassword);

        while (WiFi.status() != WL_CONNECTED) {
            delay(500);
            Serial.print(".");
        }

        Serial.println("");
        Serial.println("WiFi connected");
        Serial.println("IP address: ");
        Serial.println(WiFi.localIP());
    }

    void disconnect() {
        Serial.println("Closing MQTT connection...");
        mqttClient.disconnect();
        mqttClient.loop();
        Serial.println("Closing WiFi connection...");
        WiFi.disconnect();
    }
};

#endif
