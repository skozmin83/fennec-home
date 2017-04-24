extern "C" {
#include "user_interface.h"
}

#include<DHT.h>
#include "MqttHvacControlCenter.cpp"
#include "LogMessageListener.cpp"

ADC_MODE(ADC_VCC);

static const int MINIMUM_STATE_SWITCH_TIME = 60000;
char macAddress[18] = {0};

const char *ssid = "E7EA3E";
const char *password = "79296267";

const char *mqttServer = "raspberrypi";
const char *mqttUsername = "user";
const char *mqttPassword = "yourpassword";

char *mqttSubBase = (char *) "/control";

uint32_t lastPublishTime = 0;
MqttHvacControlCenter *mqttControlCenter;
int sensorNum = 0;
LogMessageListener listener = LogMessageListener();

char *readMac(char *toRead) {
    uint8_t mac[6];
    wifi_get_macaddr(STATION_IF, mac);
    sprintf(toRead, "%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    return toRead;
}

void setup() {
    pinMode(BUILTIN_LED, OUTPUT);
    digitalWrite(BUILTIN_LED, HIGH);
    Serial.begin(115200);
    Serial.print("Start: ");
    mqttControlCenter = new MqttHvacControlCenter(readMac(macAddress), ssid, password, mqttServer, 1883, mqttUsername,
                                              mqttPassword);
    mqttControlCenter->subscribe(&listener, mqttSubBase);
}

void loop() {
    // in loop we check for the new commands (full state), such as:
    // * vent on + cool on
    // * vent on + heat on
    // * vent on + cool on, level 2
    // * vent on + heat on, level 2
    // * vent on + emergency
    // * heartbeat ( if no heartbeat we shut everything down for certain period of time for the case of connection issues)

    // reply with status on heartbeat msg

    mqttControlCenter->loop();
}
