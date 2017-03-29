/*
* Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
* ------------------------------------------------------
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* and Apache License v2.0 which accompanies this distribution.
*
* The Eclipse Public License is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* The Apache License v2.0 is available at
* http://www.opensource.org/licenses/apache2.0.php
*
* You may elect to redistribute this code under either of these licenses.
*/
package com.fennechome.server;

import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;

import java.io.IOException;
import java.util.Collections;

public class FileSavingMqtt {
    public static void main(String[] args) throws InterruptedException, IOException {
//        PropertyConfigurator.configure("log4j.properties");
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);
        final Server mqttBroker = new Server();
        mqttBroker.startServer(classPathConfig, Collections.singletonList(new DeviceInfoSaverListener()));

        System.out.println("Broker started press [CTRL+C] to stop");
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Stopping broker");
                mqttBroker.stopServer();
                System.out.println("Broker stopped");
            }
        });
    }
}
