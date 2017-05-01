package com.fennechome.controller;

import com.google.common.base.Preconditions;
import gnu.trove.list.linked.TFloatLinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Simple controller that keeps temperature in each zone within limits with the given hysteresis
 */
public class SimpleBoundariesController implements IComfortController, IEventListener {
    public static final float DEFAULT_TEMP_HYSTERESIS = .5f;
    public static final float DEFAULT_HYSTERESIS = .5f;
    public static final int MIN_TEMP_TIME_UPDATE_MS = 10000;
    public static final int MIN_TEMP_BAND = 1;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, SingleZoneController> zones = new HashMap<>();
    private final IDirectionExecutor executor;
    private final int minTempDistributionSize;
    private final int maxTempDistributionSize;
    private final ITimeProvider timer;
    private Direction lastDirection = new Direction();
    private Direction direction = new Direction();

    public SimpleBoundariesController(IEventSource source, IDirectionExecutor executor, int minTempDistributionSize, int maxTempDistributionSize, ITimeProvider timer) {
        this.executor = executor;
        this.minTempDistributionSize = minTempDistributionSize;
        this.maxTempDistributionSize = maxTempDistributionSize;
        this.timer = timer;
        source.subscribe(this);
    }

    public SimpleBoundariesController(IEventSource source, IDirectionExecutor executor, int minTempDistributionSize, int maxTempDistributionSize) {
        this(source, executor, minTempDistributionSize, maxTempDistributionSize, new CurrentTimeProvider());
    }

    @Override
    public void onTemperatureEvent(TemperatureEvent event) {
        SingleZoneController zoneController = zones.get(event.zoneId);
        if (zoneController != null) {
            zoneController.onTemperatureEvent(event);
        } else {
            logger.error("Unable to find zone for [" + event + "]. ");
        }
        publishIfStateChanged();
    }

    @Override
    public void onZonePreferencesEvent(ZonePreferencesEvent event) {
        SingleZoneController zoneController = zones.get(event.zoneId);
        if (zoneController != null) {
            zoneController.updatePreferences(event);
        } else {
            logger.error("Unable to find zone for [" + event + "]. ");
        }
        publishIfStateChanged();
    }

    @Override
    public void onZoneChangeEvent(ZoneEvent event) {
        SingleZoneController zoneController = zones.computeIfAbsent(event.zoneId,
                k -> new SingleZoneController(event.zoneId, minTempDistributionSize, maxTempDistributionSize, timer));
        zoneController.setDevices(event.devices);
        publishIfStateChanged();
    }

    @Override
    public void onTimeEvent(TimeEvent event) {
        zones.forEach((s, zoneController) -> zoneController.onTimeEvent(event));
        publishIfStateChanged();
    }

    private void publishIfStateChanged() {
        // todo optimize, shouldn't remove all hoses
        direction.hoseStates.clear();
        ZoneComfortState targetState = ZoneComfortState.OK;
        for (SingleZoneController zone : zones.values()) {
            if (zone.state == ZoneComfortState.OK) {
                // if zone is ok, we need to close shutters on all devices
                changeShutterState(zone, HoseState.SHUT);
            } else {
                // if zone is out of comfort, we need to open shutters to modify it
                changeShutterState(zone, HoseState.OPEN);
                if (targetState == ZoneComfortState.OK) {
                    targetState = zone.state;
                } else {
                    if (targetState != zone.state) {
                        // in case one zone target state conflicts with the other just ignore. todo need to inform the user
                        logger.error("Zone [" + zone.zoneId + "] is in conflict with the other target states. ");
                        return;
                    }
                }
            }
        }
        direction.thermostatState = convert(targetState);

        // compare if state changed since last time
        if (!lastDirection.equals(direction)) {
            direction.id = lastDirection.id + 1;
            executor.send(direction);
            // swap
            Direction tmp = lastDirection;
            lastDirection = direction;
            direction = tmp;
        }
    }

