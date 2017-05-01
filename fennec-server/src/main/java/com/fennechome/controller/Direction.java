package com.fennechome.controller;

import java.util.HashMap;
import java.util.Map;

public class Direction {
    long id;
    ThermostatState thermostatState;
    Map<String, HoseState> hoseStates = new HashMap<>();

    public Direction() {
    }

    public Direction(long id, ThermostatState thermostatState, Map<String, HoseState> hoseStates) {
        this.id = id;
        this.thermostatState = thermostatState;
        this.hoseStates = hoseStates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Direction direction = (Direction) o;

        if (thermostatState != direction.thermostatState) return false;
        return hoseStates.equals(direction.hoseStates);
    }

    @Override
    public int hashCode() {
        int result = thermostatState.hashCode();
        result = 31 * result + hoseStates.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Direction{" +
                "id=" + id +
                ", thermostatState=" + thermostatState +
                ", hoseStates=" + hoseStates +
                '}';
    }
}
