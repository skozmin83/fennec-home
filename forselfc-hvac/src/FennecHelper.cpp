#ifndef FORSELFC_HVAC_FENNECHELPER_CPP
#define FORSELFC_HVAC_FENNECHELPER_CPP

#include "FennecHelper.h"
#include <Arduino.h>

void pf(const char *format, ...) {
    va_list arg;
    va_start(arg, format);
    char temp[64];
    char* buffer = temp;
    size_t len = vsnprintf(temp, sizeof(temp), format, arg);
    va_end(arg);
    if (len > sizeof(temp) - 1) {
        buffer = new char[len + 1];
        if (!buffer) {
            return;
        }
        va_start(arg, format);
        vsnprintf(buffer, len + 1, format, arg);
        va_end(arg);
    }
    len = Serial.write((const uint8_t*) buffer, len);
    if (buffer != temp) {
        delete[] buffer;
    }
//    va_list args;
//    va_start(args, format);
//    Serial.printf(format, args);
//    va_end(args);
}

void p(const char *string) {
    Serial.print(string);
}

void p(unsigned char b, int base) {
    Serial.print(b, base);
}

void p(char b) {
    Serial.print(b);
}

void pfn(const char *format, ...) {
    va_list args;
    va_start(args, format);
    pf(format, args);
    p("\n");
    va_end(args);
}

void pn(const char *string) {
    p(string);
    p("\n");
}

void pn(unsigned char b, int base) {
    p(b, base);
    p("\n");
}

void pn(char b) {
    p(b);
    p("\n");
}


//#ifdef F // check to see if F() macro is available
//size_t pF(const __FlashStringHelper *ifsh) {
//    return Serial.print(ifsh);
//}
//#endif

#endif //FORSELFC_HVAC_FENNECHELPER_CPP