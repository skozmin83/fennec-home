package com.fennechome.controller;

/**
 * Created by sergey on 5/2/2017.
 */
public class FennecEvent {
    long eventId;
    long timeMillis;

    public FennecEvent() {
    }

    public FennecEvent(long eventId, long timeMillis) {
        this.eventId = eventId;
        this.timeMillis = timeMillis;
    }
}
