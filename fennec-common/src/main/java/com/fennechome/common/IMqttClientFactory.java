package com.fennechome.common;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;

public interface IMqttClientFactory extends AutoCloseable {
    IMqttClient getMqttClient();
    IMqttClient getMqttClient(MqttCallbackExtended callback);
}
