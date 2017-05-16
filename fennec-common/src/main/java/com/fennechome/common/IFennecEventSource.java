package com.fennechome.common;

import org.bson.Document;

/**
 * Since there could be lots of sockets subscribed to the same topic, disconnect of one leads to disconnect to another
 * this source would help to keep a single connection and filter
 */
public interface IFennecEventSource {
    void subscribe(String topic, Listener l);

    void unsubscribe(String topic, Listener l);

    interface Listener {
        void onEvent(String topic, Document msg);
    }
}
