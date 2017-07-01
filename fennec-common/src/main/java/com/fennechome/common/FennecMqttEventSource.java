package com.fennechome.common;

import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;

/**
 * mqtt impl of events source
 */
public class FennecMqttEventSource implements IFennecEventSource, MqttCallbackExtended {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, TopicListener> listeners = new HashMap<>();
    private final IMqttClientFactory mqttClientFactory;

    public FennecMqttEventSource(IMqttClientFactory mqttClientFactory) {
        this.mqttClientFactory = mqttClientFactory;
    }

    @Override
    public void subscribe(String topic, Listener l) {
        TopicListener listener = listeners.get(topic);
        if (listener == null) {
            listener = new TopicListener(topic);
            listeners.put(topic, listener);
            subscribeInternal(topic, listener);
        }
        listener.add(l);
        logger.info("Subscribed to {}, listeners {} listeners state {} ", topic, listeners.size(), listeners);
    }

    private void subscribeInternal(String topic, TopicListener listener) {
        // todo shouldn't we have mqttClient link to manage lifecycle here
        IMqttClient mqttClient = mqttClientFactory.getMqttClient(FennecMqttEventSource.this);
        logger.info("Client subscribe to [{}] topic with client [{}]. ", topic, listener);
        try {
            mqttClient.subscribe(topic, listener);
        } catch (MqttException e) {
            logger.warn("Failed to subscribe to [" + topic + "]. ", e);
            // try second time when we know something's wrong
            try {
                mqttClient.disconnectForcibly();
                mqttClient = mqttClientFactory.getMqttClient(FennecMqttEventSource.this);
                mqttClient.subscribe(topic, listener);
            } catch (MqttException e1) {
                throw new FennecException("Unable to subscribe to mqtt server. ", e1);
            }
            throw new FennecException("Unable to subscribe to [" + topic + "]", e);
        }
    }

    @Override
    public void unsubscribe(String topic, Listener l) {
        TopicListener topicListeners = listeners.get(topic);
        topicListeners.remove(l);
        logger.info("Unsubscribed from {}, listeners count {} listeners state {} ", topic, listeners.size(), listeners);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
//        we lost all subscriptions by now, need to re-subscribe
        logger.info("Reconnect to {} URI {}. ", reconnect, serverURI);
        for (Map.Entry<String, TopicListener> entry : listeners.entrySet()) {
            subscribeInternal(entry.getKey(), entry.getValue());
//            logger.info("Resubscribing to topic {} with listeners {}. ", entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("Connection to mqtt server lost. Will reconnect. ", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception { }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { }

    static class TopicListener extends LinkedList<Listener> implements IMqttMessageListener {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String topic;

        public TopicListener(String topic) {
            this.topic = topic;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            logger.info("Message arrived to topic {}, content {}. ", topic, new String(message.getPayload()));
            for (int i = 0; i < size(); i++) {
                Listener listener = get(i);
                try {
                    listener.onEvent(topic, message.getPayload(), System.currentTimeMillis());
                } catch (Throwable t) {
                    logger.error("Error during message handling by {}. ", listener, t);
                }
            }
        }

        @Override
        public String toString() {
            return "TopicListener{" + "topic='" + topic + "\', listeners="  + super.toString() + "} ";
        }
    }
}
