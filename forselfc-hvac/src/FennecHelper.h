//
// Created by sergey on 6/21/2017.
//

#ifndef FORSELFC_HVAC_FENNECHELPER_H
#define FORSELFC_HVAC_FENNECHELPER_H

#include <Arduino.h>

void pf(const char * format, ...)  __attribute__ ((format (pf, 2, 3)));
//void pf(const char *format, ...);
void p(const char *string);
void p(unsigned char b, int base);
void p(char b);

void pfn(const char * format, ...)  __attribute__ ((format (pf, 2, 3)));
//void pfn(const char *format, ...);
void pn(const char *string);
void pn(unsigned char b, int base);
void pn(char b);

//#ifdef F // check to see if F() macro is available
//size_t pF(const __FlashStringHelper *ifsh);
//#endif

#endif //FORSELFC_HVAC_FENNECHELPER_H
