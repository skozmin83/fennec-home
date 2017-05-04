package com.fennechome.server;

import com.fennechome.common.FennecException;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import org.bson.Document;

/**
 * Saves all the information published by devices to a file
 */
public class SensorInfoMongoSaver extends AbstractInterceptHandler implements AutoCloseable {
    private final MongoStorage st;

    public SensorInfoMongoSaver(MongoStorage st) {
        this.st = st;
    }

    public String getID() {
        return getClass().getSimpleName();
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        String topicName = msg.getTopicName();
        String payload = null;
        try {
            payload = JsonMongoMsgParser.decodeString(msg.getPayload());
            Document dbObject = Document.parse(payload);
            st.store(dbObject, topicName);
        } catch (Exception e) {
            System.out.println("topicName = " + topicName + ", msg: " + payload);
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            st.close();
        } catch (Exception e) {
            throw new FennecException("Unable to close Mongo storage.", e);
        }
    }
}
