package com.fennechome.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sergey on 4/29/2017.
 */
public class ZonePreferencesEvent {
    String zoneId;
    float tempMin;
    float tempMax;

    public ZonePreferencesEvent(String zoneId, float tempMin, float tempMax) {
        this.zoneId = zoneId;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
    }

    @Override
    public String toString() {
        return "ZonePreferencesEvent{" +
                "zoneId='" + zoneId + '\'' +
                ", tempMax=" + tempMax +
                ", tempMin=" + tempMin +
                '}';
    }
}
