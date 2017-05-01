package com.fennechome.controller;

/**
 * Created by sergey on 4/29/2017.
 */
public interface IEventListener {
    void onTemperatureEvent(TemperatureEvent event);
    void onTimeEvent(TimeEvent event);
    void onZonePreferencesEvent(ZonePreferencesEvent event);
    void onZoneChangeEvent(ZoneEvent event);
}
