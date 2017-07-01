package com.fennechome.common;

import org.eclipse.paho.client.mqttv3.*;
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

    public MqttClientFactory(String broker, String clientId, String pwd) {
        this.broker = broker;
        this.clientId = clientId;
        this.pwd = pwd;
    }

    @Override
    public IMqttClient getMqttClient() {
        return getMqttClient(null);
    }

    @Override
    public synchronized IMqttClient getMqttClient(MqttCallbackExtended callback) {
        try {
            if (mqttClient == null) {
                mqttClient = connect(callback);
            } else if (!mqttClient.isConnected()) {
                mqttClient.connect();
            }
        } catch (MqttException me) {
            try {
                switch (me.getReasonCode()) {
                    case MqttException.REASON_CODE_CLIENT_DISCONNECTING:
                    case MqttException.REASON_CODE_CLIENT_CONNECTED:
                    case MqttException.REASON_CODE_CONNECT_IN_PROGRESS:
                        // transition state, will need to wait
                        break;
                    case MqttException.REASON_CODE_CLIENT_CLOSED: {
                        mqttClient = connect(callback);
                        break;
                    }
                    default:
                        mqttClient = reconnect(callback);
                }
            } catch (Exception t) {
                throwUnableToReconnect(me, t);
            }
        } catch (Exception e) {
            try {
                mqttClient = reconnect(callback);
            } catch (Exception t) {
                throwUnableToReconnect(e, t);
            }
        }
        return mqttClient;
    }

    private void throwUnableToReconnect(Exception originalEx, Exception reconnectEx) {
        logger.error("Original connection exception. ", originalEx);
        logger.error("Reconnect exception. ", reconnectEx);
        throw new FennecException("Connection lost, unable to reconnect. ", originalEx);
    }

    private IMqttClient reconnect(MqttCallbackExtended callback) throws MqttException {
        try {
            mqttClient.disconnectForcibly(1000, 2000);
        } catch (MqttException e) {
            logger.warn("Unable to forcibly close connection. Ignore. ", e);
        } finally {
            try {
                mqttClient.close();
            } catch (MqttException e) {
                logger.warn("Unable to forcibly close connection. Ignore. ", e);
            }
        }
        return connect(callback);
    }

    private IMqttClient connect(MqttCallbackExtended callback) throws MqttException {
        MqttClient mqttClient = new MqttClient(broker, clientId, persistence);
        mqttClient.setCallback(callback);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setPassword(pwd.toCharArray());
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        logger.info("Connecting to broker: " + broker);
        mqttClient.connect(connOpts);
        return mqttClient;
    }

    @Override
    public synchronized void close() throws Exception {
        if (mqttClient != null) {
            mqttClient.disconnectForcibly();
        }
    }
}
