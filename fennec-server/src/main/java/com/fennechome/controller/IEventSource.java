package com.fennechome.controller;

public interface IEventSource {
    void subscribe(IEventListener listener);
    void unsubscribe(IEventListener listener);
}
