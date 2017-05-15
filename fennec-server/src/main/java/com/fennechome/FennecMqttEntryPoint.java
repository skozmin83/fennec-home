package com.fennechome;

import com.fennechome.common.PropertiesUtil;
import com.fennechome.controller.IComfortController;
import com.fennechome.controller.IEventSource;
import com.fennechome.controller.SimpleBoundariesController;
import com.fennechome.mqtt.MqttDirectionExecutor;
import com.fennechome.mqtt.MqttEventSource;
import com.fennechome.mqtt.MqttInterceptor;
import com.fennechome.server.FennecMqttServer;
import com.fennechome.common.MongoAsyncStorage;
import com.fennechome.server.SensorInfoMongoSaver;
import com.google.common.collect.Lists;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

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
            MongoAsyncStorage storage = new MongoAsyncStorage(config);
            SensorInfoMongoSaver mongoSaver = new SensorInfoMongoSaver(storage);
            MqttEventSource mqttEventSource = new MqttEventSource(config);

            MqttInterceptor mqttMessagesListener = new MqttInterceptor();
            mqttMessagesListener.addListener(mongoSaver);
            mqttMessagesListener.addListener(mqttEventSource);
            FennecMqttServer server = new FennecMqttServer(Lists.newArrayList(mqttMessagesListener), config);
            MqttDirectionExecutor mqttDirectionExecutor = new MqttDirectionExecutor(config, server);
            IComfortController controller = initComfortController(config, mqttEventSource, mqttDirectionExecutor);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    controller.close();
                    mongoSaver.close();
                    server.close();
                } catch (Exception e) {
                    logger.error("Unable to un-initialize Fennec MongoDB and MQTT server. ", e);
                }
            }));
            server.start();
            controller.start();
            logger.info("Broker started. ");
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-2);
        }
    }

    private static IComfortController initComfortController(Configuration config, IEventSource eventSource, MqttDirectionExecutor mqttDirectionExecutor) {
        return new SimpleBoundariesController(eventSource, mqttDirectionExecutor, 5, 100);
    }
}
