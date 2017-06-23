extern "C" {
#include "user_interface.h"
}

#include "MqttHvacControlCenter.cpp"
#include "LogMessageListener.cpp"
#include "HvacThermostatImpl.cpp"
#include "MessageDispatcher.cpp"

ADC_MODE(ADC_VCC);

static const int MINIMUM_STATE_SWITCH_TIME = 60000; // todo introduce switch time delay
char macAddress[18] = {0};

const char *ssid = "E7EA3E";
const char *password = "79296267";

//const char *mqttServer = "raspberrypi";
const char *mqttServer = "sergeypc";
const char *mqttUsername = "thermostat";
const char *mqttPassword = "thermostatPwd1";

char *mqttSubBase = (char *) "/control";

HvacThermostatImpl thermostat = HvacThermostatImpl();
MessageDispatcher dispatcher = MessageDispatcher(&thermostat);
MqttHvacControlCenter *mqttControlCenter;

char *readMac(char *toRead) {
    uint8_t mac[6];
    wifi_get_macaddr(STATION_IF, mac);
    sprintf(toRead, "%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    return toRead;
}

void setup() {
    Serial.begin(115200);
    pn("Start: ");
    thermostat.setup();
    mqttControlCenter = new MqttHvacControlCenter(readMac(macAddress), ssid, password, mqttServer, 1883,
                                                  mqttUsername,
                                                  mqttPassword);
    mqttControlCenter->subscribe(&dispatcher, mqttSubBase);
    pn("Init done.");
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
    thermostat.loop();
}