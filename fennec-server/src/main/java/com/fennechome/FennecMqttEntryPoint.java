package com.fennechome;

import com.fennechome.common.FennecMqttEventSource;
import com.fennechome.common.IFennecEventSource;
import com.fennechome.common.MqttClientFactory;
import com.fennechome.common.PropertiesUtil;
import com.fennechome.controller.IFennecComfortController;
import com.fennechome.controller.FennecSimpleBoundariesController;
import com.fennechome.mqtt.FennecMqttControllerEventSource;
import com.fennechome.mqtt.MqttDirectionExecutor;
import com.fennechome.server.FennecMqttServer;
import com.fennechome.common.MongoAsyncStorage;
import com.fennechome.server.SensorInfoMongoSaver;
import org.apache.commons.configuration2.Configuration;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

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
                    new FennecSimpleBoundariesController(controllerEventSource, mqttDirectionExecutor, 5, 100);

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

            // subscribe to mqtt
            IFennecEventSource mqttEventSource = new FennecMqttEventSource(mqttClientFactory);
            String deviceTopicBase = config.getString("fennec.mqtt.devices-base-topic");
            mqttEventSource.subscribe(deviceTopicBase + "#", controllerEventSource);
            mqttEventSource.subscribe(deviceTopicBase + "#",
                                      (topic, msg, ts) -> {
                                          Document json = Document.parse(new String(msg));
                                          json = json.append("id",
                                                           topic.substring(deviceTopicBase.length(), topic.length()));
                                          json = json.append("ts", ts);
                                          json = json.append("time", new Date(ts));
                                          storage.store(tempCollection, json);
                                      });
            String usTopicBase = config.getString("fennec.mqtt.ui-base-topic");
            mqttEventSource.subscribe(usTopicBase + "#",
                                      (topic, msg, ts) -> {
                                          Document json = Document.parse(new String(msg));
                                          json = json.append("id",
                                                           topic.substring(usTopicBase.length(), topic.length()));
                                          json.put("time", new Date(Long.parseLong(json.getString("ts"))));
                                          storage.store(zoneEventsCollection, json);
                                      });

            logger.info("Broker started. ");
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-2);
        }
    }

}
