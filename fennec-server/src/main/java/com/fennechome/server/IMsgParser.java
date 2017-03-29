package com.fennechome.server;

import io.moquette.interception.messages.InterceptPublishMessage;

public interface IMsgParser {
    DeviceInfo parse(InterceptPublishMessage msg);
}
