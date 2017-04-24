//
// Created by sergey on 3/4/2017.
//

#ifndef FORSELFC_LOGMESSAGELISTENER
#define FORSELFC_LOGMESSAGELISTENER

#include "IMessageListener.h"

class LogMessageListener: public IMessageListener {
public:
    void onMessage(char* topic, byte* payload, unsigned int length) {
        digitalWrite(BUILTIN_LED, LOW);

        Serial.print("Message arrived [");
        Serial.print(topic);
        Serial.print("] ");
        for (int i = 0; i < length; i++) {
            Serial.print((char) payload[i]);
        }
        Serial.println();
        delay(50);
        digitalWrite(BUILTIN_LED, HIGH);
    }
};

#endif
