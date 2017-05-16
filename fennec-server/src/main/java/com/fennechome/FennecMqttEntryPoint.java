package com.fennechome;

import com.fennechome.common.FennecMqttEventSource;
import com.fennechome.common.IFennecEventSource;
import com.fennechome.common.MqttClientFactory;
import com.fennechome.common.PropertiesUtil;
import com.fennechome.controller.IFennecComfortController;
import com.fennechome.controller.IFennecControllerEventSource;
import com.fennechome.controller.FennecSimpleBoundariesController;
import com.fennechome.mqtt.FennecMqttControllerEventSource;
import com.fennechome.mqtt.MqttDirectionExecutor;
import com.fennechome.server.FennecMqttServer;
import com.fennechome.common.MongoAsyncStorage;
import com.fennechome.server.SensorInfoMongoSaver;
import com.google.common.collect.Lists;
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
        try {
            logger.info("Starting Fennec MongoDB and MQTT server with config [" + args[0] + "]. ");
            Configuration config = PropertiesUtil.getConfig(new File(args[0]));

            FennecMqttControllerEventSource controllerEventSource = new FennecMqttControllerEventSource(config);

            String tempCollection = config.getString("fennec.mongo.sensor.temperature.collection");
            String zoneEventsCollection = config.getString("fennec.mongo.zone-events.collection");
            MongoAsyncStorage storage = new MongoAsyncStorage(config);
            SensorInfoMongoSaver temperatureMongoSaver = new SensorInfoMongoSaver(storage, tempCollection);
            SensorInfoMongoSaver zoneEventsMongoSaver = new SensorInfoMongoSaver(storage, zoneEventsCollection);

            MqttClientFactory mqttClientFactory = new MqttClientFactory(
                    config.getString("fennec.mqtt.controller.broker"),
                    config.getString("fennec.mqtt.controller.user"),
                    config.getString("fennec.mqtt.controller.pwd")
            );
            FennecMqttServer server = new FennecMqttServer(Collections.emptyList(), config);
            MqttDirectionExecutor mqttDirectionExecutor = new MqttDirectionExecutor(config, server);
            IFennecComfortController controller =
                    initComfortController(config, controllerEventSource, mqttDirectionExecutor);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    controller.close();
                    mqttClientFactory.close();
                    storage.close();
                    server.close();
                } catch (Exception e) {
                    logger.error("Unable to un-initialize Fennec MongoDB and MQTT server. ", e);
                }
            }));
            server.start();
            controller.start();

            IFennecEventSource mqttEventSource = new FennecMqttEventSource(mqttClientFactory);
            mqttEventSource.subscribe(config.getString("fennec.mqtt.devices-base-topic") + "#", controllerEventSource);
            mqttEventSource.subscribe(config.getString("fennec.mqtt.devices-base-topic") + "#", temperatureMongoSaver);
            mqttEventSource.subscribe(config.getString("fennec.mqtt.ui-base-topic") + "#", zoneEventsMongoSaver);

            logger.info("Broker started. ");
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-2);
        }
    }

    private static IFennecComfortController initComfortController(Configuration config,
                                                                  IFennecControllerEventSource eventSource,
                                                                  MqttDirectionExecutor mqttDirectionExecutor) {
        return new FennecSimpleBoundariesController(eventSource, mqttDirectionExecutor, 5, 100);
    }
}
