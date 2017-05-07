package com.fennechome.controller;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleBoundariesControllerSingleZoneTest extends ControllerTest {
    private TestListenerCapturer source;
    private TestTimeProvider timer;

    @Before
    public void setUp() throws Exception {
        executor = new TestDirectionCapturer();
        source = new TestListenerCapturer();
        timer = new TestTimeProvider();
        SimpleBoundariesController controller = new SimpleBoundariesController(source, executor, 1, 100, timer);
        controller.start();
        source.listener.onZoneChangeEvent(new ZoneEvent(nextId(), 0, "zoneId", Sets.newHashSet(
                new Device("sensorDeviceId", DeviceType.TEMPERATURE_SENSOR),
                new Device("hoseDeviceId", DeviceType.HOSE)
        )));
        source.listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "zoneId", 20.0f, 25.0f));
    }

    @Test
    public void testSimpleCool() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 26f, 50f));
        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:COOL,hoseStates:{hoseDeviceId:OPEN}}\n");
    }

    @Test
    public void testSimpleCoolThenOff() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 26f, 50f));
        timer.time = 10;
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 15, "zoneId", "sensorId", 24f, 50f));

        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:COOL,hoseStates:{hoseDeviceId:OPEN}}\n" +
                        "{id:3,thermostatState:OFF,hoseStates:{hoseDeviceId:SHUT}}\n");
    }

    @Test
    public void testSimpleHeatThenOff() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        timer.time = 10;
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 15, "zoneId", "sensorId", 21f, 50f));

        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:HEAT,hoseStates:{hoseDeviceId:OPEN}}\n" +
                        "{id:3,thermostatState:OFF,hoseStates:{hoseDeviceId:SHUT}}\n");
    }


    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectPreferences() throws Exception {
        source.listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "zoneId", 20.0f, 20.5f));
    }

    @Test
    public void testNoSwingsOnRapidTempChange() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        timer.time = 10;
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 15, "zoneId", "sensorId", 23f, 50f));
        timer.time = 20;
        // shouldn't trigger cooling
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 25, "zoneId", "sensorId", 27f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 26, "zoneId", "sensorId", 28f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 27, "zoneId", "sensorId", 31f, 50f));

        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:HEAT,hoseStates:{hoseDeviceId:OPEN}}\n" +
                        "{id:3,thermostatState:OFF,hoseStates:{hoseDeviceId:SHUT}}\n");
    }

    @Test
    public void testSwingsWithingAllowedTime() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 1, "zoneId", "sensorId", 19f, 50f));
        timer.time = 10;
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 15, "zoneId", "sensorId", 23f, 50f));
        timer.time = 20;
        // shouldn't trigger cooling
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 25, "zoneId", "sensorId", 27f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 26, "zoneId", "sensorId", 28f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(nextId(), 27 + SimpleBoundariesController.FASTEST_DIRECTION_TIME_SWITCH_MS, "zoneId", "sensorId", 31f, 50f));

        checkDirections(
                "{id:1,thermostatState:OFF,hoseStates:{}}\n" +
                        "{id:2,thermostatState:HEAT,hoseStates:{hoseDeviceId:OPEN}}\n" +
                        "{id:3,thermostatState:OFF,hoseStates:{hoseDeviceId:SHUT}}\n" +
                        "{id:4,thermostatState:COOL,hoseStates:{hoseDeviceId:OPEN}}\n");
    }

    @Test
    public void testSameEventReplays() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent(1, 1, "zoneId", "sensorId", 19f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(1, 15, "zoneId", "sensorId", 23f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(1, 15, "zoneId", "sensorId", 23f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(1, 15, "zoneId", "sensorId", 23f, 50f));
        source.listener.onTemperatureEvent(new TemperatureEvent(1, 15, "zoneId", "sensorId", 23f, 50f));

        checkDirections("{id:1,thermostatState:OFF,hoseStates:{}}\n");
    }
}