package com.fennechome.controller;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class SimpleBoundariesControllerSingleZoneMultiSensorsTest extends ControllerTest {
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
        ZoneEvent event = new ZoneEvent(nextId(), 0, "zoneId", Sets.newHashSet(
                new Device("sensorDeviceIdTop", DeviceType.TEMPERATURE_SENSOR),
                new Device("sensorDeviceIdBottom", DeviceType.TEMPERATURE_SENSOR),
                new Device("hoseDeviceId", DeviceType.HOSE)
        ));
        source.listener.onZoneChangeEvent(event);
        source.listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "zoneId", 20.0f, 25.0f));
    }

    @Test
    public void testSimpleCool() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorDeviceIdTop", 26f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorDeviceIdBottom", 25f, 50f));
        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:COOL,hoseStates:{hoseDeviceId:OPEN}}\n");
    }

    @Test
    public void testSimpleCoolAverageTooLow() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorDeviceIdTop", 23f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorDeviceIdBottom", 26f, 50f));
        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:OFF,hoseStates:{hoseDeviceId:SHUT}}\n");
    }
}