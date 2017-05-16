jQuery = require('jquery');
Tether = require('tether');
bootstrap = require('bootstrap');
var d3 = require("d3");
var dateFormat = require('dateformat');

"use strict";
module.exports = {
    view: (title, startElementId) => new DataVisualizer(title, startElementId),
    graph: (view) => new DataGraph(view),
    loader: (url) => new DataLoader(url),
    dynamicLoader: (url) => new DynamicWebSocketDataLoader(url),
};

class DataGraph {
    constructor(dataView) {
        this.dataView = dataView;
        this.seriesDataArray = [];
        this.thermostatDataHolder = [];
        this.parseTime = d3.timeParse("%Y-%m-%d %H:%M:%S");
        // this.defaultDataRange = 1000 * 60 * 60 * 24;
        this.defaultDataRange = 1000 * 60 * 3;
    }

    load(dataLoader) {
        // series by series draw data
        dataLoader.load(dataHolder => {
            this.seriesDataArray[dataHolder.id] = dataHolder;
            dataHolder.seriesValues.forEach(d => this.enrichSensorDatum(d));

            Object.keys(this.seriesDataArray).forEach(key => {
                // adjust domain, todo optimize to a single traverse
                this.xMin = findMin(d3.min(dataHolder.seriesValues, d => d.time), this.xMin);
                this.xMax = findMax(d3.max(dataHolder.seriesValues, d => d.time), this.xMax);
                this.yMin = findMin(d3.min(dataHolder.seriesValues, d => d.temperature), this.yMin);
                this.yMax = findMax(d3.max(dataHolder.seriesValues, d => d.temperature), this.yMax);
            });
            this.dataView.resetDomain(this.xMin, this.xMax, this.yMin, this.yMax, this.seriesDataArray, this.thermostatDataHolder);

            // redraw
            Object.keys(this.seriesDataArray).forEach(key => {
                let dataHolder = this.seriesDataArray[key];
                this.dataView.drawSeries(dataHolder);
            });

            this.dataView.refreshMouseMapping(this.seriesDataArray);
        });
    }

    subscribeToDynamicSensorData(dynamicDataLoader) {
        dynamicDataLoader.load(incrementalUpdate => {
            let dataHolder = this.seriesDataArray[incrementalUpdate.sid];
            if (!dataHolder) {
                dataHolder = new DataHolder(incrementalUpdate.sid, [incrementalUpdate]);
                this.seriesDataArray[incrementalUpdate.sid] = dataHolder;
            }
            this.enrichSensorDatum(incrementalUpdate);

            // drop older than 15 secs from all series, todo parametrize
            let minTime = Date.now() - this.defaultDataRange;
            this.dropOlderSeriesPoints(this.seriesDataArray, minTime);

            dataHolder.seriesValues.push(incrementalUpdate);

            // adjust domain, todo optimize to a single traverse, take into account current scale/zoom factor
            this.yMin = findMin(d3.min(dataHolder.seriesValues, d => d.temperature), this.yMin);
            this.yMax = findMax(d3.max(dataHolder.seriesValues, d => d.temperature), this.yMax);
            this.dataView.resetDomain(new Date(minTime), new Date(), this.yMin, this.yMax, this.seriesDataArray, this.thermostatDataHolder);

            this.redraw();
        });
    }

    subscribeToDynamicZoneData(dynamicZoneDataLoader) {
        dynamicZoneDataLoader.load(incrementalZoneUpdate => {
            this.enrichZoneDatum(incrementalZoneUpdate);
            this.dropOlderTimePoints(this.thermostatDataHolder, Date.now() - this.defaultDataRange);
            this.thermostatDataHolder.push(incrementalZoneUpdate);
            this.redraw();
        });
    }

    redraw() {
        // todo shift the other series instead of re-drawing, faster
        Object.keys(this.seriesDataArray).forEach(key => {
            let dataHolder = this.seriesDataArray[key];
            this.dataView.drawSeries(dataHolder);
        });
        this.dataView.drawSegments(this.thermostatDataHolder);
        this.dataView.refreshMouseMapping(this.seriesDataArray);
        // todo recalc mouse cross
    }

