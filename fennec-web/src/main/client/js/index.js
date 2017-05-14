// todo move it all to mongo config table
// todo make it configurable

let websocketsUrl = "ws://raspberrypi:8080";
let fullDataUrl = "/temperature.csv";
let dynamicSensorDataUrl = websocketsUrl + "/temperature.ws";
let dynamicZoneDataUrl = websocketsUrl + "/zone.ws";

let thermostat1 = "5C:CF:7F:34:37:E0";
let dynamicThermostat1Url = dynamicZoneDataUrl + "?thermostat=" + thermostat1;

let device1 = "A0:20:A6:16:A6:34";
let device1Name = "Bedroom";
let fullDevice1Sensor1Url = fullDataUrl + "?sid=dht22-top&topic=" + device1;
let fullDevice1Sensor2Url = fullDataUrl + "?sid=dht22-bottom&topic=" + device1;
let dynamicDevice1Sensor1Url = dynamicSensorDataUrl + "?sid=dht22-top&topic=" + device1;
let dynamicDevice1Sensor2Url = dynamicSensorDataUrl + "?sid=dht22-bottom&topic=" + device1;

let device2 = "A0:20:A6:16:A7:0A";
let device2Name = "Living Room";
let fullDevice2Sensor1Url = fullDataUrl + "?sid=dht22-top&topic=" + device2;
let fullDevice2Sensor2Url = fullDataUrl + "?sid=dht22-bottom&topic=" + device2;
let dynamicDevice2Sensor1Url = dynamicSensorDataUrl + "?sid=dht22-top&topic=" + device2;
let dynamicDevice2Sensor2Url = dynamicSensorDataUrl + "?sid=dht22-bottom&topic=" + device2;

let dg = require("./graph.js");

let graph1 = dg.graph(dg.view(device1Name, "#area1"));
graph1.load(dg.loader({url: fullDevice1Sensor1Url, id: "dht22-top"}));
graph1.load(dg.loader({url: fullDevice1Sensor2Url, id: "dht22-bottom"}));
graph1.subscribeToDynamicZoneData(dg.dynamicLoader(dynamicThermostat1Url));
graph1.subscribeToDynamicSensorData(dg.dynamicLoader(dynamicDevice1Sensor1Url));
graph1.subscribeToDynamicSensorData(dg.dynamicLoader(dynamicDevice1Sensor2Url));

let graph2 = dg.graph(dg.view(device2Name, "#area2"));
graph2.load(dg.loader({url: fullDevice2Sensor1Url, id: "dht22-top"}));
graph2.load(dg.loader({url: fullDevice2Sensor2Url, id: "dht22-bottom"}));
graph2.subscribeToDynamicZoneData(dg.dynamicLoader(dynamicThermostat1Url));
graph2.subscribeToDynamicSensorData(dg.dynamicLoader(dynamicDevice2Sensor1Url));
graph2.subscribeToDynamicSensorData(dg.dynamicLoader(dynamicDevice2Sensor2Url));
