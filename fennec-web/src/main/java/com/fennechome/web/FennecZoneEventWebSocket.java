package com.fennechome.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fennechome.common.FennecException;
import com.fennechome.common.IFennecEventSource;
import com.fennechome.common.BsonSerializer;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.common.io.FutureWriteCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class FennecZoneEventWebSocket extends WebSocketAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String             uiBaseTopic;
    private final IFennecEventSource source;
    private final MqttThermostatDirectivesListener thermostatDirectivesListener =
            new MqttThermostatDirectivesListener();
    private       FutureWriteCallback              callback                     = new FutureWriteCallback();

    public FennecZoneEventWebSocket(Configuration config, IFennecEventSource source) {
        uiBaseTopic = config.getString("fennec.mqtt.ui-base-topic");
        this.source = source;
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        logger.info("Zone socket connected [" + this + "]: " + sess);
        Map<String, List<String>> params = sess.getUpgradeRequest().getParameterMap();
        List<String> thermostatParams = params.get("id");
        if (thermostatParams != null && !thermostatParams.isEmpty()) {
            String thermostat = thermostatParams.get(0);
            String thermostatTopic = uiBaseTopic + thermostat;
            thermostatDirectivesListener.setTopic(thermostatTopic);
            thermostatDirectivesListener.setId(sess.getRemoteAddress().toString());
            source.subscribe(thermostatTopic, thermostatDirectivesListener);
        } else {
            throw new IllegalArgumentException("[sid] and [topic] params, must be present. ");
        }
    }

    @Override
    public void onWebSocketText(String message) {
        logger.info("Received websocket TEXT message: " + message);
        super.onWebSocketText(message);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.warn("Socket Closed: [" + statusCode + "] " + reason);
        super.onWebSocketClose(statusCode, reason);
        close();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        logger.error("Socket error. ", cause);
        super.onWebSocketError(cause);
        close();
    }

    private void close() {
        logger.info("Disconnected, unsubscribing from [{}]. ", thermostatDirectivesListener.topic);
        source.unsubscribe(thermostatDirectivesListener.topic, thermostatDirectivesListener);
    }

    private class MqttThermostatDirectivesListener implements IFennecEventSource.Listener {
        private final ObjectMapper mapper = new ObjectMapper();
        private String topic;
        private String id;

        public void setTopic(String topic) {
            this.topic = topic;
        }

        @Override
        public void onEvent(String topic, byte[] msg, long ts) {
            try {
                ObjectNode json = (ObjectNode) mapper.readTree(msg);
//                logger.info("Publish:" + topic + ", message: " + json);
                getRemote().sendString(mapper.writeValueAsString(json), callback);
            } catch (Exception e) {
                logger.error("Unable to publish message. ", e);
                throw new FennecException(e);
            }
        }

        @Override
        public String toString() {
            return "MqttThermostatDirectivesListener{id='" + id + '\'' + '}';
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
