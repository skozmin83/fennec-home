package com.fennechome.mqtt;

import com.fennechome.common.FennecException;
import com.fennechome.common.IFennecEventSource;
import com.fennechome.controller.*;
import com.google.common.collect.Sets;
import org.apache.commons.configuration2.Configuration;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Mqtt events source
 */
public class FennecMqttControllerEventSource
        implements IFennecControllerEventSource, IFennecEventSource.Listener, AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String        devicePrefix;
    private final ITimeProvider timeProvider;
    private long id = 0;
    private IFennectControllerEventListener listener;

    // todo parse real mqtt events for this info
    private Map<String, String> deviceToZoneMapping = new HashMap<String, String>() {{
        put("A0:20:A6:16:A6:34/dht22-top", "bedroom");
        put("A0:20:A6:16:A6:34/dht22-bottom", "bedroom");
        put("A0:20:A6:16:A7:0A/dht22-top", "livingroom");
        put("A0:20:A6:16:A7:0A/dht22-bottom", "livingroom");
    }};

    public FennecMqttControllerEventSource(Configuration config, ITimeProvider timeProvider) {
        devicePrefix = config.getString("fennec.mqtt.devices-base-topic");
        this.timeProvider = timeProvider;
    }

    @Override
    public void onEvent(String topic, byte[] msg, long ts) {
        try {
            Document json = Document.parse(new String(msg));
            String deviceId = topic.substring(devicePrefix.length(), topic.length());
            String zone = deviceToZoneMapping.get(deviceId);
            if (zone != null) {
                float temperature = json.getDouble("t").floatValue();
                float humidity = json.getDouble("h").floatValue();
                listener.onTemperatureEvent(new TemperatureEvent(nextId(),
                                                                 System.currentTimeMillis(),
                                                                 zone,
                                                                 deviceId,
                                                                 temperature,
                                                                 humidity));
            }
        } catch (Exception e) {
            throw new FennecException("Unable to handle temperature event for topic [" + topic
                                              + "], event [" + msg + "]. ", e);
        }
    }

    @Override
    public void subscribe(IFennectControllerEventListener listener) {
        this.listener = listener;

        // todo in order to init system, we need to load info from database first
        // * read zones and config from mongo
        // * subscribe to various realtime mqtt updates

        // for now we just hardcode the config
        listener.onZoneChangeEvent(new ZoneEvent(nextId(), 0, "default", Sets.newHashSet(
        )));
        listener.onZoneChangeEvent(new ZoneEvent(nextId(), 0, "bedroom", Sets.newHashSet(
                new Device("A0:20:A6:16:A6:34/dht22-top", DeviceType.TEMPERATURE_SENSOR),
                new Device("A0:20:A6:16:A6:34/dht22-bottom", DeviceType.TEMPERATURE_SENSOR),
                new Device("A0:20:A6:16:A6:34/hose", DeviceType.HOSE)
        )));
        listener.onZoneChangeEvent(new ZoneEvent(nextId(), 0, "livingroom", Sets.newHashSet(
                new Device("A0:20:A6:16:A7:0A/dht22-top", DeviceType.TEMPERATURE_SENSOR),
                new Device("A0:20:A6:16:A7:0A/dht22-bottom", DeviceType.TEMPERATURE_SENSOR),
                new Device("A0:20:A6:16:A7:0A/hose", DeviceType.HOSE)
        )));
        listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "bedroom", 24.0f, 28.0f));
        listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "livingroom", 24.0f, 28.0f));

        // todo re-do to a time service
        Thread timerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                listener.onTimeEvent(new TimeEvent(timeProvider.currentTime()));
                try {
                    Thread.sleep(10000); // todo make configurable
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    @Override
    public void unsubscribe(IFennectControllerEventListener listener) {
        this.listener = null;
    }

    private long nextId() {
        return id++;
    }

    @Override
    public void close() throws Exception {
    }
}