    dropOlderSeriesPoints(seriesDataArray, minTime) {
        Object.keys(seriesDataArray).forEach(key => this.dropOlderTimePoints(seriesDataArray[key].seriesValues, minTime));
    }

    dropOlderTimePoints(timedArray, minTime) {
        while (timedArray.length > 0) {
            if (timedArray[0].timeMillis < minTime
                && timedArray[1] && timedArray[1].timeMillis < minTime) { // leave 1 point so our graph starts at the 0. todo add clipping
                let dropped = timedArray.shift();
                // console.log("Drop: " + JSON.stringify(dropped));
            } else {
                break; // assume it's monotonically incremented function, thus everything else is valid
            }
        }
    }

    enrichSensorDatum(d) {
        // format the data
        d.time = this.parseTime(d.ts); // todo convert to epoch
        d.timeMillis = d.time.getTime();
        d.temperature = +d.t;
        return d;
    }

    enrichZoneDatum(d) {
        // format the data
        d.time = this.parseTime(d.ts);
        d.timeMillis = d.time.getTime();
        return d;
    }
}

class DataHolder {
    constructor(id, seriesValues) {
        this.id = id;
        this.seriesValues = seriesValues;
    }
}

class DataLoader {
    constructor(url) {
        this.url = url;
    }

    load(fullSeriesCallback) {
        d3.csv(this.url.url, (error, data) => {
            if (error) throw error;
            fullSeriesCallback(new DataHolder(this.url.id, data))
        });
    }
}

class DynamicWebSocketDataLoader {
    constructor(url) {
        this.url = url;
        this.maxNumberOfReconnects = 100;
    }

    load(incrementalCallback) {
        this.ws = new WebSocket(this.url);
        this.ws.socketDataLoader = this;
        this.ws.onopen = function () {
            console.log("Connection opened to [" + this.url + "]");
        };

        this.ws.onclose = function () {
            console.log("Connection is closed to [" + this.url + "]. Reconnects left [" + this.socketDataLoader.maxNumberOfReconnects + "]");
            if (this.socketDataLoader.maxNumberOfReconnects-- > 0) {
                console.log("Reconnecting to [" + this.url + "]");
                setTimeout((function () {
                    return this.load(incrementalCallback);
                }).bind(this.socketDataLoader), 10000);
            } else {
                console.log("Connection is closed, system needs reboot [" + this.url + "]. ");
                // todo disable control, notify system it needs to reload
            }
        };
        this.bacon = require('baconjs').Bacon;
        let updateStream = this.bacon.fromEventTarget(this.ws, "message")
            .map(event => {
                try { //console.log(event.data);
                    return JSON.parse(event.data);
                } catch (e) {
                    console.log(event.data);
                    console.log(e);
                }
            });
        let sensorsStream = updateStream.filter(function (update) {
            return update.etype === "TEMPERATURE_SENSOR";
            // return update.type === "unspecified";
        });
        sensorsStream.onValue(json => {
            incrementalCallback(json);
        });
        let thermostatStream = updateStream.filter(function (update) {
            return update.etype === 'THERMOSTAT';
            // return update.type === "unspecified";
        });
        thermostatStream.onValue(json => {
            console.log(JSON.stringify(json));
            incrementalCallback(json);
        });
    }
}

class DataVisualizer {
    constructor(title, startElementId) { // todo accept element
        // set the dimensions and margins of the graph
        this.margin = {top: 20, right: 160, bottom: 30, left: 50};
        this.width = 960 - this.margin.left - this.margin.right;
        this.height = 500 - this.margin.top - this.margin.bottom;

        // set the ranges
        this.x = d3.scaleTime().range([0, this.width]);
        this.y = d3.scaleLinear().range([this.height, 0]);
        this.z = d3.scaleOrdinal(d3.schemeCategory10);

        this.xMin = new Date();
        this.xMax = new Date();
        this.yMin = new Date();
        this.yMax = new Date();
        this.xPrevMin = this.xMin;
        this.xPrevMax = this.xMax;
        this.seriesIdsArray = [];
        this.inverseMapping = [];

        this.svg = d3.select(startElementId)
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom);
        this.g = this.svg.append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");
        this.svg.append("text")
            .attr("x", (this.width / 2))
            .attr("y", 10 + (this.margin.top / 2))
            .attr("text-anchor", "middle")
            .attr("class", "title")
            .text(title);
        this.pathes = this.g.append("g")
            .attr("id", "pathes")
        ;
        this.xAxis = this.g.append("g")
            .attr("class", ".xaxis")
            .attr("transform", "translate(0," + this.height + ")");
        this.yAxis = this.g.append("g")
            .attr("class", ".yaxis");
        this.yAxis
            .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", "0.71em")
            .attr("fill", "#000")
            .text("Temperature, ºC");
        this.zoom = d3.zoom()
            .scaleExtent([1, 32])
            .translateExtent([[0, 0], [this.width, this.height]])
            .extent([[0, 0], [this.width, this.height]])
            .on("zoom", (this.zoomed).bind(this));
        this.g.call(this.zoom);

