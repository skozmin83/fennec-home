package com.fennechome.server;

import com.fennechome.common.FennecException;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class FennecMqttServer implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final IConfig classPathConfig;
    private final Server mqttBroker;
    private final List<? extends InterceptHandler> handlers;

    public FennecMqttServer(List<? extends InterceptHandler> handlers) {
        this.handlers = handlers;
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        classPathConfig = new ResourceLoaderConfig(classpathLoader);
        mqttBroker = new Server();
    }

    public void start() {
        try {
            logger.info("Starting broker");
            mqttBroker.startServer(classPathConfig, handlers);
        } catch (IOException e) {
            throw new FennecException("Unable to start MQTT server. ", e);
        }
    }

    @Override
    public void close() {
        logger.info("Stopping broker");
        mqttBroker.stopServer();
        logger.info("Broker stopped");
    }
}
