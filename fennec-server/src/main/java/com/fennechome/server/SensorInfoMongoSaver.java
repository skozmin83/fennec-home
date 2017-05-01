package com.fennechome.server;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.buffer.ByteBuf;
import org.bson.Document;

import java.io.UnsupportedEncodingException;

/**
 * Saves all the information published by devices to a file
 */
class SensorInfoMongoSaver extends AbstractInterceptHandler implements AutoCloseable {
    private final MongoStorage st = new MongoStorage();

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
    public void close() throws Exception {
        st.close();
    }
}
