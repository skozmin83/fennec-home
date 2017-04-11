let websocketsUrl = "ws://localhost:8080";
let fullDataUrl = "/temperature.csv";
let dynamicDataUrl = websocketsUrl + "/temperature.ws";

let device1 = "A0:20:A6:16:A6:34";
let device1Name = "Bedroom";
let fullDevice1Sensor1Url = fullDataUrl + "?sid=dht22-top&topic=" + device1;
let fullDevice1Sensor2Url = fullDataUrl + "?sid=dht22-bottom&topic=" + device1;
let dynamicDevice1Sensor1Url = dynamicDataUrl + "?sid=dht22-top&topic=" + device1;
let dynamicDevice1Sensor2Url = dynamicDataUrl + "?sid=dht22-bottom&topic=" + device1;

let device2 = "A0:20:A6:16:A7:0A";
let device2Name = "Living Room";
let fullDevice2Sensor1Url = fullDataUrl + "?sid=dht22-top&topic=" + device2;
let fullDevice2Sensor2Url = fullDataUrl + "?sid=dht22-bottom&topic=" + device2;
let dynamicDevice2Sensor1Url = dynamicDataUrl + "?sid=dht22-top&topic=" + device2;
let dynamicDevice2Sensor2Url = dynamicDataUrl + "?sid=dht22-bottom&topic=" + device2;

let dg = require("./graph.js");

let graph1 = dg.graph(dg.view(device1Name, "#area1"));
graph1.load(dg.loader({url: fullDevice1Sensor1Url, id: "dht22-top"}));
graph1.load(dg.loader({url: fullDevice1Sensor2Url, id: "dht22-bottom"}));
graph1.subscribe(dg.dynamicLoader(dynamicDevice1Sensor1Url));
graph1.subscribe(dg.dynamicLoader(dynamicDevice1Sensor2Url));

let graph2 = dg.graph(dg.view(device2Name, "#area2"));
graph2.load(dg.loader({url: fullDevice2Sensor1Url, id: "dht22-top"}));
graph2.load(dg.loader({url: fullDevice2Sensor2Url, id: "dht22-bottom"}));
graph2.subscribe(dg.dynamicLoader(dynamicDevice2Sensor1Url));
graph2.subscribe(dg.dynamicLoader(dynamicDevice2Sensor2Url));

var updatesOverTime = [];
var maxNumberOfDataPoints = 20;
function update(updates) {
    // Update the ranges of the chart to reflect the new data
    if (updates.length > 0) {
        xRange.domain(d3.extent(updates, function (d) {
            return d.x;
        }));
        yRange.domain([d3.min(updates, function (d) {
            return d.y;
        }),
            d3.max(updates, function (d) {
                return d.y;
            })]);
    }

    // Until we have filled up our data window, we just keep adding data
    // points to the end of the chart.
    if (updates.length < maxNumberOfDataPoints) {
        line.transition()
            .ease("linear")
            .attr("d", lineFunc(updates));

        svg.selectAll("g.x.axis")
            .transition()
            .ease("linear")
            .call(xAxis);
    }
    // Once we have filled up the window, we then remove points from the
    // start of the chart, and move the data over so the chart looks
    // like it is scrolling forwards in time
    else {
        // Calculate the amount of translation on the x axis which equates
        // to the time between two samples
        var xTranslation = xRange(updates[0].x) - xRange(updates[1].x);

        // Transform our line series immediately, then translate it from
        // right to left. This gives the effect of our chart scrolling
        // forwards in time
        line
            .attr("d", lineFunc(updates))
            .attr("transform", null)
            .transition()
            .duration(samplingTime - 20)
            .ease("linear")
            .attr("transform", "translate(" + xTranslation + ", 0)");

        svg.selectAll("g.x.axis")
            .transition()
            .duration(samplingTime - 20)
            .ease("linear")
            .call(xAxis);
    }

    svg.selectAll("g.y.axis")
        .transition()
        .call(yAxis);
}
// updateStream.onValue(function(value) {
//     updatesOverTime.push({
//         x: new Date(),
//         y:(value - totalUpdatesBeforeLastSample) / (samplingTime / 1000)
//     });
//     if (updatesOverTime.length > maxNumberOfDataPoints)  {
//         updatesOverTime.shift();
//     }
//     totalUpdatesBeforeLastSample = value;
//     update(updatesOverTime);
//     return value;
// });