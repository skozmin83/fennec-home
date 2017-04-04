jQuery = require('jquery');
Tether = require('tether');
bootstrap = require('bootstrap');
var d3 = require("d3");
var dateFormat = require('dateformat');

"use strict";
module.exports = {
    loader : (urls) => new DataLoader(urls),
    view : (startElement) => new DataVisualizer(startElement),
    graph : (dataCollector, view) => new DataGraph(dataCollector, view),
};
function DataGraph(dataCollector, dataView) {
    dataCollector.load(dataView.draw);
}

function DataHolder(seriesCallBack, id) {
    this.id = id;
    this.fetchData = (function (error, data) {
        if (error) throw error;
        this.values = data;
        seriesCallBack(data, this.id);
    }).bind(this);
}
function DataLoader(urls) {
    this.countDownSize = urls.length;
    this.seriesDataArray = [];
    this.load = (allFetchedCallback) => {
        this.seriesCallBack = (data, id) => {
            "use strict";
            console.log("Loaded: " + id + ", size: " + data.length);
            if (--this.countDownSize === 0) {
                allFetchedCallback(this.seriesDataArray);
            }
        };

        for (let i = 0; i < urls.length; i++) {
            let url = urls[i];
            this.seriesDataArray[i] = new DataHolder(this.seriesCallBack, url);
            d3.csv(url, this.seriesDataArray[i].fetchData);
        }
    }
}
function DataVisualizer(startElement) {
    // set the dimensions and margins of the graph
    var margin = {top: 20, right: 160, bottom: 30, left: 50};
    var width = 960 - margin.left - margin.right;
    var height = 500 - margin.top - margin.bottom;
    var parseTime = d3.timeParse("%Y-%m-%d %H:%M:%S");

// set the ranges
    var x = d3.scaleTime().range([0, width]);
    var y = d3.scaleLinear().range([height, 0]);
    var z = d3.scaleOrdinal(d3.schemeCategory10);

    var svg = d3.select("body")
        .append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom);
    var g = svg.append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    this.draw = function(seriesDataArray) {
        // format the data
        seriesDataArray.forEach(function (dataHolder) {
                dataHolder.values.forEach(function (d) {
                    let ts = parseTime(d.ts);
                    d.ts = ts.getTime();
                    d.time = ts;
                    d.t = +d.t;
                })
            }
        );
        // todo optimize to a single traverse
        x.domain([
            d3.min(seriesDataArray, function (c) {
                return d3.min(c.values, function (d) {
                    return d.time;
                });
            }),
            d3.max(seriesDataArray, function (c) {
                return d3.max(c.values, function (d) {
                    return d.time;
                });
            })
        ]);
        y.domain([
            d3.min(seriesDataArray, function (c) {
                return d3.min(c.values, function (d) {
                    return d.t;
                });
            }),
            d3.max(seriesDataArray, function (c) {
                return d3.max(c.values, function (d) {
                    return d.t;
                });
            })]
        );
        z.domain(seriesDataArray.map(function (c) {
            return c.id;
        }));

        // define the line
        let line = d3.line()
            .curve(d3.curveMonotoneX)
            .x(function (d) {
                return x(d.time);
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
        seriesId.append("text")
            .datum(function (d) {
                return d.values.length > 0
                    ? {id: d.id, value: d.values[d.values.length - 1]}
                    : {id: "none", value: {time: "0", t: "0"}};
            })
            .attr("transform", function (d) {
                return "translate(" + x(d.value.time) + "," + y(d.value.t) + ")";
            })
            .attr("fill", function (d) {
                return z(d.id);
            })
            .attr("x", 3)
            .attr("dy", "0.35em")
            .style("font", "12px sans-serif")
            .text(function (d) {
                return d.value.id;
            });

        let focus = g.append("g").style("display", "none");
        for (let i = 0; i < seriesDataArray.length; i++) {
            let series = seriesDataArray[i];
            // append the horizontal line
            // focus.append("line")
            //     .attr("class", "horizontal-line")
            //     .attr("id", "horizontal-line-" + i)
            //     .attr("x1", width)
            //     .attr("x2", width);
            // append the circle at the intersection
            focus.append("circle")
                .attr("class", "cross")
                .attr("id", "cross-" + i)
                .attr("r", 3);

            // place the value at the intersection
            // focus.append("text")
            //     .attr("id", "value-text-" + i)
            //     .style("stroke", "white")
            //     .style("stroke-width", "3.5px")
            //     .style("opacity", 0.8)
            //     .attr("dx", 8)
            //     .attr("dy", "-.3em");
            focus.append("text")
                .attr("id", "cross-value-text-" + i)
                .attr("dx", 8)
                .attr("dy", "-.3em");

            // place the date at the intersection
            // focus.append("text")
            //     .attr("class", "y3")
            //     .style("stroke", "white")
            //     .style("stroke-width", "3.5px")
            //     .style("opacity", 0.8)
            //     .attr("dx", 8)
            //     .attr("dy", "1em");
            focus.append("text")
                .attr("id", "cross-time-text-" + i)
                .attr("dx", 8)
                .attr("dy", "1em");
        }
        // append the vertical line
        focus.append("line")
            .attr("class", "vertical-line")
            .attr("y1", 0)
            .attr("y2", height);
        // append the rectangle to capture mouse
        g.append("rect")
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
            // todo optimize to traverse once per load X values structure
            let rawX = d3.mouse(this)[0];
            let pointsArray = null;
            // search for for any previous point registered
            for (let k = rawX; k >= 0; k--) {
                if (typeof inverseMapping[k] !== 'undefined') {
                    pointsArray = inverseMapping[k];
                    break;
                }
            }
            if (pointsArray === null) {
                return;
            }
            let enteredOnce = false;
            for (let i = 0; i < pointsArray.length; i++) {
                let d = pointsArray[i];
                if (enteredOnce === false) {
                    focus.select(".vertical-line")
                    // .attr("transform", "translate(" + x(d.time) + "," + y(d.t) + ")")
                        .attr("transform", "translate(" + x(d.time) + "," + 0 + ")")
                    // .attr("y2", height - y(d.t))
                    // .attr("y2", height)
                    ;
                    enteredOnce = true;
                }
                focus.select("#cross-" + i)
                    .attr("transform", "translate(" + x(d.time) + "," + y(d.t) + ")");
                // focus.select("text.y1")
                //     .attr("transform", "translate(" + x(d.time) + "," + y(d.t) + ")")
                //     .text(d.t);
                focus.select("#cross-value-text-" + i)
                    .attr("transform", "translate(" + x(d.time) + "," + y(d.t) + ")")
                    .text(d.t);

                // focus.select("text.y3")
                //     .attr("transform", "translate(" + x(d.time) + "," + y(d.t) + ")")
                //     .text(formatDate(d.time));
                //
                focus.select("#cross-time-text-" + i)
                    .attr("transform", "translate(" + x(d.time) + "," + y(d.t) + ")")
                    // .text(dateFormat(d.time, "mmm dd yyyy HH:MM"));
                    .text(dateFormat(d.time, "mmm dd HH:MM"));

                // focus.select("#horizontal-line-" + i)
                //     .attr("transform", "translate(" + width * -1 + "," + y(d.t) + ")")
                //     .attr("x2", width + width);
            }
        }

        function round(value, decimals) {
            return Number(Math.round(value + 'e' + decimals) + 'e-' + decimals);
        }

        // create inverse mapping to quickly find mouse position in merged structure
        // todo we already traverse it above, move there
        let inverseMapping = [];
        for (let i = 0; i < seriesDataArray.length; i++) {
            let dataHolder = seriesDataArray[i];
            let seriesValues = dataHolder.values;
            loopBySeriesValues:for (let j = 0; j < seriesValues.length; j++) {
                let dot = seriesValues[j];
                let xValue = round(x(dot.time), 0);
                let dotsForThisX = inverseMapping[xValue];
                if (typeof dotsForThisX === 'undefined') {
                    inverseMapping[xValue] = [dot];
                } else { // already contains element
                    // we allow only one dot per series==id
                    for (let k = 0; k < dotsForThisX.length; k++) { // todo can optimize
                        if (dotsForThisX[k].id === dot.id) {
                            // console.log("filter out: " + JSON.stringify(dot));
                            continue loopBySeriesValues; // got to next dot in series
                        }
                    }
                    // allow only one dot per series in back mapping as hard to find a single value for X mouse coordinate
                    dotsForThisX.push(dot);
                }
            }
        }
    }
}
