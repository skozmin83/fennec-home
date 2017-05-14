package com.fennechome.server;

import com.fennechome.common.IMqttClientFactory;
import com.fennechome.common.MqttClientFactory;
import com.fennechome.common.PropertiesUtil;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;

public class TestMqttSender {
    public static void main(String[] args) {
        Configuration config = PropertiesUtil.getConfig(new File("fennechome-mqtt-server-test.properties"));
        IMqttClientFactory mqttClientFactory = new MqttClientFactory(config);
        IMqttClient mqttClient = mqttClientFactory.getMqttClient();
        String topicBase = config.getString("fennec.mqtt.devices-base-topic");

        int step = 0;
        while(true) {
            try {
                Thread.sleep(300);
//                Thread.sleep(1000);
                int tempBase = 23;
                float sinPart = (float) (Math.sin(Math.toRadians(step++ * 6)) * 3);
                String msg = "{\"t\":" + (tempBase + sinPart) + ",\"h\":59.30,\"v\":2.67,\"sid\":\"dht22-top\"}";
                System.out.println("Send: " + msg);
                mqttClient.publish(topicBase + "A0:20:A6:16:A6:34/dht22-top", new MqttMessage(msg.getBytes()));
            } catch (Exception e) {
                e.printStackTrace();
                mqttClient = mqttClientFactory.getMqttClient();
            }
//        mqttClient.publish(topicBase + "A0:20:A6:16:A6:34/dht22-bottom", );
//        mqttClient.publish(topicBase + "A0:20:A6:16:A7:0A/dht22-top", );
//        mqttClient.publish(topicBase + "A0:20:A6:16:A7:0A/dht22-bottom", );
        }
    }
}
