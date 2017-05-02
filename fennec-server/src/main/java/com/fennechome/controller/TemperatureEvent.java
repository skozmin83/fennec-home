package com.fennechome.controller;

/**
 * Created by sergey on 4/29/2017.
 */
public class TemperatureEvent extends FennecEvent {
    String zoneId;
    String sensorId;
    float temperature;
    float humidity;

    public TemperatureEvent(long eventId, long timeMillis, String zoneId, String sensorId, float temperature, float humidity) {
        super(eventId, timeMillis);
        this.zoneId = zoneId;
        this.sensorId = sensorId;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    @Override
    public String toString() {
        return "TemperatureEvent{" +
                "zoneId='" + zoneId + '\'' +
                ", sensorId='" + sensorId + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                '}';
    }
}
