jQuery = require('jquery');
Tether = require('tether');
bootstrap = require('bootstrap');
var d3 = require("d3");
var dateFormat = require('dateformat');

"use strict";
module.exports = {
    loader: (urls) => new DataLoader(urls),
    view: (startElement) => new DataVisualizer(startElement),
    graph: (dataLoader, view) => new DataGraph(dataLoader, view),
};

class DataGraph {
    // dataLoader = null;
    // dataView = null;
    constructor(dataLoader, dataView) {
        this.dataLoader = dataLoader;
        this.dataView = dataView;
        // series by series draw data
        this.dataLoader.load(seriesDataArray => {
            for (let i = 0; i < seriesDataArray.length; i++) {
                this.dataView.drawSeries(seriesDataArray[i]);
            }
        });
    }

    incrementalUpdate(updates) {
        this.dataView.incrementalUpdate(this.dataLoader.seriesDataArray, updates);
    }
}

class DataHolder {
    // seriesCallBack;
    // seriesValues;
    // id;
    /**
     *
     * @param seriesCallBack function
     * @param id unique series id
     */
    constructor(seriesCallBack, id) {
        this.seriesCallBack = seriesCallBack;
        this.id = id;
        this.seriesValues = [];
    }

    fetchData(error, data) {
        if (error) throw error;
        this.seriesValues = data;
        this.seriesCallBack(data, this.id);
    }
}

class DataLoader {
    constructor(urls) {
        this.countDownSize = urls.length;
        this.seriesDataArray = [];
        this.urls = urls;
    }

    load(allFetchedCallback) {
        this.seriesCallBack = (data, id) => {
            "use strict";
            console.log("Loaded: " + id + ", size: " + data.length);
            if (--this.countDownSize === 0) {
                allFetchedCallback(this.seriesDataArray);
            }
        };

        for (let i = 0; i < this.urls.length; i++) {
            let url = this.urls[i];
            this.seriesDataArray[i] = new DataHolder(this.seriesCallBack, url.id);
            d3.csv(url.url, (this.seriesDataArray[i].fetchData).bind(this.seriesDataArray[i]));
        }
    }
}
class DataVisualizer {
    constructor(startElement) { // todo accept element
        // set the dimensions and margins of the graph
        this.margin = {top: 20, right: 160, bottom: 30, left: 50};
        this.width = 960 - this.margin.left - this.margin.right;
        this.height = 500 - this.margin.top - this.margin.bottom;
        this.parseTime = d3.timeParse("%Y-%m-%d %H:%M:%S");

// set the ranges
        this.x = d3.scaleTime().range([0, this.width]);
        this.y = d3.scaleLinear().range([this.height, 0]);
        this.z = d3.scaleOrdinal(d3.schemeCategory10);

        this.svg = d3.select("body")
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom);
        this.g = this.svg.append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");
        this.xAxis = this.g.append("g")
            .attr("class", ".xaxis");
        this.yAxis = this.g.append("g")
            .attr("class", ".yaxis");
        this.yAxis
            .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", "0.71em")
            .attr("fill", "#000")
            .text("Temperature, ºC");

        this.focus = this.g.append("g").style("display", "none");
        // append the rectangle to capture mouse
        this.g.append("rect")
            .attr("width", this.width)
            .attr("height", this.height)
            .style("fill", "none")
            .style("pointer-events", "all")
            .on("mouseover", () => this.focus.style("display", null))
            .on("mouseout", () => this.focus.style("display", "none"))
            .on("mousemove", (this.onMouseMove).bind(this));

        // define the line
        this.line = d3.line()
            .curve(d3.curveMonotoneX)
            .x(d => this.x(d.time))
            .y(d => this.y(d.temperature));

