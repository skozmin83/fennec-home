//
// Created by sergey on 3/4/2017.
//

#ifndef FORSELFC_IMESSAGELISTENER_H
#define FORSELFC_IMESSAGELISTENER_H

#include <Arduino.h>

class IMessageListener {
public:
    virtual void onMessage(char* topic, byte* payload, unsigned int length) = 0;
};

#endif //FORSELFC_IMESSAGELISTENER_H
