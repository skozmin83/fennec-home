package com.fennechome.web;

/**
 * Since there could be lots of sockets subscribed to the same topic, disconnect of one leads to disconnect to another
 * this source would help to keep a single connection and filter
 */
public interface IUiEventSource {
    void subscribe(String topic, IUiEventListener l);
    void unsubscribe(String topic, IUiEventListener l);
    interface IUiEventListener {
        void onEvent(String topic, String msg);
    }
}
