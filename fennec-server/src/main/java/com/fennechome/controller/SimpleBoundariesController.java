package com.fennechome.controller;

import com.fennechome.common.FennecException;
import com.google.common.base.Preconditions;
import gnu.trove.list.linked.TFloatLinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Simple controller that keeps temperature in each zone within limits with the given hysteresis
 */
public class SimpleBoundariesController implements IComfortController, IEventListener {
    public static final int MIN_TEMP_TIME_UPDATE_MS = 10000;
    public static final int FASTEST_DIRECTION_TIME_SWITCH_MS = 1000 * 60 * 10;
    public static final int MIN_TEMP_BAND = 1;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final OppositeDirectionMonitor oppositeDirectionMonitor = new OppositeDirectionMonitor(FASTEST_DIRECTION_TIME_SWITCH_MS);
    private final Map<String, SingleZoneController> zones = new HashMap<>();
    private final IEventSource source;
    private final IDirectionExecutor executor;
    private final int minTempDistributionSize;
    private final int maxTempDistributionSize;
    private final ITimeProvider timer;
    private long eventIdWatermark = -1;
    private Direction lastDirection = new Direction();

    // todo add same zone sensor differences analysis:
    // if temp between two sensors is far apart at time 0 do:
    // * start FAN with hoses in SWING
    // * monitor gradient of temps difference
    // * as soon as it's close to 0 - turn it off and save that configuration (gradient profile with staleness timestamp) to zone profile
    // each time temp diff reaches certain number we need to check that profile and see if FAN would help to mixup the air

    public SimpleBoundariesController(IEventSource source, IDirectionExecutor executor, int minTempDistributionSize, int maxTempDistributionSize, ITimeProvider timer) {
        this.source = source;
        this.executor = executor;
        this.minTempDistributionSize = minTempDistributionSize;
        this.maxTempDistributionSize = maxTempDistributionSize;
        this.timer = timer;
    }

    public SimpleBoundariesController(IEventSource source, IDirectionExecutor executor, int minTempDistributionSize, int maxTempDistributionSize) {
        this(source, executor, minTempDistributionSize, maxTempDistributionSize, new CurrentTimeProvider());
    }

    @Override
    public void onTemperatureEvent(TemperatureEvent event) {
        if (checkWatermark(event)) return;
        SingleZoneController zoneController = zones.get(event.zoneId);
        if (zoneController != null) {
            zoneController.onTemperatureEvent(event);
        } else {
            logger.error("Unable to find zone for [" + event + "]. ");
        }
        publishIfStateChanged(event.timeMillis);
    }

    @Override
    public void onZonePreferencesEvent(ZonePreferencesEvent event) {
        if (checkWatermark(event)) return;
        SingleZoneController zoneController = zones.get(event.zoneId);
        if (zoneController != null) {
            zoneController.updatePreferences(event);
        } else {
            logger.error("Unable to find zone for [" + event + "]. ");
        }
        publishIfStateChanged(event.timeMillis);
    }

    @Override
    public void onTimeEvent(TimeEvent event) {
        zones.forEach((s, zoneController) -> zoneController.onTimeEvent(event));
        publishIfStateChanged(event.timeMillis);
    }

    @Override
    public void onZoneChangeEvent(ZoneEvent event) {
        if (checkWatermark(event)) return;
        SingleZoneController zoneController = zones.computeIfAbsent(event.zoneId,
                k -> new SingleZoneController(event.zoneId, minTempDistributionSize, maxTempDistributionSize, timer));
        zoneController.setDevices(event.devices);
        publishIfStateChanged(event.timeMillis);
    }

    private boolean checkWatermark(FennecEvent event) {
        if (event.eventId <= eventIdWatermark) {
            return true;
        } else {
            eventIdWatermark = event.eventId;
        }
        return false;
    }

    private void publishIfStateChanged(long timeMillis) {
        // todo pool
        Direction direction = new Direction();
        ZoneComfortState targetState = ZoneComfortState.OK;
        for (SingleZoneController zone : zones.values()) {
            switch (zone.state) {
                case OK: {
                    // if zone is ok, we need to close shutters on all devices
                    changeShutterState(zone, direction, HoseState.SHUT);
                    break;
                }
                case TOO_COLD:
                case TOO_WARM: {
                    // if zone is out of comfort, we need to open shutters to modify it
                    changeShutterState(zone, direction, HoseState.OPEN);
                    if (targetState == ZoneComfortState.OK) {
                        targetState = zone.state;
                    } else {
                        if (targetState != zone.state) {
                            // in case one zone target state conflicts with the other just ignore. todo need to inform the user
                            logger.error("Zone [" + zone.zoneId + "] is in conflict with the other target states. ");
                            return;
                        }
                    }
                    break;
                }
            }
        }
        direction.thermostatState = convert(targetState);

        // compare if state changed since last time
        if (!lastDirection.equals(direction)) {
            if (oppositeDirectionMonitor.wantToSwitch(direction.thermostatState, timeMillis)) {
                direction.id = lastDirection.id + 1;
                executor.send(direction);
                lastDirection = direction;
            } else {
                logger.warn("Blocking event as potential heat/cool swing event [" + direction + "]");
            }
        }
    }

