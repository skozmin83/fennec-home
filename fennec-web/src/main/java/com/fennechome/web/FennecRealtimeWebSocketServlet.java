package com.fennechome.web;

import com.fennechome.common.FennecException;
import com.fennechome.common.IMqttClientFactory;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.websocket.servlet.*;

public class FennecRealtimeWebSocketServlet extends WebSocketServlet {
    private final IMqttClientFactory mqttClientFactory;
    private final Configuration configuration;

    public FennecRealtimeWebSocketServlet(Configuration configuration, IMqttClientFactory mqttClientFactory) {
        try {
            this.mqttClientFactory = mqttClientFactory;
            this.configuration = configuration;
        } catch (Exception e) {
            throw new FennecException("Unable to get a connection. ");
        }
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        // set a 10 second timeout
//        factory.getPolicy().setAsyncWriteTimeout(10000);
        factory.getPolicy().setIdleTimeout(0);
//        factory.register(FennecRealtimeWebSocket.class);
        // todo pool as soon as closed
        factory.setCreator((req, resp) -> new FennecRealtimeWebSocket(configuration, mqttClientFactory));
    }
}
