// Load jQuery with the simulated jsdom window.
// var npmcss = require('npm-css');
// var css = npmcss('./css/index.css');
jQuery = require('jquery');
Tether = require('tether');
bootstrap = require('bootstrap');
d3 = require("d3");
// set the dimensions and margins of the graph
var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 960 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom;
const bisectDate = d3.bisector(function (d) {
    return d.ts;
}).left;

"use strict";
// parse the date / time
// 2017-03-25T06:07:34.780Z
// d3.time.format("%Y-%m-%d")	1986-01-28
// var parseTime = d3.timeParse("%Y-%m-%dT%XZ");
var parseTime = d3.timeParse("%Y-%m-%d %H:%M:%S");
var formatDate = d3.timeParse("%Y-%m-%d %H:%M:%S");
//    var parseTime = d3.timeParse("%Y-%m-%dT%H:%M:%S.%LZ");
//    var parseTime = d3.timeParse("%Y-%m-%d %H:%M:%S");
//var parseTime = d3.timeParse("%d-%b-%y");

// set the ranges
var x = d3.scaleTime().range([0, width]);
var y = d3.scaleLinear().range([height, 0]);
var z = d3.scaleOrdinal(d3.schemeCategory10);

// append the svg obgect to the body of the page
// appends a 'group' element to 'svg'
// moves the 'group' element to the top left margin
var svg = d3.select("body")
    .append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom);
var g = svg.append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

