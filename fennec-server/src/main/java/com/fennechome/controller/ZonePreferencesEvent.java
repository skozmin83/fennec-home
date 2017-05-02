package com.fennechome.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sergey on 4/29/2017.
 */
public class ZonePreferencesEvent extends FennecEvent {
    String zoneId;
    float tempMin;
    float tempMax;

    public ZonePreferencesEvent(long eventId, long timeMillis, String zoneId, float tempMin, float tempMax) {
        super(eventId, timeMillis);
        this.zoneId = zoneId;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
    }

    @Override
    public String toString() {
        return "ZonePreferencesEvent{" +
                "zoneId='" + zoneId + '\'' +
                ", tempMin=" + tempMin +
                ", tempMax=" + tempMax +
                "} " + super.toString();
    }
}
