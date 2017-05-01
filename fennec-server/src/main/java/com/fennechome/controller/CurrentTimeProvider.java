package com.fennechome.controller;

/**
 * Created by sergey on 5/1/2017.
 */
public class CurrentTimeProvider implements ITimeProvider {
    @Override
    public long currentTime() {
        return System.currentTimeMillis();
    }
}
