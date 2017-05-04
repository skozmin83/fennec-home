package com.fennechome.server;

import com.fennechome.common.PropertiesUtil;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class FennecMqttEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(FennecMqttEntryPoint.class);
    public static void main(String[] args) throws InterruptedException, IOException {
        logger.info("Starting Fennec MongoDB and MQTT server. ");
        Configuration config = PropertiesUtil.getConfig(new File("fennechome-mqtt-server.properties"));
        MongoStorage storage = new MongoStorage(config);
        SensorInfoMongoSaver mongoSaver = new SensorInfoMongoSaver(storage);
        FennecMqttServer server = new FennecMqttServer(Collections.singletonList(mongoSaver));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mongoSaver.close();
            server.close();
        }));
        server.start();
        logger.info("Broker started. ");
    }
}
