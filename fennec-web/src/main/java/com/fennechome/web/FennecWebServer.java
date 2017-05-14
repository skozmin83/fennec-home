package com.fennechome.web;

import com.fennechome.common.FennecException;
import com.fennechome.common.IMqttClientFactory;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FennecWebServer implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Server server;

    public FennecWebServer(Configuration configuration, IMqttClientFactory mqttClientFactory, String resourceBase) {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
//        server.setHandler(context);

        // Create the ResourceHandler. It is the object that will actually handle the request for a given file. It is
        // a Jetty Handler object so it is suitable for chaining with other handlers as you will see in other examples.
        ResourceHandler resourceHandler = new ResourceHandler();

        // Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
        // In this example it is the current directory but it can be configured to anything that the jvm has access to.
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase(resourceBase);
//        resourceHandler.setDirAllowed(false);

        // Add the ResourceHandler to the server.
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resourceHandler, context});
        server.setHandler(handlers);

        // Add a websocket to a specific path spec
        try {
            MqttUiEventSource source = new MqttUiEventSource(mqttClientFactory);
            WebSocketCreator sensorEventCreator = (req, resp) -> new FennecSensorEventWebSocket(configuration, source);
            WebSocketServlet sensorWs = new FennecWebSocketServlet(sensorEventCreator);
            context.addServlet(new ServletHolder("ws-temperature", sensorWs), "/temperature.ws");


            WebSocketCreator zoneEventsCreator = (req, resp) -> new FennecZoneEventWebSocket(configuration, source);
            WebSocketServlet zoneWs = new FennecWebSocketServlet(zoneEventsCreator);
            context.addServlet(new ServletHolder("ws-zone", zoneWs), "/zone.ws");
        } catch (Exception e) {
            throw new FennecException("Unable to get a connection. ");
        }
        context.addServlet(DeviceTemperatureCsvServlet.class, "/temperature.csv");
    }

    public void start() {
        try {
            server.start();
            server.dump(System.err);
        } catch (Exception e) {
            throw new FennecException("Unable to start Web UI server. ", e);
        }
    }

    public void join() {
        try {
            server.join();
        } catch (InterruptedException e) {
            throw new FennecException("Unable to join. ", e);
        }
    }

    @Override
    public void close() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new FennecException("Unable to stop Web UI server. ", e);
        }
    }
}
