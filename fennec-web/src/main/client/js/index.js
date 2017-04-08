let device1 = "A0:20:A6:16:A6:34";
let device2 = "A0:20:A6:16:A7:0A";
let dataGraph = require("./graph.js");
let graph1 = dataGraph.graph(dataGraph.loader([
    {url: "/temperature.csv?sid=dht22-bottom&topic=" + device1, id: "dht22-bottom"},
    {url: "/temperature.csv?sid=dht22-top&topic=" + device1, id: "dht22-bottom"},
]), dataGraph.view("#area1"));
let graph2 = dataGraph.graph(dataGraph.loader([
    {url: "/temperature.csv?sid=dht22-bottom&topic=" + device2, id: "dht22-bottom"},
    {url: "/temperature.csv?sid=dht22-top&topic=" + device2, id: "dht22-top"},
]), dataGraph.view("#area2"));


var Bacon = require('baconjs').Bacon;
// Create our websocket to get wiki updates
var ws = new WebSocket("ws://localhost:8080/events/");
ws.onopen = function () {
    console.log("Connection opened");
};

ws.onclose = function () {
    console.log("Connection is closed...");
};
var updateStream = Bacon.fromEventTarget(ws, "message").map(function (event) {
    var dataString = event.data;
    // console.log("Received message: " + dataString);
    graph1.incrementalUpdate(JSON.parse(dataString));
    return JSON.parse(dataString);
});
// Filter the update stream for unspecified events, which we're taking to
// mean edits in this case
var editStream = updateStream.filter(function (update) {
    return update.type === "unspecified";
});
editStream.onValue(function (results) {
    // console.log(JSON.stringify(results));
    graph1.incrementalUpdate(results);
    // update(results);
});

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