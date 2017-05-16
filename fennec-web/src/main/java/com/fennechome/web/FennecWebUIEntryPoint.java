package com.fennechome.web;

import com.fennechome.common.IMqttClientFactory;
import com.fennechome.common.MqttClientFactory;
import com.fennechome.common.PropertiesUtil;
import org.apache.commons.configuration2.Configuration;

import java.io.File;

public class FennecWebUIEntryPoint {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage java " + FennecWebUIEntryPoint.class.getSimpleName() + " <config-name> ");
            System.exit(-1);
        }
        try {
            Configuration config = PropertiesUtil.getConfig(new File(args[0]));
            String resourceBase = config.getString("fennec.web.resource-base");
            MqttClientFactory mqttClientFactory = new MqttClientFactory(
                    config.getString("fennec.mqtt.ui.broker"),
                    config.getString("fennec.mqtt.ui.user"),
                    config.getString("fennec.mqtt.ui.pwd")
            );
            FennecWebServer server = new FennecWebServer(config, mqttClientFactory, resourceBase);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            server.join();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-2);
        }
    }
}