        // clipping for path'
        this.g.append("defs").append("clipPath")
            .attr("id", "clip")
            .append("rect")
            .attr("width", this.width)
            .attr("height", this.height)
        ;

        this.focus = this.g.append("g")
            .attr("class", "focus")
            .attr("id", "focus123")
            .style("display", "none")
        ;
        // append the vertical line
        this.focus.append("line")
            .attr("class", "vertical-line")
            .attr("y1", 0)
            .attr("y2", this.height);
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
        this.area = d3.area()
            .curve(d3.curveStepAfter)
            .x(d => this.x(d.time))
            .y1(d => this.y(d.state === 'OFF' ? this.yMax : this.yMin));
    }

    zoomed() {
        let t = d3.event.transform;
        this.x = t.rescaleX(this.x);
        this.line = d3.line()
            .curve(d3.curveMonotoneX)
            .x(d => this.x(d.time))
            .y(d => this.y(d.temperature));
        this.area = d3.area()
            .curve(d3.curveStepAfter)
            .x(d => this.x(d.time))
            .y1(d => this.y(d.state === 'OFF' ? this.yMax : this.yMin));
        // this.g.select(".area").attr("d", this.line.x(d => xt(d.date)));
        Object.keys(this.seriesDataArray).forEach(key => {
            let dataHolder = this.seriesDataArray[key];
            let domSeriesId = "series-" + dataHolder.id;
            this.g
                .selectAll("#path-" + domSeriesId)
                .data([dataHolder], d => d.id)
                .attr("d", d => this.line(d.seriesValues))
                .attr("transform", null)
            ;
        });
        this.drawSegments(this.thermostatDataHolder);
        // d => this.line(d.seriesValues)
        // this.g.select(".axis--x").call(xAxis.scale(xt));
    }

    resetDomain(xMin, xMax, yMin, yMax, seriesDataArray, thermostatDataHolder) {
        this.seriesDataArray = seriesDataArray;
        this.thermostatDataHolder = thermostatDataHolder;
        xMax = typeof xMax === 'undefined' ? new Date() : xMax;
        xMin = typeof xMin === 'undefined' ? new Date() : xMin;
        yMin = typeof yMin === 'undefined' ? new Date() : yMin;
        yMax = typeof yMax === 'undefined' ? new Date() : yMax;

        this.xPrevMin = this.xMin;
        this.xPrevMax = this.xMax;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;

        this.x.domain([xMin, xMax]);
        this.y.domain([yMin, yMax]);
    }

    drawSegments(thermostatDataHolder) {
        let domSegmentId = "thermostat-" + "1"; // todo introduce id if needed
        // draw group and create a structure
        let segmentId = this.pathes.selectAll("#" + domSegmentId)
            .data([thermostatDataHolder])
            .enter()
            .append("g")
            .attr("class", "segment")
            .attr("id", domSegmentId);
        // draw line if doesn't exist
        let path = segmentId.selectAll("#segment-" + domSegmentId)
            .data([thermostatDataHolder])
            .enter()
            .append("g")
            .attr("clip-path", "url(#clip)")
            .append("path")
            .attr("class", "line")
            .attr("id", "segment-" + domSegmentId)
        ;
        // ==========  update section
        // update line itself
        if (thermostatDataHolder.length > 0) {
            let lastElem = thermostatDataHolder[thermostatDataHolder.length - 1];
            thermostatDataHolder.push({
                state: lastElem.state,
                etype: lastElem.etype,
                // timeMillis: lastElem.timeMillis + 20 * 1000, // todo get max from the last X position, don't add offsets
                time: typeof this.xMax === 'undefined' ? new Date() : this.xMax
            });
            this.g
                .selectAll("#segment-" + domSegmentId)
                .data([thermostatDataHolder])
                .attr("d", d => this.area(d))
                .attr("transform", null)
                .attr("class", "segment")
            ;
            thermostatDataHolder.pop();// todo make it a field to not create new every time
        }
    }

    drawSeries(dataHolder) {
        this.seriesIdsArray = d3.merge([this.seriesIdsArray, [dataHolder.id]]);
        this.z.domain(this.seriesIdsArray);

        let domSeriesId = "series-" + dataHolder.id;
        // draw group and create a structure
        let seriesId = this.pathes.selectAll("#" + domSeriesId)
            .data([dataHolder], d => d.id)
            .enter()
            .append("g")
            .attr("class", "series")
            .attr("id", domSeriesId);
        // draw line if doesn't exist
        let path = seriesId.selectAll("#path-" + domSeriesId)
                .data([dataHolder], d => d.id)
                .enter()
                .append("g")
                .attr("clip-path", "url(#clip)")
                .append("path")
                .attr("class", "line")
                .attr("id", "path-" + domSeriesId)
                .style("stroke", d => this.z(d.id))
            // .transition()
            // .duration(500)
            // .ease(d3.easeLinear)
            // .on("start", () => {console.log("start")})
        ;
        // draw text at the end
        let text = seriesId.selectAll("#text-" + domSeriesId)
            .data([dataHolder], d => d.id)
            .enter()
            .append("text")
            .attr("id", "text-" + domSeriesId)
            .attr("x", 3)
            .attr("dy", "0.35em")
            .style("font", "12px sans-serif")
        ;
        // append the circle at the intersection
        this.focus
            .selectAll("#cross-" + dataHolder.id)
            .data([dataHolder], series => series.id)
            .enter()
            .append("circle")
            .attr("class", "cross")
            .attr("id", series => "cross-" + series.id)
            .style("stroke", series => this.z(series.id))
            .style("fill", series => this.z(series.id))
            .attr("r", 3);
        // place the value at the intersection
        this.focus
            .selectAll("#cross-value-text-" + dataHolder.id)
            .data([dataHolder], series => series.id)
            .enter()
            .append("text")
            .attr("id", series => "cross-value-text-" + series.id)
            .attr("class", "cross-value-text")
            .attr("dx", 8)
            .attr("dy", "-.3em");
        // place the date at the intersection
        this.focus
            .selectAll("#cross-time-text-" + dataHolder.id)
            .data([dataHolder], series => series.id)
            .enter()
            .append("text")
            .attr("id", series => "cross-time-text-" + series.id)
            .attr("class", "cross-time-text")
            .attr("dx", 8)
            .attr("dy", "1em");

        // ==========  update section
        // let xTranslation = this.x(this.xPrevMax) - this.x(this.xMax);
        // console.log("move [" + dataHolder.id + "] on:" + xTranslation);
        // update line itself
        this.g
            .selectAll("#path-" + domSeriesId)
            .data([dataHolder], d => d.id)
            .attr("d", d => this.line(d.seriesValues))
            .attr("transform", null)
        ;
        // this.pathes.transition()
        //     .duration(1000)
        //     .ease(d3.easeLinear)
        //     .attr("transform", "translate(" + xTranslation + ", 0)");
        this.xAxis
            .transition()
            .duration(1000)
            .ease(d3.easeLinear)
            .call(d3.axisBottom(this.x))
        ;
        this.yAxis
            .call(d3.axisLeft(this.y));

        if (dataHolder.seriesValues.length === 0) {
            return;
        }
        this.g
            .selectAll("#text-" + domSeriesId)
            .data([{
                id: dataHolder.id,
                value: dataHolder.seriesValues[dataHolder.seriesValues.length - 1]
            }])
            .attr("fill", d => this.z(d.id))
            .text(d => d.id)
            .attr("transform", d => "translate(" + this.x(d.value.time) + "," + this.y(d.value.temperature) + ")");
    }

    onMouseMove() {
        // todo optimize to traverse once per load X values structure
        let rawX = ~~d3.mouse(d3.event.currentTarget)[0]; // use integer value of mouse pointer
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
                this.focus
                    .selectAll(".vertical-line")
                    .attr("transform", "translate(" + this.x(d.time) + "," + 0 + ")")
                ;
                enteredOnce = true;
            }
            this.focus
                .selectAll("#cross-" + d.sid)
                .attr("transform", "translate(" + this.x(d.time) + "," + this.y(d.temperature) + ")");
            this.focus
                .selectAll("#cross-value-text-" + d.sid)
                .attr("transform", "translate(" + this.x(d.time) + "," + this.y(d.temperature) + ")")
                .text(d.temperature);
            this.focus
                .selectAll("#cross-time-text-" + d.sid)
                .attr("transform", "translate(" + this.x(d.time) + "," + this.y(d.temperature) + ")")
                .text(dateFormat(d.time, "mmm dd HH:MM"));
        }
    }

    refreshMouseMapping(seriesDataArray) { // todo optimize
        this.inverseMapping = this.calculateInverseMapping(seriesDataArray);
    }

    calculateInverseMapping(seriesDataArray) {
        let inverseMapping = [];
        // create inverse mapping to quickly find mouse position in merged structure
        // todo we already traverse it above, move there
        Object.keys(seriesDataArray).forEach(key => {
            // console.log("key:" + JSON.stringify(key) + "data:" + JSON.stringify(seriesDataArray[key]));
            let dataHolder = seriesDataArray[key];
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
                        if (dotsForThisX[k].sid === dot.sid) {
                            // console.log("filter out: " + JSON.stringify(dot));
                            continue loopBySeriesValues; // got to next dot in series
                        }
                    }
                    // allow only one dot per series in back mapping as hard to find a single value for X mouse coordinate
                    dotsForThisX.push(dot);
                }
            }
        });
        // console.log("inversed:" + JSON.stringify(inverseMapping));
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

