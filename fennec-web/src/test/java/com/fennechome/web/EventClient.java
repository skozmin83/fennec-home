package com.fennechome.web;

import java.io.File;
import java.net.URI;
import java.util.concurrent.Future;

import com.fennechome.common.IMqttClientFactory;
import com.fennechome.common.MqttClientFactory;
import com.fennechome.common.PropertiesUtil;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class EventClient {
    public static void main(String[] args) {
        URI uri = URI.create("ws://localhost:8080/events/");

        WebSocketClient client = new WebSocketClient();
        Configuration config = PropertiesUtil.getConfig(new File("fennechome-ui-server-local.properties"));
        IMqttClientFactory mqttClientFactory = new MqttClientFactory(config);
        try {
            try {
                client.start();
                // The socket that receives events
                FennecSensorEventWebSocket socket =
                        new FennecSensorEventWebSocket(config, new MqttUiEventSource(mqttClientFactory));
                // Attempt Connect
                Future<Session> fut = client.connect(socket, uri);
                // Wait for Connect
                Session session = fut.get();
                // Send a message
                session.getRemote().sendString("Hello");
                // Close session
                session.close();
            } finally {
                client.stop();
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
}
