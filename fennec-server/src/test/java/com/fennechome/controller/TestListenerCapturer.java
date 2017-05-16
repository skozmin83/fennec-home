package com.fennechome.controller;

class TestListenerCapturer implements IFennecControllerEventSource {
    IFennectControllerEventListener listener;

    @Override
    public void subscribe(IFennectControllerEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void unsubscribe(IFennectControllerEventListener listener) {
        this.listener = null;
    }
}
