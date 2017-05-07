package com.fennechome.controller;

class TestListenerCapturer implements IEventSource {
    IEventListener listener;

    @Override
    public void subscribe(IEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void unsubscribe(IEventListener listener) {
        this.listener = null;
    }
}