    private void changeShutterState(SingleZoneController nextZone, HoseState newState) {
        for (Device device : nextZone.devices) {
            if (device.getType() == DeviceType.HOSE) {
                direction.hoseStates.put(device.getId(), newState);
            }
        }
    }

    private ThermostatState convert(ZoneComfortState targetState) {
        ThermostatState thermostatState = ThermostatState.OFF;
        switch (targetState) {
            case TOO_COLD:
                thermostatState = ThermostatState.HEAT;
                break;
            case TOO_WARM:
                thermostatState = ThermostatState.COOL;
                break;
        }
        return thermostatState;
    }

    enum ZoneComfortState {
        TOO_COLD, TOO_WARM, OK
    }

    static class SingleZoneController {
        private final ZoneTemperatureTrend temperature;
        private final Set<Device> devices = new HashSet<>();
        private final String zoneId;
        private final ITimeProvider timer;
        private float tempMax;
        private float tempMin;
        private ZoneComfortState state = ZoneComfortState.OK;

        public SingleZoneController(String zoneId, int minTempDistributionSize, int maxTempDistributionSize, ITimeProvider timer) {
            this.timer = timer;
            Preconditions.checkNotNull(zoneId);
            this.zoneId = zoneId;
            temperature = new ZoneTemperatureTrend(minTempDistributionSize, maxTempDistributionSize);
        }

        public void onTemperatureEvent(TemperatureEvent event) {
            temperature.temperatureUpdate(event);
            evaluate();
        }

        public void updatePreferences(ZonePreferencesEvent event) {
            Preconditions.checkArgument(event.tempMax - event.tempMin > MIN_TEMP_BAND,
                    "Max temp [" + event.tempMax + "] is too close to [" + event.tempMin + "]");
            this.tempMax = event.tempMax;
            this.tempMin = event.tempMin;

            evaluate();
        }

        public void onTimeEvent(TimeEvent event) {
            evaluate();
        }

        private void evaluate() {
            if (!temperature.isValid() ||
                    timer.currentTime() - temperature.getLastTempUpdateTime() > MIN_TEMP_TIME_UPDATE_MS) {
                // if last update came too long time ago we consider that either:
                // * sensor went down
                // * sensor deliberately disabled
                // so we revert to regular strategy: open all shutters in that area
                state = ZoneComfortState.OK;
            } else {
                float avgCurTemp = temperature.getAvgTmp(5);
                if (avgCurTemp > tempMax) {
                    state = ZoneComfortState.TOO_WARM;
                } else if (avgCurTemp < tempMin) {
                    state = ZoneComfortState.TOO_COLD;
                } else {
                    state = ZoneComfortState.OK;
                }
            }
        }

        public void setDevices(Set<Device> devices) {
            this.devices.clear();
            this.devices.addAll(devices);
        }
    }

    static class ZoneTemperatureTrend {
        private final TFloatLinkedList temperatures = new TFloatLinkedList();
        private final int minTempDistributionSize;
        private final int maxTempDistributionSize;
        private long lastTempUpdateTime;

        public ZoneTemperatureTrend(int minTempDistributionSize, int maxTempDistributionSize) {
            this.minTempDistributionSize = minTempDistributionSize;
            this.maxTempDistributionSize = maxTempDistributionSize;
        }

        public void temperatureUpdate(TemperatureEvent event) {
            lastTempUpdateTime = event.timeMillis;
            if (temperatures.size() > maxTempDistributionSize) {
                temperatures.removeAt(temperatures.size());
            }
            temperatures.add(event.temperature);
        }

        public long getLastTempUpdateTime() {
            return lastTempUpdateTime;
        }

        public float getAvgTmp(int lastMeasurements) {
            float ret = 0;
            int avtSize = Math.min(lastMeasurements, temperatures.size());
            for (int i = 0; i < avtSize; i++) {
                ret += temperatures.get(i);
            }
            return ret / avtSize;
        }

        public boolean isValid() {
            return temperatures.size() >= minTempDistributionSize;
        }
    }
}
