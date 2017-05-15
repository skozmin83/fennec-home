package com.fennechome.web;

import com.fennechome.common.FennecException;
import com.fennechome.common.IMqttClientFactory;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * mqtt impl
 */
public class MqttUiEventSource implements IUiEventSource {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, TopicListener> listeners = new HashMap<>();
    private final IMqttClientFactory mqttClientFactory;

    public MqttUiEventSource(IMqttClientFactory mqttClientFactory) {
        this.mqttClientFactory = mqttClientFactory;
    }

    @Override
    public void subscribe(String topic, IUiEventListener l) {
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
    public void unsubscribe(String topic, IUiEventListener l) {
        // don't bother having reverse map, just walk over it, it's small and happens rarely
        for (TopicListener topicListeners : listeners.values()) {
            topicListeners.remove(l);
        }
        logger.info("Unsubscribed from {}, listeners {} listeners state {} ", topic, listeners.size(), listeners);
    }

    static class TopicListener extends HashSet<IUiEventListener> implements Set<IUiEventListener>, IMqttMessageListener {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            for (IUiEventListener listener : this) {
                listener.onEvent(topic, new String(message.getPayload()));
            }
        }
    }
}
