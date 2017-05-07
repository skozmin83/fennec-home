package com.fennechome.web;

import com.fennechome.common.PropertiesUtil;
import org.apache.commons.configuration2.Configuration;

import java.io.File;

public class FennecWebUIEntryPoint {
    public static void main(String[] args) {
        String resourceBase = args.length > 1 ? args[0] : "./fennec-web/src/main/resources/webroot";
        Configuration config = PropertiesUtil.getConfig(new File("fennechome-ui-server.properties"));
        IMqttClientFactory mqttClientFactory = new MqttClientFactory(config);
        FennecWebServer server = new FennecWebServer(config, mqttClientFactory, resourceBase);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.join();
    }
}
