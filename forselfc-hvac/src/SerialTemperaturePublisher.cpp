//
// Created by sergey on 3/4/2017.
//
#ifndef SERIAL_TEMPERATURE_PUBLISHER
#define SERIAL_TEMPERATURE_PUBLISHER

#include "ITemperaturePublisher.h"
#include <Arduino.h>

class SerialTemperaturePublisher : public ITemperaturePublisher {
public:
    SerialTemperaturePublisher() {
        Serial.begin(115200);
    }
    boolean publish(char *baseTopic, char *controllerId, char *sensorId, float humidity, float temperature, float voltage,
                    uint32_t measureId) {
        if (!(isnan(humidity) || isnan(temperature))) {
            Serial.print(": controller: ");
            Serial.print(controllerId);
            Serial.print(": sensor: ");
            Serial.print(sensorId);
            Serial.print(": Humidity: ");
            Serial.print(humidity);
            Serial.print(" %\t");
            Serial.print("Temperature: ");
            Serial.print(temperature);
            Serial.println(" C");
            Serial.print(" %\t");
            Serial.print("Voltage: ");
            Serial.print(voltage);
            Serial.println(" volts");
        } else {
            Serial.println("Failed to read from DHT bottomSensor!");
        }
        return (boolean) true;
    }

private:
};

#endif

//
//Found a solution to my problem: #define NEW(x,y) *(x=(y*)malloc(sizeof(y)))=y
//        Kind of recreates the standard new operator. First is uses malloc to allocate enough space to fit the class, casts it to the correct pointer type and assigns it to the variable. The value (object) then has it's constructor run to initialise the member fields
//
//Usage: On a separate line NEW(destinationVariable,Class)(constructorParamList);
//
//Sample
//        Code: [Select]
//#define NEW(x,y) *(x=(y*)malloc(sizeof(y)))=y
//
//class Bar {
//private :
//    int counter;
//public :
//    Bar() : counter(10) {}
//
//    int get() {return counter++>>1;}
//};
//
//class Foo {
//private :
//    int counter;
//    Bar bar;
//public :
//    Foo(int x) {counter=x;}
//    int get() {return bar.get()+counter++;}
//};
//
//Foo* foo;
//
//void setup() {
//    Serial.begin(9600);
//    NEW(foo,Foo)(10);
//    Serial.print("foo's address: ");
//    Serial.println((int)foo);
//}
//
//void loop() {
//    Serial.println((foo->get()));
//    delay(500);
//}
//
//
//My output:
//foo's address: 456
//15
//16
//18
//19
//21
//22
//24
//25
//27
//28
//30
//31
//...
//
//Notes:
//        The define statements leaves room at the end for any constructor parameters, so the () are required.
//The return type of NEW(x,y)() is of type y, not *y.
//The y() constructor also initialises direct field objects (in the example bar).
//To assign to more than one variable do something like NEW(a=b=c,Foo)(10).