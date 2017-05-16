package com.fennechome.common;

import org.eclipse.paho.client.mqttv3.IMqttClient;

public interface IMqttClientFactory extends AutoCloseable {
    IMqttClient getMqttClient();
}
