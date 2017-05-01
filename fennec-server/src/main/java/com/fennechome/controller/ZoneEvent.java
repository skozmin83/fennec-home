package com.fennechome.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by sergey on 4/29/2017.
 */
public class ZoneEvent {
    String zoneId;
    Set<Device> devices = new HashSet<>();

    public ZoneEvent(String zoneId, Set<Device> devices) {
        this.zoneId = zoneId;
        this.devices = devices;
    }
}
