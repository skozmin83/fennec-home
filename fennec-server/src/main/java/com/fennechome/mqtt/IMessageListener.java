package com.fennechome.mqtt;

import org.bson.Document;

public interface IMessageListener {
    void onMessage(String topicName, long currentTime, Document json);
}
