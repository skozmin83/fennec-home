//
// Created by sergey on 3/4/2017.
//

#ifndef FORSELFC_IMESSAGELISTENER_H
#define FORSELFC_IMESSAGELISTENER_H

#include <Arduino.h>
#include <stdint.h>

class IMessageListener {
public:
    virtual void onMessage(char *topic, uint8_t *payload, unsigned int length) = 0;
};

#endif //FORSELFC_IMESSAGELISTENER_H
