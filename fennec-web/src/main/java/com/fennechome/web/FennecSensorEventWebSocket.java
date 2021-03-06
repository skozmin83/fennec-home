package com.fennechome.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fennechome.common.FennecException;
import com.fennechome.common.IFennecEventSource;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.common.io.FutureWriteCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class FennecSensorEventWebSocket extends WebSocketAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String devicesBaseTopic;
    private final IFennecEventSource source;
    private final DateFormat df;
    private final MqttTempSensorListener sensorsListener = new MqttTempSensorListener();
    private FutureWriteCallback callback = new FutureWriteCallback();

    public FennecSensorEventWebSocket(Configuration config, IFennecEventSource source) {
        devicesBaseTopic = config.getString("fennec.mqtt.devices-base-topic");
        this.source = source;
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        //            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        // Quoted "Z" to indicate UTC, no timezone offset
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(tz);
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        logger.info("Socket Connected [" + this + "]: " + sess);

        Map<String, List<String>> params = sess.getUpgradeRequest().getParameterMap();
        List<String> sidParams = params.get("sid");
        List<String> deviceParams = params.get("id");
        if (sidParams.isEmpty() || deviceParams.isEmpty()) {
            throw new IllegalArgumentException("[sid] and [topic] params, must be present. ");
        }
        String sid = sidParams.get(0);
        String device = deviceParams.get(0);
        String sensorTopic = devicesBaseTopic + device + "/" + sid;
        sensorsListener.setTopic(sensorTopic);
        sensorsListener.setId(sess.getRemoteAddress().toString());
        source.subscribe(sensorTopic, sensorsListener);
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
        logger.info("Disconnected, unsubscribing from [{}, {}]. ", sensorsListener.topic);
        source.unsubscribe(sensorsListener.topic, sensorsListener);
    }

    private class MqttTempSensorListener implements IFennecEventSource.Listener {
        private final ObjectMapper   mapper     = new ObjectMapper();
        private String topic;
        private String id;

        public void setTopic(String topic) {
            this.topic = topic;
        }

        @Override
        public void onEvent(String topic, byte[] msg, long ts) {
            try {
                ObjectNode json = (ObjectNode) mapper.readTree(msg);
                json.put("ts", System.currentTimeMillis());
                json.put("etype", "TEMPERATURE_SENSOR");
//                logger.info("Publish:" + topic + ", message: " + json);
                getRemote().sendString(mapper.writeValueAsString(json), callback);
            } catch (Exception e) {
                logger.error("Unable to publish message. ", e);
                throw new FennecException(e);
            }
        }

        @Override
        public String toString() {
            return "MqttTempSensorListener{id='" + id + '\'' + '}';
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
