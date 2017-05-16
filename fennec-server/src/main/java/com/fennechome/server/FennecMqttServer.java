package com.fennechome.server;

import com.fennechome.common.FennecException;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;
import io.moquette.server.netty.MessageBuilder;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class FennecMqttServer implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final IConfig classPathConfig;
    private final Server mqttBroker;
    private final List<? extends InterceptHandler> handlers;

    public FennecMqttServer(List<? extends InterceptHandler> handlers, Configuration config) {
        this.handlers = handlers;
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        classPathConfig = new ResourceLoaderConfig(classpathLoader, config.getString("fennec.mqtt.config"));
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

    @Deprecated
    public void internalPublish(String topic, String forClientId, byte[] payload) {
        MqttPublishMessage message = MessageBuilder.publish()
                .topicName(topic)
                .retained(true)
//                .qos(MqttQoS.AT_MOST_ONCE)
//                .qos(MqttQoS.AT_LEAST_ONCE)
                .qos(MqttQoS.EXACTLY_ONCE) // todo check if we can build it on acks
                .payload(payload)
                .build();
        mqttBroker.internalPublish(message, forClientId);
    }

    @Override
    public void close() {
        logger.info("Stopping broker");
        mqttBroker.stopServer();
        logger.info("Broker stopped");
    }
}
