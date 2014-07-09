@(implicit r: RequestHeader)

$(function() {

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var ws = new WS("@routes.Application.socket().webSocketURL()")
    var i = 0

    var stockprice = 0;

    /*
    var n = 243,
        duration = 750,
        now = new Date(Date.now() - duration),
        count = 0,
        data = d3.range(n).map(function() { return 0; });


    var margin = {top: 6, right: 0, bottom: 20, left: 40},
        width = 960 - margin.right,
        height = 150 - margin.top - margin.bottom;

    var x = d3.time.scale()
        .domain([now - (n - 2) * duration, now - duration])
        .range([0, width]);

    var y = d3.scale.linear()
        .range([height, 0]);

    var line = d3.svg.line()
        .interpolate("basis")
        .x(function(d, i) { return x(now - (n - 1 - i) * duration); })
        .y(function(d, i) { return y(d); });

    var svg = d3.select("#stockportfolio").append("p").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .style("margin-left", -margin.left + "px")
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    svg.append("defs").append("clipPath")
        .attr("id", "clip")
        .append("rect")
        .attr("width", width)
        .attr("height", height);

    var axis = svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(x.axis = d3.svg.axis().scale(x).orient("bottom"));

    var path = svg.append("g")
        .attr("clip-path", "url(#clip)")
        .append("path")
        .data([data])
        .attr("class", "line");

    var tick = function() {
        // update the domains
        now = new Date();
        x.domain([now - (n - 2) * duration, now - duration]);
        y.domain([0, 1]);

        data.push(stockprice);
        count = 0;

        // redraw the line
        svg.select(".line")
            .attr("d", line)
            .attr("transform", null);

        // slide the x-axis left
        axis.transition()
            .duration(duration)
            .ease("linear")
            .call(x.axis);

        // slide the line left
        path.transition()
            .duration(duration)
            .ease("linear")
            .attr("transform", "translate(" + x(now - (n - 1) * duration) + ")")
            .each("end", tick);

        // pop the old data point off the front
        data.shift();
    }
    tick()

    */
     var receiveEvent = function(event) {
        var stockportfolio = JSON.parse(event.data)

        $("#price").html(stockportfolio.value)

        //update the graph
        stockprice = stockportfolio.value
     }


    ws.onmessage = receiveEvent

    this.send = function (message) {
        this.waitForConnection(function () {
            ws.send(message);
        }, 1000);
    };

    this.waitForConnection = function (callback, interval) {
        if (ws.readyState === 1) {
            callback();
        } else {
            var that = this;
            setTimeout(function () {
                that.waitForConnection(callback);
            }, interval);
        }
    };

    this.send(JSON.stringify({"portfolioElements":{"YHOO":"1"}}))


    Highcharts.setOptions({
        global : {
            useUTC : false
        }
    });

    // Create the chart
    $('#stockportfolio2').highcharts('StockChart', {
        chart : {
            events : {
                load : function() {

                    // set up the updating of the chart each second
                    var series = this.series[0];
                    setInterval(function() {
                        var x = (new Date()).getTime(), // current time
                            y = stockprice;
                        series.addPoint([x, y], true, true);
                    }, 1000);
                }
            }
        },

        rangeSelector: {
            buttons: [{
                count: 1,
                type: 'minute',
                text: '1M'
            }, {
                count: 5,
                type: 'minute',
                text: '5M'
            }, {
                type: 'all',
                text: 'All'
            }],
            inputEnabled: false,
            selected: 0
        },

        title : {
            text : 'Streaming live data'
        },

        exporting: {
            enabled: false
        },

        series : [{
            name : 'Data',
            data : (function() {
                // generate an array of random data
                var data = [], time = (new Date()).getTime(), i;

                for( i = -500; i <= 0; i++) {
                    data.push([
                            time + i * 1000,
                        0
                    ]);
                }
                return data;
            })()
        }]
    });

})