function DataHolder(seriesCallBack, id) {
    this.id = id;
    this.fetchData = (function (error, data) {
        if (error) throw error;
        this.values = data;
        seriesCallBack(data, this.id);
    }).bind(this);
}
function DataCollector(urls, allFetchedCallback) {
    this.countDownSize = urls.length;
    this.allFetchedCallback = allFetchedCallback;
    this.seriesDataArray = [];
    this.seriesCallBack = (function (data, id) {
        "use strict";
        console.log("Loaded: " + id + ", size: " + data.length);
        if (--this.countDownSize === 0) {
            this.allFetchedCallback(this.seriesDataArray);
        }
    }).bind(this);
    for (let i = 0; i < urls.length; i++) {
        let url = urls[i];
        this.seriesDataArray[i] = new DataHolder(this.seriesCallBack, url);
        d3.csv(url, this.seriesDataArray[i].fetchData);
    }
}
let dataCollector = new DataCollector([
        "/temperature.csv?sid=dht22-bottom&topic=A0:20:A6:16:A6:34",
        "/temperature.csv?sid=dht22-top&topic=A0:20:A6:16:A6:34"],
    function (seriesDataArray) {
        // format the data
        seriesDataArray.forEach(function (data) {
                data.values.forEach(function (d) {
                    d.ts = parseTime(d.ts);
                    d.t = +d.t;
                })
            }
        );
        x.domain([
            d3.min(seriesDataArray, function (c) { return d3.min(c.values, function (d) { return d.ts; }); }),
            d3.max(seriesDataArray, function (c) { return d3.max(c.values, function (d) { return d.ts; }); })
        ]);
        y.domain([
            d3.min(seriesDataArray, function (c) { return d3.min(c.values, function (d) { return d.t; }); }),
            d3.max(seriesDataArray, function (c) { return d3.max(c.values, function (d) { return d.t; }); })
        ]);
        z.domain(seriesDataArray.map(function (c) { return c.id; }));

        // define the line
        let line = d3.line()
            .curve(d3.curveMonotoneX)
            .x(function (d) {
                return x(d.ts);
            })
            .y(function (d) {
                return y(d.t);
            });

        g.append("g")
            .attr("class", "axis axis--x")
            .attr("transform", "translate(0," + height + ")")
            .call(d3.axisBottom(x));

        g.append("g")
            .attr("class", "axis axis--y")
            .call(d3.axisLeft(y))
            .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", "0.71em")
            .attr("fill", "#000")
            .text("Temperature, ºC");

        let seriesId = g.selectAll(".series")
            .data(seriesDataArray)
            .enter()
            .append("g")
            .attr("class", "series");
        seriesId.append("path")
            .attr("class", "line")
            .attr("d", function (d) {
                return line(d.values);
            })
            .style("stroke", function (d) {
                return z(d.id);
            });
        //
        // seriesId.append("text")
        //     .datum(function (d) {
        //         return {id: d.id, value: d.values[d.values.length - 1]};
        //     })
        //     .attr("transform", function (d) {
        //         return "translate(" + x(d.ts) + "," + y(d.t) + ")";
        //     })
        //     .attr("x", 3)
        //     .attr("dy", "0.35em")
        //     .style("font", "10px sans-serif")
        //     .text(function (d) { return d.id; });

        // Scale the range of the data
        // x.domain(d3.extent(data, function (d) {
        //     return d.ts;
        // }));
//        var yExtent = d3.extent(data, function (d) {
//            return d.t;
//        });
//        y.domain(yExtent);
//         y.domain([15, 23]);
//        x.domain(d3.extent(data, function(d) { return d.ts; }));
//        y.domain([0, d3.max(data, function(d) { return d.t; })]);
//         svg.datum(data);
//         // Add the valueline path.
//         svg.append("path")
//             .attr("class", "top-line")
//             .attr("d", valueline)
//             .attr('clip-path', 'url(#rect-clip)');

        let focus = svg.append("g").style("display", "none");

        // Add the X Axis
        // svg.append("g")
        //     .attr("transform", "translate(0," + height + ")")
        //     .call(d3.axisBottom(x));

        // Add the Y Axis
        // svg.append("g")
        //     .call(d3.axisLeft(y));


        // append the x line
        focus.append("line")
            .attr("class", "x")
            .style("stroke", "blue")
            .style("stroke-dasharray", "3,3")
            .style("opacity", 0.5)
            .attr("y1", 0)
            .attr("y2", height);

        // append the y line
        focus.append("line")
            .attr("class", "y")
            .style("stroke", "blue")
            .style("stroke-dasharray", "3,3")
            .style("opacity", 0.5)
            .attr("x1", width)
            .attr("x2", width);

        // append the circle at the intersection
        focus.append("circle")
            .attr("class", "y")
            .style("fill", "none")
            .style("stroke", "blue")
            .attr("r", 4);

        // place the value at the intersection
        focus.append("text")
            .attr("class", "y1")
            .style("stroke", "white")
            .style("stroke-width", "3.5px")
            .style("opacity", 0.8)
            .attr("dx", 8)
            .attr("dy", "-.3em");
        focus.append("text")
            .attr("class", "y2")
            .attr("dx", 8)
            .attr("dy", "-.3em");

        // place the date at the intersection
        focus.append("text")
            .attr("class", "y3")
            .style("stroke", "white")
            .style("stroke-width", "3.5px")
            .style("opacity", 0.8)
            .attr("dx", 8)
            .attr("dy", "1em");
        focus.append("text")
            .attr("class", "y4")
            .attr("dx", 8)
            .attr("dy", "1em");

        // append the rectangle to capture mouse
        svg.append("rect")
            .attr("width", width)
            .attr("height", height)
            .style("fill", "none")
            .style("pointer-events", "all")
            .on("mouseover", function () {
                focus.style("display", null);
            })
            .on("mouseout", function () {
                focus.style("display", "none");
            })
            .on("mousemove", mousemove);

        function mousemove() {
            // var x0 = x.invert(d3.mouse(this)[0]),
            //     i = bisectDate(data, x0, 1),
            //     d0 = data[i - 1],
            //     d1 = data[i],
            //     d = x0 - d0.ts > d1.ts - x0 ? d1 : d0;
            //
            // focus.select("circle.y")
            //     .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")");
            //
            // focus.select("text.y1")
            //     .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
            //     .text(d.t);
            //
            // focus.select("text.y2")
            //     .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
            //     .text(d.t);
            //
            // focus.select("text.y3")
            //     .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
            //     .text(formatDate(d.ts));
            //
            // focus.select("text.y4")
            //     .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
            //     .text(formatDate(d.ts));
            //
            // focus.select(".x")
            //     .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
            //     .attr("y2", height - y(d.t));
            //
            // focus.select(".y")
            //     .attr("transform", "translate(" + width * -1 + "," + y(d.t) + ")")
            //     .attr("x2", width + width);
        }
    }
);
//
// d3.csv("/temperature.csv?sid=dht22-bottom&topic=A0:20:A6:16:A6:34", function (error, data) {
//     if (error) throw error;
//
//     // format the data
//     data.forEach(function (d) {
//         d.ts = parseTime(d.ts);
//         d.t = +d.t;
//     });
//
//     // Scale the range of the data
//     x.domain(d3.extent(data, function (d) {
//         return d.ts;
//     }));
// //        var yExtent = d3.extent(data, function (d) {
// //            return d.t;
// //        });
// //        y.domain(yExtent);
//     y.domain([15, 23]);
// //        x.domain(d3.extent(data, function(d) { return d.ts; }));
// //        y.domain([0, d3.max(data, function(d) { return d.t; })]);
//     svg.datum(data);
//     // Add the valueline path.
//     svg.append("path")
//     //            .data([data])
//         .attr("class", "top-line")
//         .attr("d", valueline)
//         .attr('clip-path', 'url(#rect-clip)');
//
//     var focus = svg.append("g")
//         .style("display", "none");
//
//     // Add the X Axis
//     svg.append("g")
//         .attr("transform", "translate(0," + height + ")")
//         .call(d3.axisBottom(x));
//
//     // Add the Y Axis
//     svg.append("g")
//         .call(d3.axisLeft(y));
//
//
//     // append the x line
//     focus.append("line")
//         .attr("class", "x")
//         .style("stroke", "blue")
//         .style("stroke-dasharray", "3,3")
//         .style("opacity", 0.5)
//         .attr("y1", 0)
//         .attr("y2", height);
//
//     // append the y line
//     focus.append("line")
//         .attr("class", "y")
//         .style("stroke", "blue")
//         .style("stroke-dasharray", "3,3")
//         .style("opacity", 0.5)
//         .attr("x1", width)
//         .attr("x2", width);
//
//     // append the circle at the intersection
//     focus.append("circle")
//         .attr("class", "y")
//         .style("fill", "none")
//         .style("stroke", "blue")
//         .attr("r", 4);
//
//     // place the value at the intersection
//     focus.append("text")
//         .attr("class", "y1")
//         .style("stroke", "white")
//         .style("stroke-width", "3.5px")
//         .style("opacity", 0.8)
//         .attr("dx", 8)
//         .attr("dy", "-.3em");
//     focus.append("text")
//         .attr("class", "y2")
//         .attr("dx", 8)
//         .attr("dy", "-.3em");
//
//     // place the date at the intersection
//     focus.append("text")
//         .attr("class", "y3")
//         .style("stroke", "white")
//         .style("stroke-width", "3.5px")
//         .style("opacity", 0.8)
//         .attr("dx", 8)
//         .attr("dy", "1em");
//     focus.append("text")
//         .attr("class", "y4")
//         .attr("dx", 8)
//         .attr("dy", "1em");
//
//     // append the rectangle to capture mouse
//     svg.append("rect")
//         .attr("width", width)
//         .attr("height", height)
//         .style("fill", "none")
//         .style("pointer-events", "all")
//         .on("mouseover", function() { focus.style("display", null); })
//         .on("mouseout", function() { focus.style("display", "none"); })
//         .on("mousemove", mousemove);
//
//     function mousemove() {
//         var x0 = x.invert(d3.mouse(this)[0]),
//             i = bisectDate(data, x0, 1),
//             d0 = data[i - 1],
//             d1 = data[i],
//             d = x0 - d0.ts > d1.ts - x0 ? d1 : d0;
//
//         focus.select("circle.y")
//             .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")");
//
//         focus.select("text.y1")
//             .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
//             .text(d.t);
//
//         focus.select("text.y2")
//             .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
//             .text(d.t);
//
//         focus.select("text.y3")
//             .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
//             .text(formatDate(d.ts));
//
//         focus.select("text.y4")
//             .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
//             .text(formatDate(d.ts));
//
//         focus.select(".x")
//             .attr("transform", "translate(" + x(d.ts) + "," + y(d.t) + ")")
//             .attr("y2", height - y(d.t));
//
//         focus.select(".y")
//             .attr("transform", "translate(" + width * -1 + "," + y(d.t) + ")")
//             .attr("x2", width + width);
//     }
// });
// // Get the data
// d3.csv("/temperature.csv?sid=dht22-top&topic=A0:20:A6:16:A6:34", function (error, data) {
//     if (error) throw error;
//
//     // format the data
//     data.forEach(function (d) {
//         d.ts = parseTime(d.ts);
//         d.t = +d.t;
//     });
//
//     // Scale the range of the data
// //        x.domain(d3.extent(data, function (d) {
// //            return d.ts;
// //        }));
// //        y.domain([17, 25]);
//     <!--y.domain([0, d3.max(data, function(d) { return d.t; })]);-->
//     svg.datum(data);
//     // Add the valueline path.
//     svg.append("path")
//     //            .data([data])
//         .attr("class", "bottom-line")
//         .attr("d", valueline)
//         .attr('clip-path', 'url(#rect-clip)');
//
//     // Add the X Axis
//     svg.append("g")
//         .attr("transform", "translate(0," + height + ")")
//         .call(d3.axisBottom(x));
// });