class DatesHelper {
    /**
     * Converts the date in d to a date-object. The input can be:
     *   a date object: returned without modification
     *  an array      : Interpreted as [year,month,day]. NOTE: month is 0-11.
     *   a number     : Interpreted as number of milliseconds
     *                  since 1 Jan 1970 (a timestamp)
     *   a string     : Any format supported by the javascript engine, like
     *                  "YYYY/MM/DD", "MM/DD/YYYY", "Jan 31 2009" etc.
     *  an object     : Interpreted as an object with year, month and date
     *                  attributes.  **NOTE** month is 0-11.
     */
    static convert(d) {
        return (
            d.constructor === Date ? d :
                d.constructor === Array ? new Date(d[0], d[1], d[2]) :
                    d.constructor === Number ? new Date(d) :
                        d.constructor === String ? new Date(d) :
                            typeof d === "object" ? new Date(d.year, d.month, d.date) :
                                NaN
        );
    }

    /**
     * Compare two dates (could be of any type supported by the convert
     * function above) and returns:
     *  -1 : if a &lt; b
     *   0 : if a = b
     *   1 : if a > b
     * NaN : if a or b is an illegal date
     * NOTE: The code inside isFinite does an assignment (=).
     */
    static compare(a, b) {
        return (
            isFinite(a = this.convert(a).valueOf()) &&
            isFinite(b = this.convert(b).valueOf()) ?
                (a > b) - (a < b) :
                NaN
        );
    }

    /**
     * Checks if date in d is between dates in start and end.
     * Returns a boolean or NaN:
     *    true  : if d is between start and end (inclusive)
     *    false : if d is before start or after end
     *    NaN   : if one or more of the dates is illegal.
     * NOTE: The code inside isFinite does an assignment (=).
     */
    static inRange(d, start, end) {
        return (
            isFinite(d = this.convert(d).valueOf()) &&
            isFinite(start = this.convert(start).valueOf()) &&
            isFinite(end = this.convert(end).valueOf()) ?
                start <= d && d <= end :
                NaN
        );
    }
}