package com.fennechome.web;

import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class FennecWebSocketServlet extends WebSocketServlet {
    private final WebSocketCreator creator;

    public FennecWebSocketServlet(WebSocketCreator creator) {
        this.creator = creator;
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(0);
        factory.setCreator(creator);
    }
}
