package com.fennechome.mqtt;

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
public class MqttEventSource implements IEventSource, AutoCloseable, IMessageListener {
    private final String devicePrefix;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private long id = 0;
    private IEventListener listener;
//    private Thread t;

    // todo parse real mqtt events for this info
    private Map<String, String> deviceToZoneMapping = new HashMap<String, String>() {{
        put("A0:20:A6:16:A6:34/dht22-top", "bedroom");
        put("A0:20:A6:16:A6:34/dht22-bottom", "bedroom");
        put("A0:20:A6:16:A7:0A/dht22-top", "livingroom");
        put("A0:20:A6:16:A7:0A/dht22-bottom", "livingroom");
    }};

    public MqttEventSource(Configuration config) {
        devicePrefix = config.getString("fennec.mqtt.devices-base-topic");
    }

    @Override
    public void onMessage(String topicName, long currentTime, Document json) {
        if (topicName.startsWith(devicePrefix)) {
            String deviceId = topicName.substring(devicePrefix.length(), topicName.length());
            String zone = deviceToZoneMapping.get(deviceId);
            float temperature = json.getDouble("t").floatValue();
            float humidity = json.getDouble("h").floatValue();
            if (zone != null) {
                listener.onTemperatureEvent(new TemperatureEvent(nextId(), currentTime, zone, deviceId, temperature, humidity));
            }
        }
    }

    @Override
    public void subscribe(IEventListener listener) {
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
        listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "bedroom", 22.0f, 24.0f));
        listener.onZonePreferencesEvent(new ZonePreferencesEvent(nextId(), 0, "livingroom", 22.0f, 24.0f));

        // just do sine signal for the time being
//        t = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(true) {
//                    try {
//                        Thread.sleep(1000);
//                        int tempBase = 23;
//                        float sinPart = (float) (Math.sin(Math.toRadians(id * 2)) * 3);
//                        long ts = System.currentTimeMillis();
//                        TemperatureEvent te = new TemperatureEvent(nextId(), ts, "bedroom", "A0:20:A6:16:A6:34/dht22-top", tempBase + sinPart, 50f);
//                        logger.info("Publish event [" + te + "]. ");
//                        listener.onTemperatureEvent(te);
////                        listener.onTimeEvent(new TimeEvent(ts));
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        break;
//                    }
//                }
//            }
//        }, "test temp publishing");
//        t.start();
    }

    @Override
    public void unsubscribe(IEventListener listener) {
        this.listener = null;
//        t.interrupt();
    }

    private long nextId() {
        return id++;
    }

    @Override
    public void close() throws Exception {
//        t.interrupt();
    }
}
