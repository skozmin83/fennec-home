package com.fennechome.controller;

public interface IFennecControllerEventSource {
    void subscribe(IFennectControllerEventListener listener);
    void unsubscribe(IFennectControllerEventListener listener);

    interface IFennectControllerEventListener {
        void onTemperatureEvent(TemperatureEvent event);
        void onTimeEvent(TimeEvent event);
        void onZonePreferencesEvent(ZonePreferencesEvent event);
        void onZoneChangeEvent(ZoneEvent event);
    }
}