        this.xMin = new Date();
        this.xMax = new Date();
        this.yMin = new Date();
        this.yMax = new Date();
        this.seriesIdsArray = [];
        this.inverseMapping = [];
    }

    incrementalUpdate(seriesDataArray, dataUpdate) {
        console.log("Update: " + JSON.stringify(dataUpdate));
        seriesDataArray.forEach(dataHolder => {
                if (dataHolder.id === dataUpdate.sid) {
                    dataHolder.seriesValues.push(dataUpdate);
                    this.drawSeries(dataHolder);
                }
            }
        );
    };

    drawSeries(dataHolder) {
        // format the data
        dataHolder.seriesValues.forEach(d => {
            d.time = this.parseTime(d.ts);
            d.timeMillis = d.time.getTime();
            d.temperature = +d.t;
        });
        // todo optimize to a single traverse
        this.xMin = findMin(d3.min(dataHolder.seriesValues, d => d.time), this.xMin);
        this.xMax = findMax(d3.max(dataHolder.seriesValues, d => d.time), this.xMax);
        this.yMin = findMin(d3.min(dataHolder.seriesValues, d => d.temperature), this.yMin);
        this.yMax = findMax(d3.max(dataHolder.seriesValues, d => d.temperature), this.yMax);
        this.seriesIdsArray = d3.merge(this.seriesIdsArray, [dataHolder.id]);

        this.x.domain([this.xMin, this.xMax]);
        this.y.domain([this.yMin, this.yMax]);
        this.z.domain(this.seriesIdsArray);

        this.xAxis
            .attr("transform", "translate(0," + this.height + ")")
            .call(d3.axisBottom(this.x));
        this.yAxis
            .call(d3.axisLeft(this.y));

        let domSeriesId = "series-" + dataHolder.id;
        let seriesId = this.g.selectAll("#" + domSeriesId);
        let text = null;
        if (seriesId.size() === 0) {
            seriesId = this.g
                .append("g")
                .attr("class", "series")
                .attr("id", domSeriesId)
            // .merge(g.select("#" + domSeriesId))
            ;
            text = seriesId.append("text")
                .attr("id", "text-" + domSeriesId)
                .attr("x", 3)
                .attr("dy", "0.35em")
                .style("font", "12px sans-serif")
            ;
            seriesId
                .append("path")
                .attr("class", "line")
                .attr("id", "path-" + domSeriesId);
            this.drawCross([dataHolder]);
        } else {
            text = seriesId.selectAll("#text-" + domSeriesId);
        }
        if (dataHolder.seriesValues.length === 0) {
            return;
        }
        text
            .data([{
                id: dataHolder.id,
                value: dataHolder.seriesValues[dataHolder.seriesValues.length - 1]
            }])
            // .datum(d => d.values.length > 0
            //     ? {id: d.id, value: d.seriesValues[d.seriesValues.length - 1]}
            //     : {id: "none", value: {time: "0", temperature: "0"}})
            .attr("fill", d => this.z(d.id))
            .text(d => d.id)
            .attr("transform", d => "translate(" + this.x(d.value.time) + "," + this.y(d.value.temperature) + ")");

        seriesId.selectAll("#path-" + domSeriesId)
            .datum(dataHolder)
            .attr("d", d => this.line(d.seriesValues))
            .style("stroke", d => this.z(d.id));

        this.inverseMapping = this.calculateInverseMapping([dataHolder]); // todo optimize
    }

    drawCross(dataHolder) {
        for (let i = 0; i < dataHolder.length; i++) {
            let series = dataHolder[i];
            // append the horizontal line
            // focus.append("line")
            //     .attr("class", "horizontal-line")
            //     .attr("id", "horizontal-line-" + i)
            //     .attr("x1", width)
            //     .attr("x2", width);
            // append the circle at the intersection
            this.focus.append("circle")
                .attr("class", "cross")
                .attr("id", "cross-" + i)
                .style("stroke", this.z(series.id))
                .style("fill", this.z(series.id))
                .attr("r", 3);

            // place the value at the intersection
            // focus.append("text")
            //     .attr("id", "value-text-" + i)
            //     .style("stroke", "white")
            //     .style("stroke-width", "3.5px")
            //     .style("opacity", 0.8)
            //     .attr("dx", 8)
            //     .attr("dy", "-.3em");
            this.focus.append("text")
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
            this.focus.append("text")
                .attr("id", "cross-time-text-" + i)
                .attr("dx", 8)
                .attr("dy", "1em");
        }
        // append the vertical line
        this.focus.append("line")
            .attr("class", "vertical-line")
            .attr("y1", 0)
            .attr("y2", this.height);
    }

    onMouseMove() {
        // todo optimize to traverse once per load X values structure
        let rawX = d3.mouse(d3.event.currentTarget)[0];
        let pointsArray = null;
        // search for for any previous point registered
        for (let k = rawX; k >= 0; k--) {
            if (typeof this.inverseMapping[k] !== 'undefined') {
                pointsArray = this.inverseMapping[k];
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
                this.focus.selectAll(".vertical-line")
                // .attr("transform", "translate(" + x(d.time) + "," + y(d.temperature) + ")")
                    .attr("transform", "translate(" + this.x(d.time) + "," + 0 + ")")
                // .attr("y2", height - y(d.temperature))
                // .attr("y2", height)
                ;
                enteredOnce = true;
            }
            this.focus.selectAll("#cross-" + i)
                .attr("transform", "translate(" + this.x(d.time) + "," + this.y(d.temperature) + ")");
            // focus.select("text.y1")
            //     .attr("transform", "translate(" + x(d.time) + "," + y(d.temperature) + ")")
            //     .text(d.temperature);
            this.focus.selectAll("#cross-value-text-" + i)
                .attr("transform", "translate(" + this.x(d.time) + "," + this.y(d.temperature) + ")")
                .text(d.temperature);

            // focus.select("text.y3")
            //     .attr("transform", "translate(" + x(d.time) + "," + y(d.temperature) + ")")
            //     .text(formatDate(d.time));
            //
            this.focus.selectAll("#cross-time-text-" + i)
                .attr("transform", "translate(" + this.x(d.time) + "," + this.y(d.temperature) + ")")
                // .text(dateFormat(d.time, "mmm dd yyyy HH:MM"));
                .text(dateFormat(d.time, "mmm dd HH:MM"));

            // focus.select("#horizontal-line-" + i)
            //     .attr("transform", "translate(" + width * -1 + "," + y(d.temperature) + ")")
            //     .attr("x2", width + width);
        }
    }

    calculateInverseMapping(seriesDataArray) {
        let inverseMapping = [];

        // create inverse mapping to quickly find mouse position in merged structure
        // todo we already traverse it above, move there
        for (let i = 0; i < seriesDataArray.length; i++) {
            let dataHolder = seriesDataArray[i];
            let seriesValues = dataHolder.seriesValues;
            loopBySeriesValues:for (let j = 0; j < seriesValues.length; j++) {
                let dot = seriesValues[j];
                let xValue = round(this.x(dot.time), 0);
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
        return inverseMapping;
    }
}

function round(value, decimals) {
    return Number(Math.round(value + 'e' + decimals) + 'e-' + decimals);
}

function findMin(a, b) {
    return (a >= b) ? b : a;
}

function findMax(a, b) {
    return (a < b) ? b : a;
}
