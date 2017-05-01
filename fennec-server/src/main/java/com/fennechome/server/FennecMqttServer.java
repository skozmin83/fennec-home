package com.fennechome.server;

import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;

import java.io.IOException;
import java.util.Collections;

public class FennecMqttServer {
    public static void main(String[] args) throws InterruptedException, IOException {
//        PropertyConfigurator.configure("log4j.properties");
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);
        final Server mqttBroker = new Server();
        final SensorInfoMongoSaver mongoSaver = new SensorInfoMongoSaver();
        mqttBroker.startServer(classPathConfig, Collections.singletonList(mongoSaver));

        System.out.println("Broker started press [CTRL+C] to stop");
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Stopping mongo client.");
                mongoSaver.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Stopping broker");
            mqttBroker.stopServer();
            System.out.println("Broker stopped");
        }));
    }
}
