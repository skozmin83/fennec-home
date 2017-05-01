package com.fennechome.server;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;
import io.moquette.server.netty.MessageBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import static java.util.Arrays.asList;

public class EmbeddedLauncher {
    static class PublisherListener extends AbstractInterceptHandler {
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            try {
                System.out.println("Received on topic: " + msg.getTopicName() + " content: " + decodeString(msg.getPayload()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
//        PropertyConfigurator.configure("log4j.properties");
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);

        final Server mqttBroker = new Server();
        List<? extends InterceptHandler> userHandlers = asList(new PublisherListener());
        mqttBroker.startServer(classPathConfig, userHandlers);

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

        Thread.sleep(1000);
        mqttBroker.addInterceptHandler(new PublisherListener());
//        MqttSubscribeMessage msg = MessageBuilder.subscribe().addSubscription(MqttQoS.AT_MOST_ONCE, "#").build();
//        mqttBroker.internalPublish(msg, "INTRLPUB");
        System.out.println("Before self publish");
        MqttPublishMessage message = MessageBuilder.publish()
            .topicName("/exit")
            .retained(true)
//        qos(MqttQoS.AT_MOST_ONCE);
//        qQos(MqttQoS.AT_LEAST_ONCE);
            .qos(MqttQoS.EXACTLY_ONCE)
            .payload("Hello World!!".getBytes())
            .build();
        mqttBroker.internalPublish(message, "INTRLPUB");
        System.out.println("After self publish");
    }


    /**
     * Load a string from the given buffer, reading first the two bytes of len
     * and then the UTF-8 bytes of the string.
     *
     * @return the decoded string or null if NEED_DATA
     */
    static String decodeString(ByteBuf in) throws UnsupportedEncodingException {
        return new String(readFixedLengthContent(in), "UTF-8");
    }

    /**
     * Read a byte array from the buffer, use two bytes as length information followed by length bytes.
     * */
    static byte[] readFixedLengthContent(ByteBuf in) throws UnsupportedEncodingException {
        if (in.readableBytes() < 2) {
            return new byte[]{};
        }
        int strLen = in.readableBytes();
//        if (in.readableBytes() < strLen) {
//            return null;
//        }
        byte[] strRaw = new byte[strLen];
        in.readBytes(strRaw);

        return strRaw;
    }
}
