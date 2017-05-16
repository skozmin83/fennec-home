package com.fennechome.server;

import com.fennechome.common.IFennecEventSource;
import com.fennechome.common.MongoAsyncStorage;
import org.bson.Document;

/**
 * Saves all the information published by devices to a file
 */
public class SensorInfoMongoSaver implements IFennecEventSource.Listener {
    private final MongoAsyncStorage st;
    private final String            store;

    public SensorInfoMongoSaver(MongoAsyncStorage st, String store) {
        this.st = st;
        this.store = store;
    }

    @Override
    public void onEvent(String topic, Document msg) {
        st.store(store, msg);
    }
}
