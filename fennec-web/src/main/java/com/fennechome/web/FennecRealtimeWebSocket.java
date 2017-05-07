package com.fennechome.web;

import com.fennechome.common.FennecException;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.common.io.FutureWriteCallback;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class FennecRealtimeWebSocket extends WebSocketAdapter implements IMqttMessageListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String topicBase;
    private final IMqttClientFactory mqttClientFactory;
    private final DateFormat df;
    private FutureWriteCallback callback = new FutureWriteCallback();

    public FennecRealtimeWebSocket(String topicBase, IMqttClientFactory mqttClientFactory, DateFormat df) {
        this.topicBase = topicBase;
        this.mqttClientFactory = mqttClientFactory;
        this.df = df;
    }

    public FennecRealtimeWebSocket(Configuration config, IMqttClientFactory mqttClientFactory) {
        topicBase = config.getString("fennec.mqtt.devices-base-topic");
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
        subscribe(topicBase + device + "/" + sid);
    }

    private void subscribe(String subTopic) {
        IMqttClient mqttClient = mqttClientFactory.getMqttClient();
        try {
            logger.info("Subscribing to [" + subTopic + "]. ");
            mqttClient.subscribe(subTopic, this);
        } catch (MqttException e) {
            logger.warn("Failed to subscribe to [" + subTopic + "]. ", e);
            // try second time when we know something's wrong
            try {
                mqttClient.disconnectForcibly();
                mqttClientFactory.getMqttClient().subscribe(subTopic, this);
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
        logger.info("Socket Closed: [" + statusCode + "] " + reason);
        super.onWebSocketClose(statusCode, reason);
        close();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        logger.info("Socket error. ", cause);
        super.onWebSocketError(cause);
        close();
    }

    private void close() {
//        try {
//            mqttClientFactory.getMqttClient().disconnect();
            logger.info("Disconnected");
//        } catch (MqttException e) {
            // ignore
//        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            logger.info("Received mqtt TEXT message on topic:" + topic + ", message: " + new String(message.getPayload()));
            String text = new String(message.getPayload());
            text = text.replace("}", ", \"ts\": \"" + df.format(new Date()) + "\"}");
            getRemote().sendString(text, callback);
        } catch (Exception e) {
            logger.error("Unable to publish message. ", e);
            throw new FennecException(e);
        }
    }
}
