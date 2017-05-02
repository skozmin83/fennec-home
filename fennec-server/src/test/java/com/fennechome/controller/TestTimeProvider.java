package com.fennechome.controller;

/**
 * Created by sergey on 5/1/2017.
 */
class TestTimeProvider implements ITimeProvider {
    long time;

    @Override
    public long currentTime() {
        return time;
    }
}
