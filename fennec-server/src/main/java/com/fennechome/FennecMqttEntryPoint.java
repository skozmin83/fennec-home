package com.fennechome;

import com.fennechome.common.PropertiesUtil;
import com.fennechome.controller.IComfortController;
import com.fennechome.controller.IEventListener;
import com.fennechome.controller.IEventSource;
import com.fennechome.controller.SimpleBoundariesController;
import com.fennechome.controller.mqtt.MqttDirectorExecutor;
import com.fennechome.controller.mqtt.MqttEventSource;
import com.fennechome.server.FennecMqttServer;
import com.fennechome.server.MongoStorage;
import com.fennechome.server.SensorInfoMongoSaver;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class FennecMqttEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(FennecMqttEntryPoint.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 1) {
            System.out.println("Usage java " + FennecMqttEntryPoint.class.getSimpleName() + " <config-name> ");
            System.exit(-1);
        }
        logger.info("Starting Fennec MongoDB and MQTT server with config [" + args[0] + "]. ");
        Configuration config = PropertiesUtil.getConfig(new File(args[0]));
        MongoStorage storage = new MongoStorage(config);
        SensorInfoMongoSaver mongoSaver = new SensorInfoMongoSaver(storage);
        FennecMqttServer server = new FennecMqttServer(Collections.singletonList(mongoSaver), config);
        IComfortController controller = initComfortController(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                controller.close();
                mongoSaver.close();
                server.close();
            } catch (Exception e) {
                logger.error("Unable to un-initialize Fennec MongoDB and MQTT server. ", e);
            }
        }));
        controller.start();
        server.start();
        logger.info("Broker started. ");
    }

    private static IComfortController initComfortController(Configuration config) {
        IEventSource eventSource = new MqttEventSource();
        return new SimpleBoundariesController(eventSource, new MqttDirectorExecutor(), 5, 100);
    }
}
