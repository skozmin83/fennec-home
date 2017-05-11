package com.fennechome.web;

import com.fennechome.common.FennecException;
import com.fennechome.common.IMqttClientFactory;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.common.io.FutureWriteCallback;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class FennecRealtimeWebSocket extends WebSocketAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String devicesBase;
    private final String controlsBase;
    private final IMqttClientFactory mqttClientFactory;
    private final DateFormat df;
    private final MqttTempSensorListener sensorsListener = new MqttTempSensorListener();
    private final MqttThermostatDirectivesListener thermostatDirectivesListener = new MqttThermostatDirectivesListener();
    private FutureWriteCallback callback = new FutureWriteCallback();
    private Set<String> subTopics = new HashSet<>();

    public FennecRealtimeWebSocket(Configuration config, IMqttClientFactory mqttClientFactory) {
        devicesBase = config.getString("fennec.mqtt.devices-base-topic");
        controlsBase = config.getString("fennec.mqtt.control-base-topic");
        this.mqttClientFactory = mqttClientFactory;
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
        List<String> deviceParams = params.get("topic");
        if (sidParams.isEmpty() || deviceParams.isEmpty()) {
            throw new IllegalArgumentException("[sid] and [topic] params, must be present. ");
        }
        String sid = sidParams.get(0);
        String device = deviceParams.get(0);
        subscribe(devicesBase + device + "/" + sid, sensorsListener);

        List<String> thermostatParams = params.get("thermostat");
        if (thermostatParams != null && !thermostatParams.isEmpty()) {
            String thermostat = thermostatParams.get(0);
            subscribe(controlsBase + thermostat, thermostatDirectivesListener);
        }
    }

    private void subscribe(String subTopic, IMqttMessageListener listener) {
        subTopics.add(subTopic);
        IMqttClient mqttClient = mqttClientFactory.getMqttClient();
        try {
            logger.info("Subscribing to [" + subTopic + "]. ");
            mqttClient.subscribe(subTopic, listener);
        } catch (MqttException e) {
            logger.warn("Failed to subscribe to [" + subTopic + "]. ", e);
            // try second time when we know something's wrong
            try {
                mqttClient.disconnectForcibly();
                mqttClientFactory.getMqttClient().subscribe(subTopic, listener);
            } catch (MqttException e1) {
                throw new FennecException("Unable to subscribe to mqtt server. ", e1);
            }
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
        try {
            logger.info("Disconnected, unsubscribing from [{}]. ", subTopics);
            mqttClientFactory.getMqttClient().unsubscribe(subTopics.toArray(new String[subTopics.size()]));
        } catch (MqttException e) {
            throw new FennecException("Unable to unsubscribe from [" + subTopics + "]. ", e);
        }
    }

    private class MqttTempSensorListener implements IMqttMessageListener {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                String text = new String(message.getPayload());
//            MessageFormat f = new MessageFormat("\"ts") .format("")
                text = text.replace("}", ", \"ts\": \"" + df.format(new Date()) + "\", \"etype\":\"TEMPERATURE_SENSOR\"}");
                logger.info("Publish:" + topic + ", message: " + text);
                getRemote().sendString(text, callback);
            } catch (Exception e) {
                logger.error("Unable to publish message. ", e);
                throw new FennecException(e);
            }
        }
    }

    private class MqttThermostatDirectivesListener implements IMqttMessageListener {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                String text = new String(message.getPayload());
                text = text.replace("}", ", \"etype\":\"THERMOSTAT\"}");
                logger.info("Publish:" + topic + ", message: " + text);
                getRemote().sendString(text, callback);
            } catch (Exception e) {
                logger.error("Unable to publish message. ", e);
                throw new FennecException(e);
            }
        }
    }
}
