package com.fennechome.controller;

/**
 * Created by sergey on 5/1/2017.
 */
class TestListenerCapturer implements IEventSource {
    IEventListener listener;

    @Override
    public void subscribe(IEventListener listener) {
        this.listener = listener;
    }
}
