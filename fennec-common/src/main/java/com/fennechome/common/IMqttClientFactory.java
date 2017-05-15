package com.fennechome.common;

import org.eclipse.paho.client.mqttv3.IMqttClient;

public interface IMqttClientFactory {
    IMqttClient getMqttClient();
}
