package com.fennechome.mqtt;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MqttInterceptor extends AbstractInterceptHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<IMessageListener> listeners = new ArrayList<>();
    @Override
    public void onPublish(InterceptPublishMessage msg) {
        long currentTime = System.currentTimeMillis();
        String topicName = msg.getTopicName();
        Document json = null;
        try {
            String payload = JsonMongoMsgParser.decodeString(msg.getPayload());
            json = Document.parse(payload);
            json.append("topic", topicName);
            json.append("ts", new Date(currentTime));
            logger.info("publishing message [{}]", json);
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onMessage(topicName, currentTime, json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unable to convert message to json {}.", json, e);
        }
    }

    @Override
    public String getID() {
        return getClass().getSimpleName();
    }

    public void addListener(IMessageListener listener) {
        listeners.add(listener);
    }
}
