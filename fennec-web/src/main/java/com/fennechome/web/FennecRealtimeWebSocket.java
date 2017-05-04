package com.fennechome.web;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.pool2.ObjectPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.common.io.FutureWriteCallback;
import org.eclipse.paho.client.mqttv3.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class FennecRealtimeWebSocket extends WebSocketAdapter implements IMqttMessageListener {
    private final String topicBase;
    private final ObjectPool<MqttClient> pool;
    private final DateFormat df;
    public FennecRealtimeWebSocket(Configuration config, ObjectPool<MqttClient> pool) {
        topicBase = config.getString("fennec.mqtt.devices-base-topic");
        this.pool = pool;
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        //            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        // Quoted "Z" to indicate UTC, no timezone offset
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(tz);
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        System.out.println("Socket Connected [" + this + "]: " + sess);

        Map<String, List<String>> params = sess.getUpgradeRequest().getParameterMap();
        List<String> sidParams = params.get("sid");
        List<String> deviceParams = params.get("topic");
        if (sidParams.isEmpty() || deviceParams.isEmpty()) {
            throw new IllegalArgumentException("[sid] and [topic] params, must be present. ");
        }
        final String sid = sidParams.get(0);
        final String device = deviceParams.get(0);

        try {
            String subTopic = this.topicBase + device + "/" + sid;
            sampleClient.subscribe(subTopic, this);
            System.out.println("Subscribed to [" + subTopic + "]");
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace(System.out);
        }
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        System.out.println("Received websocket TEXT message: " + message);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        close();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
        close();
    }

    private void close() {
//        try {
//            sampleClient.disconnect();
        pool.returnObject(this);
            System.out.println("Disconnected");
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("Received mqtt TEXT message on topic:" + topic + ", message: " + new String(message.getPayload()));
        String text = new String(message.getPayload());
        text = text.replace("}", ", \"ts\": \"" + df.format(new Date()) + "\"}");
        getRemote().sendString(text, new FutureWriteCallback());
    }
}
