package com.fennechome.common;

import org.eclipse.paho.client.mqttv3.IMqttClient;

/**
 * Created by sergey on 5/6/2017.
 */
public interface IMqttClientFactory {
    IMqttClient getMqttClient();
}
