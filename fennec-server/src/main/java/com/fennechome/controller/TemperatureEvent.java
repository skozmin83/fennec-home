package com.fennechome.controller;

/**
 * Created by sergey on 4/29/2017.
 */
public class TemperatureEvent {
    String zoneId;
    String sensorId;
    long eventId;
    long timeMillis;
    float temperature;
    float humidity;

    public TemperatureEvent(String zoneId, String sensorId, long eventId, long timeMillis, float temperature, float humidity) {
        this.zoneId = zoneId;
        this.sensorId = sensorId;
        this.eventId = eventId;
        this.timeMillis = timeMillis;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    @Override
    public String toString() {
        return "TemperatureEvent{" +
                "zoneId='" + zoneId + '\'' +
                ", sensorId='" + sensorId + '\'' +
                ", eventId=" + eventId +
                ", timeMillis=" + timeMillis +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                '}';
    }
}
