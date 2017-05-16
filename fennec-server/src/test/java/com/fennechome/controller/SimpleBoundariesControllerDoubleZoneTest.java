package com.fennechome.controller;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class SimpleBoundariesControllerDoubleZoneTest extends ControllerTest {
    private TestListenerCapturer source;
    private TestTimeProvider timer;

    @Before
    public void setUp() throws Exception {
        executor = new TestDirectionCapturer();
        source = new TestListenerCapturer();
        timer = new TestTimeProvider();
        FennecSimpleBoundariesController
                controller = new FennecSimpleBoundariesController(source, executor, 1, 100, timer);
        controller.start();

        source.listener.onZoneChangeEvent(new ZoneEvent(nextId(), 0, "zoneId", Sets.newHashSet(
                new Device("sensorDeviceId", DeviceType.TEMPERATURE_SENSOR),
                new Device("hoseDeviceId", DeviceType.HOSE)
        )));
        source.listener.onZoneChangeEvent(new ZoneEvent(nextId(), 0, "zoneId2", Sets.newHashSet(
                new Device("sensorDeviceId2", DeviceType.TEMPERATURE_SENSOR),
                new Device("hoseDeviceId2", DeviceType.HOSE)
        )));
        source.listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "zoneId", 20.0f, 25.0f));
        source.listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "zoneId2", 15.0f, 22.0f));
    }

    @Test
    public void testTwoZonesCoolThenOff() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorDeviceId", 26f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId2", "sensorDeviceId2", 26f, 50f));
        timer.time = 10;
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 15, "zoneId", "sensorDeviceId", 24f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 20, "zoneId2", "sensorDeviceId2", 17f, 50f));

        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:COOL,hoseStates:{hoseDeviceId:OPEN}}\n" +
                        "{id:3,thermostatState:COOL,hoseStates:{hoseDeviceId2:OPEN,hoseDeviceId:OPEN}}\n" +
                        "{id:4,thermostatState:COOL,hoseStates:{hoseDeviceId2:OPEN,hoseDeviceId:SHUT}}\n" +
                        "{id:5,thermostatState:OFF,hoseStates:{hoseDeviceId2:SHUT,hoseDeviceId:SHUT}}\n");
    }

    @Test
    public void testTwoZonesHeatThenOff() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorDeviceId", 19f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId2", "sensorDeviceId2", 13f, 50f));
        timer.time = 10;
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 15, "zoneId", "sensorDeviceId", 22f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 20, "zoneId2", "sensorDeviceId2", 25f, 50f));

        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:HEAT,hoseStates:{hoseDeviceId:OPEN}}\n" +
                        "{id:3,thermostatState:HEAT,hoseStates:{hoseDeviceId2:OPEN,hoseDeviceId:OPEN}}\n" +
                        "{id:4,thermostatState:HEAT,hoseStates:{hoseDeviceId2:OPEN,hoseDeviceId:SHUT}}\n" +
                        "{id:5,thermostatState:OFF,hoseStates:{hoseDeviceId2:SHUT,hoseDeviceId:SHUT}}\n");
    }

}