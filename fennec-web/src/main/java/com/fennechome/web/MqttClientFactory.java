package com.fennechome.web;

import com.fennechome.common.FennecException;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttClientFactory implements IMqttClientFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String broker;
    private final String clientId;
    private final String pwd;
    private final MemoryPersistence persistence = new MemoryPersistence();
    private IMqttClient mqttClient;

    public MqttClientFactory(Configuration configuration) {
        broker = configuration.getString("fennec.mqtt.broker");
        clientId = configuration.getString("fennec.mqtt.user");
        pwd = configuration.getString("fennec.mqtt.pwd");
    }

    @Override
    public synchronized IMqttClient getMqttClient() {
        try {
            if (mqttClient == null) {
                mqttClient = connect();
            } else if (!mqttClient.isConnected()) {
                mqttClient.disconnectForcibly();
                mqttClient = connect();
            }
        } catch (MqttException e) {
            throw new FennecException("Unable to open mqtt connection. ", e);
        }
        return mqttClient;
    }

    private IMqttClient connect() throws MqttException {
        MqttClient mqttClient = new MqttClient(broker, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setPassword(pwd.toCharArray());
        connOpts.setCleanSession(true);
        logger.info("Connecting to broker: " + broker);
        mqttClient.connect(connOpts);
        return mqttClient;
    }
}
