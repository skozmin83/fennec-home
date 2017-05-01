package com.fennechome.controller;


import com.google.common.base.Preconditions;

/**
 * Created by sergey on 4/29/2017.
 */
public class Device {
    private final String id;
    private final DeviceType type;

    public Device(String id, DeviceType type) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(type);
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public DeviceType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Device device = (Device) o;

        return id.equals(device.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", type=" + type +
                '}';
    }
}
