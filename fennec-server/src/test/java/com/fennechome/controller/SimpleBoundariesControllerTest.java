package com.fennechome.controller;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;

public class SimpleBoundariesControllerTest {

    private ListenerCapturer source;
    private IDirectionExecutor executor;
    private TestTimeProvider timer;

    @Before
    public void setUp() throws Exception {
        executor = Mockito.mock(IDirectionExecutor.class);
        source = new ListenerCapturer();
        timer = new TestTimeProvider();
        SimpleBoundariesController controller = new SimpleBoundariesController(source, executor, 1, 100, timer);

        ZoneEvent event = new ZoneEvent("zoneId", Sets.newHashSet(
                new Device("sensorDeviceId", DeviceType.TEMPERATURE_SENSOR),
                new Device("hoseDeviceId", DeviceType.HOSE)
        ));
        source.listener.onZoneChangeEvent(event);
        source.listener.onZonePreferencesEvent(new ZonePreferencesEvent("zoneId", 20.0f, 25.0f));
    }

    @Test
    public void testSimpleOn() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent("zoneId", "sensorId", 1, 1, 26f, 50f));

        Mockito.verify(executor).send(
                new Direction(
                        0,
                        ThermostatState.COOL, new HashMap<String, HoseState>() {{
                    put("hoseDeviceId", HoseState.OPEN);
                }}));
    }

    @Test
    public void testSimpleOnThenOff() throws Exception {
        source.listener.onTemperatureEvent(new TemperatureEvent("zoneId", "sensorId", 1, 1, 26f, 50f));
        timer.time = 10;
        source.listener.onTemperatureEvent(new TemperatureEvent("zoneId", "sensorId", 2, 15, 24f, 50f));

        Mockito.verify(executor).send(
                new Direction(
                        0,
                        ThermostatState.COOL, new HashMap<String, HoseState>() {{
                    put("hoseDeviceId", HoseState.OPEN);
                }}));
        Mockito.verify(executor).send(
                new Direction(
                        1,
                        ThermostatState.OFF, new HashMap<String, HoseState>() {{
                    put("hoseDeviceId", HoseState.SHUT);
                }}));
    }

    private static class ListenerCapturer implements IEventSource {
        IEventListener listener;

        @Override
        public void subscribe(IEventListener listener) {
            this.listener = listener;
        }
    }

    private static class TestTimeProvider implements ITimeProvider {
        long time;
        @Override
        public long currentTime() {
            return time;
        }
    }
}