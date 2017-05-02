package com.fennechome.controller;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sergey on 4/29/2017.
 */
public class ZoneEvent extends FennecEvent {
    String zoneId;
    Set<Device> devices = new HashSet<>();

    public ZoneEvent(long eventId, long timeMillis, String zoneId, Set<Device> devices) {
        super(eventId, timeMillis);
        this.zoneId = zoneId;
        this.devices = devices;
    }

    @Override
    public String toString() {
        return "ZoneEvent{" +
                "zoneId='" + zoneId + '\'' +
                ", devices=" + devices +
                "} " + super.toString();
    }
}
