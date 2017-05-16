package com.fennechome.controller;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class SimpleBoundariesControllerSingleZoneLongSeriesTest extends ControllerTest {
    private TestListenerCapturer source;
    private TestTimeProvider timer;

    @Before
    public void setUp() throws Exception {
        executor = new TestDirectionCapturer();
        source = new TestListenerCapturer();
        timer = new TestTimeProvider();
        // average of last five 5
        FennecSimpleBoundariesController controller = new FennecSimpleBoundariesController(source, executor, 5, 7, timer);
        controller.start();
        source.listener.onZoneChangeEvent(new ZoneEvent(nextId(), 0, "zoneId", Sets.newHashSet(
                new Device("sensorDeviceId", DeviceType.TEMPERATURE_SENSOR),
                new Device("hoseDeviceId", DeviceType.HOSE)
        )));
        source.listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "zoneId", 18.0f, 25.0f));
    }

    @Test
    public void testNewerValues() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));

        // now raise temperature
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 15, "zoneId", "sensorId", 26f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 15, "zoneId", "sensorId", 26f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 25, "zoneId", "sensorId", 26f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 25, "zoneId", "sensorId", 26f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 26, "zoneId", "sensorId", 26f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 26, "zoneId", "sensorId", 26f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 26, "zoneId", "sensorId", 26f, 50f));

        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:OFF,hoseStates:{hoseDeviceId:SHUT}}\n" +
                        "{id:3,thermostatState:COOL,hoseStates:{hoseDeviceId:OPEN}}\n");
    }
}