let dataGraph = require("./graph.js");
dataGraph.graph(dataGraph.loader([
    "/temperature.csv?sid=dht22-bottom&topic=A0:20:A6:16:A6:34",
    "/temperature.csv?sid=dht22-top&topic=A0:20:A6:16:A6:34"]
), dataGraph.view("#area1"));
dataGraph.graph(dataGraph.loader([
    "/temperature.csv?sid=dht22-bottom&topic=A0:20:A6:16:A7:0A",
    "/temperature.csv?sid=dht22-top&topic=A0:20:A6:16:A7:0A"]
), dataGraph.view("#area2"));