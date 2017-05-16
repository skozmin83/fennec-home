package com.fennechome.common;

import org.bson.Document;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * mqtt impl
 */
public class FennecMqttEventSource implements IFennecEventSource {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, TopicListener> listeners = new HashMap<>();
    private final IMqttClientFactory mqttClientFactory;

    public FennecMqttEventSource(IMqttClientFactory mqttClientFactory) {
        this.mqttClientFactory = mqttClientFactory;
    }

    @Override
    public void subscribe(String topic, Listener l) {
        TopicListener value = listeners.computeIfAbsent(topic, k -> {
            IMqttClient mqttClient = mqttClientFactory.getMqttClient();
            TopicListener listener = new TopicListener();
            try {
                mqttClient.subscribe(topic, listener);
                return listener;
            } catch (MqttException e) {
                logger.warn("Failed to subscribe to [" + topic + "]. ", e);
                // try second time when we know something's wrong
                try {
                    mqttClient.disconnectForcibly();
                    mqttClientFactory.getMqttClient().subscribe(topic, listener);
                } catch (MqttException e1) {
                    throw new FennecException("Unable to subscribe to mqtt server. ", e1);
                }
                throw new FennecException("Unable to subscribe to [" + topic + "]", e);
            }
        });
        value.add(l);
        logger.info("Subscribed to {}, listeners {} listeners state {} ", topic, listeners.size(), listeners);
    }

    @Override
    public void unsubscribe(String topic, Listener l) {
        // don't bother having reverse map, just walk over it, it's small and happens rarely
        for (TopicListener topicListeners : listeners.values()) {
            topicListeners.remove(l);
        }
        logger.info("Unsubscribed from {}, listeners {} listeners state {} ", topic, listeners.size(), listeners);
    }

    static class TopicListener extends LinkedList<Listener> implements IMqttMessageListener {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Document json = Document.parse(new String(message.getPayload()));
            for (int i = 0; i < size(); i++) {
                get(i).onEvent(topic, json);
            }
        }
    }
}