    private void changeShutterState(SingleZoneController zoneController, Direction direction, HoseState newState) {
        for (Device device : zoneController.devices) {
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

    @Override
    public void start() {
        source.subscribe(this);
    }

    @Override
    public void close() {
        source.unsubscribe(this);
    }

    enum ZoneComfortState {
        TOO_COLD, TOO_WARM, OK, DETECTING
    }

    static class SingleZoneController {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final ZoneTemperatureTrend temperature;
        private final Set<Device> devices = new HashSet<>();
        private final String zoneId;
        private final ITimeProvider timer;
        private float tempMax;
        private float tempMin;
        private ZoneComfortState state = ZoneComfortState.DETECTING;

        public SingleZoneController(String zoneId, int minTempDistributionSize, int maxTempDistributionSize, ITimeProvider timer) {
            this.timer = timer;
            Preconditions.checkNotNull(zoneId);
            this.zoneId = zoneId;
            temperature = new ZoneTemperatureTrend(minTempDistributionSize, maxTempDistributionSize);
        }

        public void onTemperatureEvent(TemperatureEvent event) {
            temperature.temperatureUpdate(event);
            switchState(event);
        }

        public void updatePreferences(ZonePreferencesEvent event) {
            Preconditions.checkArgument(event.tempMax - event.tempMin > MIN_TEMP_BAND,
                    "Max temp [" + event.tempMax + "] is too close to [" + event.tempMin + "]");
            this.tempMax = event.tempMax;
            this.tempMin = event.tempMin;
            switchState(event);
        }

        public void onTimeEvent(TimeEvent event) {
            switchState(event);
        }

        private void switchState(Object event) {
            ZoneComfortState newState = calculateNewState();
            if (newState != state) {
                logger.info("Switch state from [" + state + "] to [" + newState + "] because of the event [" + event + "]");
                state = newState;
            }
        }

        private ZoneComfortState calculateNewState() {
            ZoneComfortState ret;
            if (!temperature.isNotEnoughData()) {
                // if last update came too long time ago we consider that either:
                // * sensor went down
                // * sensor deliberately disabled
                // so we revert to regular strategy: open all shutters in that area
                ret = ZoneComfortState.DETECTING;
//            } else if (timer.currentTime() - temperature.getLastTempUpdateTime() > MIN_TEMP_TIME_UPDATE_MS) {
//                state = ZoneComfortState.OK;
            } else {
                float avgCurTemp = temperature.getAvgTmp(5);
                if (avgCurTemp > tempMax) {
                    ret = ZoneComfortState.TOO_WARM;
                } else if (avgCurTemp < tempMin) {
                    ret = ZoneComfortState.TOO_COLD;
                } else {
                    ret = ZoneComfortState.OK;
                }
            }
            return ret;
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
                temperatures.removeAt(0); // remove oldest
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
                ret += temperatures.get(temperatures.size() - 1 - i);
            }
            return ret / avtSize;
        }

        public boolean isNotEnoughData() {
            return temperatures.size() >= minTempDistributionSize;
        }
    }

    static class OppositeDirectionMonitor {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final long fastestTimeSwitchMs;
        private ThermostatState lastDirection;
        private long lastDirectionMs;

        OppositeDirectionMonitor(long fastestDirectionTimeSwitchMs) {
            this.fastestTimeSwitchMs = fastestDirectionTimeSwitchMs;
        }

        /**
         * @param newState
         * @return true if should go ahead, false if switching direction banned
         */
        public boolean wantToSwitch(ThermostatState newState, long eventTime) {
            boolean propagateEvent = false;
            boolean lastOppositeDirectionHappenedTooLongAgo = (eventTime - lastDirectionMs) > fastestTimeSwitchMs;
            if (lastDirection == null || lastOppositeDirectionHappenedTooLongAgo) {
                switchStateTo(newState, eventTime);
                propagateEvent = true;
            } else {
                switch (newState) {
                    case COOL:
                    case COOL_LEVEL_2: {
                        if (lastDirection != ThermostatState.HEAT
                                && lastDirection != ThermostatState.HEAT_LEVEL_2
                                && lastDirection != ThermostatState.EMERGENCY_HEAT) {
                            switchStateTo(newState, eventTime);
                            propagateEvent = true;
                        } else {
                            logger.warn("Last switch to HEATING happened on [" + new Date(lastDirectionMs) +
                                    "] or [" + (eventTime - lastDirectionMs) / 1000 +
                                    "] seconds ago. Want to switch to COOLING now. Something is wrong. " +
                                    "Minimum switching time is [" + fastestTimeSwitchMs / 1000 + "] seconds");
                        }
                        break;
                    }
                    case HEAT:
                    case HEAT_LEVEL_2: {
                        if (lastDirection != ThermostatState.COOL
                                && lastDirection != ThermostatState.COOL_LEVEL_2) {
                            switchStateTo(newState, eventTime);
                            propagateEvent = true;
                        } else {
                            logger.warn("Last switch to COOLING happened on [" + new Date(lastDirectionMs) +
                                    "] or [" + (eventTime - lastDirectionMs) / 1000 +
                                    "] seconds ago. Want to switch to HEATING now. Something is wrong. " +
                                    "Minimum switching time is [" + fastestTimeSwitchMs / 1000 + "] seconds");
                        }
                        break;
                    }
                    case FAN:
                    case OFF: {
                        propagateEvent = true;
                        break;
                    }
                    default:
                        throw new FennecException("Unable to handle unknown status [" + newState + "]");
                }
            }
            return propagateEvent;
        }

        private void switchStateTo(ThermostatState newState, long eventTime) {
            // only switch if not neutral status
            if (newState != ThermostatState.FAN && newState != ThermostatState.OFF) {
                lastDirection = newState;
                lastDirectionMs = eventTime;
            }
        }
    }
}
