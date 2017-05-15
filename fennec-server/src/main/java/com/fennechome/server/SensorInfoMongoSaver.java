package com.fennechome.server;

import com.fennechome.common.FennecException;
import com.fennechome.common.MongoAsyncStorage;
import com.fennechome.mqtt.IMessageListener;
import org.bson.Document;

/**
 * Saves all the information published by devices to a file
 */
public class SensorInfoMongoSaver implements AutoCloseable, IMessageListener {
    private final MongoAsyncStorage st;

    public SensorInfoMongoSaver(MongoAsyncStorage st) {
        this.st = st;
    }

    @Override
    public void onMessage(String topicName, long currentTime, Document json) {
        st.store(json, topicName);
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
