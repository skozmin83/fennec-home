package com.fennechome.web;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.pool2.ObjectPool;
import org.eclipse.jetty.websocket.servlet.*;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class FennecRealtimeWebSocketServlet extends WebSocketServlet {
    private ObjectPool<MqttClient> pool;
    private final Configuration configuration;

    public FennecRealtimeWebSocketServlet(ObjectPool<MqttClient> pool, Configuration configuration) {
        this.pool = pool;
        this.configuration = configuration;
    }

    private static final MqttClient sampleClient;
    private static final String broker = "tcp://raspberrypi:1883";
    private static final String clientId = "user";
    private static final String pwd = "yourpassword";
    private static final MemoryPersistence persistence = new MemoryPersistence();

    static {
        try {
            sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setPassword(pwd.toCharArray());
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace(System.out);
            throw new RuntimeException("Unable to connect. ", me);
        }
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        // set a 10 second timeout
        factory.getPolicy().setIdleTimeout(10000);
//        factory.register(FennecRealtimeWebSocket.class);
        // todo pool as soon as closed
        factory.setCreator((req, resp) -> new FennecRealtimeWebSocket(configuration, pool));
    }
}
