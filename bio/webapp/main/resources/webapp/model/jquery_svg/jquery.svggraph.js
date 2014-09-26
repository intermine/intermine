﻿/* http://keith-wood.name/svg.html
   SVG graphing extension for jQuery v1.4.3.
   Written by Keith Wood (kbwood{at}iinet.com.au) August 2007.
   Dual licensed under the GPL (http://dev.jquery.com/browser/trunk/jquery/GPL-LICENSE.txt) and
   MIT (http://dev.jquery.com/browser/trunk/jquery/MIT-LICENSE.txt) licenses.
   Please attribute the author if you use it. */

(function($) { // Hide scope, no $ conflict

$.svg.addExtension('graph', SVGGraph);

// Singleton primary SVG graphing interface
$.svg.graphing = new SVGGraphing();

function SVGGraphing() {
	this.regional = [];
	this.regional[''] = {percentageText: 'Percentage'};
	this.region = this.regional[''];
}

$.extend(SVGGraphing.prototype, {
	_chartTypes: [],

	/* Add a new chart rendering type to the package.
	   The rendering object must implement the following functions:
	   getTitle(), getDescription(), getOptions(), drawChart(graph).
	   @param  id         (string) the ID of this graph renderer
	   @param  chartType  (object) the object implementing this chart type */
	addChartType: function(id, chartType) {
		this._chartTypes[id] = chartType;
	},

	/* Retrieve the list of chart types.
	   @return  (object[string]) the array of chart types indexed by ID */
	chartTypes: function() {
		return this._chartTypes;
	}
});

/* Extension point for SVG graphing.
   Access through svg.graph. */
function SVGGraph(wrapper) {
	this._wrapper = wrapper; // The attached SVG wrapper object
	this._drawNow = false; // True for immediate update, false to wait for redraw call
	for (var id in $.svg.graphing._chartTypes) {
		this._chartType = $.svg.graphing._chartTypes[id]; // Use first graph renderer
		break;
	}
	this._chartOptions = {}; // Extra options for the graph type
	// The graph title and settings
	this._title = {value: '', offset: 25, settings: {textAnchor: 'middle'}};
	this._area = [0.1, 0.1, 0.8, 0.9]; // The chart area: left, top, right, bottom,
		// > 1 in pixels, <= 1 as proportion
	this._chartFormat = {fill: 'none', stroke: 'black'}; // The formatting for the chart area
	this._gridlines = []; // The formatting of the x- and y-gridlines
	this._series = []; // The series to be plotted, each is an object
	this._onstatus = null; // The callback function for status updates
	this._chartCont = this._wrapper.svg(0, 0, 0, 0, {class_: 'svg-graph'}); // The main container for the graph
	
	this.xAxis = new SVGGraphAxis(this); // The main x-axis
	this.xAxis.title('', 40);
	this.yAxis = new SVGGraphAxis(this); // The main y-axis
	this.yAxis.title('', 40);
	this.x2Axis = null; // The secondary x-axis
	this.y2Axis = null; // The secondary y-axis
	this.legend = new SVGGraphLegend(this); // The chart legend
	this._drawNow = true;
}

$.extend(SVGGraph.prototype, {

	/* Useful indexes. */
	X: 0,
	Y: 1,
	W: 2,
	H: 3,
	L: 0,
	T: 1,
	R: 2,
	B: 3,

	/* Standard percentage axis. */
	_percentageAxis: new SVGGraphAxis(this, $.svg.graphing.region.percentageText, 0, 100, 10, 0),

	/* Set or retrieve the container for the graph.
	   @param  cont  (SVG element) the container for the graph
	   @return  (SVGGraph) this graph object or
	            (SVG element) the current container (if no parameters) */
	container: function(cont) {
		if (arguments.length == 0) {
			return this._chartCont;
		}
		this._chartCont = cont;
		return this;
	},

	/* Set or retrieve the type of chart to be rendered.
	   See $.svg.graphing.getChartTypes() for the list of available types.
	   @param  id       (string) the ID of the chart type
	   @param  options  (object) additional settings for this chart type (optional)
	   @return  (SVGGraph) this graph object or
	            (string) the chart type (if no parameters)
	   @deprecated  use type() */
	chartType: function(id, options) {
		return (arguments.length == 0 ? this.type() : this.type(id, options));
	},

	/* Set or retrieve the type of chart to be rendered.
	   See $.svg.graphing.getChartTypes() for the list of available types.
	   @param  id       (string) the ID of the chart type
	   @param  options  (object) additional settings for this chart type (optional)
	   @return  (SVGGraph) this graph object or
	            (string) the chart type (if no parameters) */
	type: function(id, options) {
		if (arguments.length == 0) {
			return this._chartType;
		}
		var chartType = $.svg.graphing._chartTypes[id];
		if (chartType) {
			this._chartType = chartType;
			this._chartOptions = $.extend({}, options || {});
		}
		this._drawGraph();
		return this;
	},

	/* Set or retrieve additional options for the particular chart type.
	   @param  options  (object) the extra options
	   @return  (SVGGraph) this graph object or
	            (object) the chart options (if no parameters)
	   @deprecated  use options() */
	chartOptions: function(options) {
		return(arguments.length == 0 ? this.options() : this.options(options));
	},

	/* Set or retrieve additional options for the particular chart type.
	   @param  options  (object) the extra options
	   @return  (SVGGraph) this graph object or
	            (object) the chart options (if no parameters) */
	options: function(options) {
		if (arguments.length == 0) {
			return this._chartOptions;
		}
		this._chartOptions = $.extend({}, options);
		this._drawGraph();
		return this;
	},

	/* Set or retrieve the background of the graph chart.
	   @param  fill      (string) how to fill the chart background
	   @param  stroke    (string) the colour of the outline (optional)
	   @param  settings  (object) additional formatting for the chart background (optional)
	   @return  (SVGGraph) this graph object or
	            (object) the chart format (if no parameters)
	   @deprecated  use format() */
	chartFormat: function(fill, stroke, settings) {
		return (arguments.length == 0 ? this.format() : this.format(fill, stroke, settings));
	},

	/* Set or retrieve the background of the graph chart.
	   @param  fill      (string) how to fill the chart background
	   @param  stroke    (string) the colour of the outline (optional)
	   @param  settings  (object) additional formatting for the chart background (optional)
	   @return  (SVGGraph) this graph object or
	            (object) the chart format (if no parameters) */
	format: function(fill, stroke, settings) {
		if (arguments.length == 0) {
			return this._chartFormat;
		}
		if (typeof stroke == 'object') {
			settings = stroke;
			stroke = null;
		}
		this._chartFormat = $.extend({fill: fill},
			(stroke ? {stroke: stroke} : {}), settings || {});
		this._drawGraph();
		return this;
	},

	/* Set or retrieve the main chart area.
	   @param  left    (number) > 1 is pixels, <= 1 is proportion of width or
	                   (number[4]) for left, top, right, bottom
	   @param  top     (number) > 1 is pixels, <= 1 is proportion of height
	   @param  right   (number) > 1 is pixels, <= 1 is proportion of width
	   @param  bottom  (number) > 1 is pixels, <= 1 is proportion of height
	   @return  (SVGGraph) this graph object or
	            (number[4]) the chart area: left, top, right, bottom (if no parameters)
	   @deprecated use area() */
	chartArea: function(left, top, right, bottom) {
		return (arguments.length == 0 ? this.area() : this.area(left, top, right, bottom));
	},

	/* Set or retrieve the main chart area.
	   @param  left    (number) > 1 is pixels, <= 1 is proportion of width or
	                   (number[4]) for left, top, right, bottom
	   @param  top     (number) > 1 is pixels, <= 1 is proportion of height
	   @param  right   (number) > 1 is pixels, <= 1 is proportion of width
	   @param  bottom  (number) > 1 is pixels, <= 1 is proportion of height
	   @return  (SVGGraph) this graph object or
	            (number[4]) the chart area: left, top, right, bottom (if no parameters) */
	area: function(left, top, right, bottom) {
		if (arguments.length == 0) {
			return this._area;
		}
		this._area = (isArray(left) ? left : [left, top, right, bottom]);
		this._drawGraph();
		return this;
	},

	/* Set or retrieve the gridlines formatting for the graph chart.
	   @param  xSettings  (string) the colour of the gridlines along the x-axis, or
	                      (object) formatting for the gridlines along the x-axis, or
	                      null for none
	   @param  ySettings  (string) the colour of the gridlines along the y-axis, or
	                      (object) formatting for the gridlines along the y-axis, or
	                      null for none
	   @return  (SVGGraph) this graph object or
	            (object[2]) the gridlines formatting (if no parameters) */
	gridlines: function(xSettings, ySettings) {
		if (arguments.length == 0) {
			return this._gridlines;
		}
		this._gridlines = [(typeof xSettings == 'string' ? {stroke: xSettings} : xSettings),
			(typeof ySettings == 'string' ? {stroke: ySettings} : ySettings)];
		if (this._gridlines[0] == null && this._gridlines[1] == null) {
			this._gridlines = [];
		}
		this._drawGraph();
		return this;
	},

	/* Set or retrieve the title of the graph and its formatting.
	   @param  value     (string) the title
	   @param  offset    (number) the vertical positioning of the title
                          > 1 is pixels, <= 1 is proportion of width (optional)
	   @param  colour    (string) the colour of the title (optional)
	   @param  settings  (object) formatting for the title (optional)
	   @return  (SVGGraph) this graph object or
	            (object) value, offset, and settings for the title (if no parameters) */
	title: function(value, offset, colour, settings) {
		if (arguments.length == 0) {
			return this._title;
		}
		if (typeof offset != 'number') {
			settings = colour;
			colour = offset;
			offset = null;
		}
		if (typeof colour != 'string') {
			settings = colour;
			colour = null;
		}
		this._title = {value: value, offset: offset || this._title.offset,
			settings: $.extend({textAnchor: 'middle'},
			(colour ? {fill: colour} : {}), settings || {})};
		this._drawGraph();
		return this;
	},

	/* Add a series of values to be plotted on the graph.
	   @param  name         (string) the name of this series (optional)
	   @param  values       (number[]) the values to be plotted
	   @param  fill         (string) how the plotted values are filled
	   @param  stroke       (string) the colour of the plotted lines (optional)
	   @param  strokeWidth  (number) the width of the plotted lines (optional)
	   @param  settings     (object) additional settings for the plotted values (optional)
	   @return  (SVGGraph) this graph object */
	addSeries: function(name, values, fill, stroke, strokeWidth, settings) {
		this._series.push(new SVGGraphSeries(
			this, name, values, fill, stroke, strokeWidth, settings));
		this._drawGraph();
		return this;
	},

	/* Retrieve the series wrappers.
	   @param  i  (number) the series index (optional)
	   @return  (SVGGraphSeries) the specified series or
	            (SVGGraphSeries[]) the list of series */
	series: function(i) {
		return (arguments.length > 0 ? this._series[i] : null) || this._series;
	},

	/* Suppress drawing of the graph until redraw() is called.
	   @return  (SVGGraph) this graph object */
	noDraw: function() {
		this._drawNow = false;
		return this;
	},

	/* Redraw the entire graph with the current settings and values.
	   @return  (SVGGraph) this graph object */
	redraw: function() {
		this._drawNow = true;
		this._drawGraph();
		return this;
	},

	/* Set the callback function for status updates.
	   @param  onstatus  (function) the callback function
	   @return  (SVGGraph) this graph object */
	status: function(onstatus) {
		this._onstatus = onstatus;
		return this;
	},

	/* Actually draw the graph (if allowed) based on the graph type set. */
	_drawGraph: function() {
		if (!this._drawNow) {
			return;
		}
		while (this._chartCont.firstChild) {
			this._chartCont.removeChild(this._chartCont.firstChild);
		}
		if (!this._chartCont.parent) {
			this._wrapper._svg.appendChild(this._chartCont);
		}
		// Set sizes if not already there
		if (!this._chartCont.width) {
			this._chartCont.setAttribute('width',
				parseInt(this._chartCont.getAttribute('width'), 10) || this._wrapper._width());
		}
		else if (this._chartCont.width.baseVal) {
			this._chartCont.width.baseVal.value =
				this._chartCont.width.baseVal.value || this._wrapper._width();
		}
		else {
			this._chartCont.width = this._chartCont.width || this._wrapper._width();
		}
		if (!this._chartCont.height) {
			this._chartCont.setAttribute('height',
				parseInt(this._chartCont.getAttribute('height'), 10) || this._wrapper._height());
		}
		else if (this._chartCont.height.baseVal) {
			this._chartCont.height.baseVal.value =
				this._chartCont.height.baseVal.value || this._wrapper._height();
		}
		else {
			this._chartCont.height = this._chartCont.height || this._wrapper._height();
		}
		this._chartType.drawGraph(this);
	},

	/* Decode an attribute value.
	   @param  node  the node to examine
	   @param  name  the attribute name
	   @return  the actual value */
	_getValue: function(node, name) {
		return (!node[name] ? parseInt(node.getAttribute(name), 10) :
			(node[name].baseVal ? node[name].baseVal.value : node[name]));
	},

	/* Draw the graph title - centred. */
	_drawTitle: function() {
		this._wrapper.text(this._chartCont, this._getValue(this._chartCont, 'width') / 2,
			this._title.offset, this._title.value, this._title.settings);
	},

	/* Calculate the actual dimensions of the chart area.
	   @param  area  (number[4]) the area values to evaluate (optional)
	   @return  (number[4]) an array of dimension values: left, top, width, height */
	_getDims: function(area) {
		area = area || this._area;
		var availWidth = this._getValue(this._chartCont, 'width');
		var availHeight = this._getValue(this._chartCont, 'height');
		var left = (area[this.L] > 1 ? area[this.L] : availWidth * area[this.L]);
		var top = (area[this.T] > 1 ? area[this.T] : availHeight * area[this.T]);
		var width = (area[this.R] > 1 ? area[this.R] : availWidth * area[this.R]) - left;
		var height = (area[this.B] > 1 ? area[this.B] : availHeight * area[this.B]) - top;
		return [left, top, width, height];
	},

	/* Draw the chart background, including gridlines.
	   @param  noXGrid  (boolean) true to suppress the x-gridlines, false to draw them (optional)
	   @param  noYGrid  (boolean) true to suppress the y-gridlines, false to draw them (optional)
	   @return  (element) the background group element */
	_drawChartBackground: function(noXGrid, noYGrid) {
		var bg = this._wrapper.group(this._chartCont, {class_: 'background'});
		var dims = this._getDims();
		this._wrapper.rect(bg, dims[this.X], dims[this.Y], dims[this.W], dims[this.H], this._chartFormat);
		if (this._gridlines[0] && this.yAxis._ticks.major && !noYGrid) {
			this._drawGridlines(bg, this.yAxis, true, dims, this._gridlines[0]);
		}
		if (this._gridlines[1] && this.xAxis._ticks.major && !noXGrid) {
			this._drawGridlines(bg, this.xAxis, false, dims, this._gridlines[1]);
		}
		return bg;
	},

	/* Draw one set of gridlines.
	   @param  bg      (element) the background group element
	   @param  axis    (SVGGraphAxis) the axis definition
	   @param  horiz   (boolean) true if horizontal, false if vertical
	   @param  dims    (number[]) the left, top, width, height of the chart area
	   @param  format  (object) additional settings for the gridlines */
	_drawGridlines: function(bg, axis, horiz, dims, format) {
		var g = this._wrapper.group(bg, format);
		var scale = (horiz ? dims[this.H] : dims[this.W]) / (axis._scale.max - axis._scale.min);
		var major = Math.floor(axis._scale.min / axis._ticks.major) * axis._ticks.major;
		major = (major < axis._scale.min ? major + axis._ticks.major : major);
		while (major <= axis._scale.max) {
			var v = (horiz ? axis._scale.max - major : major - axis._scale.min) * scale +
				(horiz ? dims[this.Y] : dims[this.X]);
			this._wrapper.line(g, (horiz ? dims[this.X] : v), (horiz ? v : dims[this.Y]),
				(horiz ? dims[this.X] + dims[this.W] : v), (horiz ? v : dims[this.Y] + dims[this.H]));
			major += axis._ticks.major;
		}
	},

	/* Draw the axes in their standard configuration.
	   @param  noX  (boolean) true to suppress the x-axes, false to draw it (optional) */
	_drawAxes: function(noX) {
		var dims = this._getDims();
		if (this.xAxis && !noX) {
			if (this.xAxis._title) {
				this._wrapper.text(this._chartCont, dims[this.X] + dims[this.W] / 2,
					dims[this.Y] + dims[this.H] + this.xAxis._titleOffset,
					this.xAxis._title, this.xAxis._titleFormat);
			}
			this._drawAxis(this.xAxis, 'xAxis', dims[this.X], dims[this.Y] + dims[this.H],
				dims[this.X] + dims[this.W], dims[this.Y] + dims[this.H]);
		}
		if (this.yAxis) {
			if (this.yAxis._title) {
				this._wrapper.text(this._chartCont, 0, 0, this.yAxis._title, $.extend({textAnchor: 'middle',
					transform: 'translate(' + (dims[this.X] - this.yAxis._titleOffset) + ',' +
					(dims[this.Y] + dims[this.H] / 2) + ') rotate(-90)'}, this.yAxis._titleFormat || {}));
			}
			this._drawAxis(this.yAxis, 'yAxis', dims[this.X], dims[this.Y],
				dims[this.X], dims[this.Y] + dims[this.H]);
		}
		if (this.x2Axis && !noX) {
			if (this.x2Axis._title) {
				this._wrapper.text(this._chartCont, dims[this.X] + dims[this.W] / 2,
					dims[this.X] - this.x2Axis._titleOffset, this.x2Axis._title, this.x2Axis._titleFormat);
			}
			this._drawAxis(this.x2Axis, 'x2Axis', dims[this.X], dims[this.Y],
				dims[this.X] + dims[this.W], dims[this.Y]);
		}
		if (this.y2Axis) {
			if (this.y2Axis._title) {
				this._wrapper.text(this._chartCont, 0, 0, this.y2Axis._title, $.extend({textAnchor: 'middle',
					transform: 'translate(' + (dims[this.X] + dims[this.W] + this.y2Axis._titleOffset) +
					',' + (dims[this.Y] + dims[this.H] / 2) + ') rotate(-90)'}, this.y2Axis._titleFormat || {}));
			}
			this._drawAxis(this.y2Axis, 'y2Axis', dims[this.X] + dims[this.W], dims[this.Y],
				dims[this.X] + dims[this.W], dims[this.Y] + dims[this.H]);
		}
	},

	/* Draw an axis and its tick marks.
	   @param  axis  (SVGGraphAxis) the axis definition
	   @param  id    (string) the identifier for the axis group element
	   @param  x1    (number) starting x-coodinate for the axis
	   @param  y1    (number) starting y-coodinate for the axis
	   @param  x2    (number) ending x-coodinate for the axis
	   @param  y2    (number) ending y-coodinate for the axis */
	_drawAxis: function(axis, id, x1, y1, x2, y2) {
		var horiz = (y1 == y2);
		var gl = this._wrapper.group(this._chartCont, $.extend({class_: id}, axis._lineFormat));
		var gt = this._wrapper.group(this._chartCont, $.extend({class_: id + 'Labels',
			textAnchor: (horiz ? 'middle' : 'end')}, axis._labelFormat));
		this._wrapper.line(gl, x1, y1, x2, y2);
		if (axis._ticks.major) {
			var bottomRight = (x2 > (this._getValue(this._chartCont, 'width') / 2) &&
				y2 > (this._getValue(this._chartCont, 'height') / 2));
			var scale = (horiz ? x2 - x1 : y2 - y1) / (axis._scale.max - axis._scale.min);
			var size = axis._ticks.size;
			var major = Math.floor(axis._scale.min / axis._ticks.major) * axis._ticks.major;
			major = (major < axis._scale.min ? major + axis._ticks.major : major);
			var minor = (!axis._ticks.minor ? axis._scale.max + 1 :
				Math.floor(axis._scale.min / axis._ticks.minor) * axis._ticks.minor);
			minor = (minor < axis._scale.min ? minor + axis._ticks.minor : minor);
			var offsets = this._getTickOffsets(axis, bottomRight);
			var count = 0;
			while (major <= axis._scale.max || minor <= axis._scale.max) {
				var cur = Math.min(major, minor);
				var len = (cur == major ? size : size / 2);
				var v = (horiz ? x1 : y1) +
					(horiz ? cur - axis._scale.min : axis._scale.max - cur) * scale;
				this._wrapper.line(gl, (horiz ? v : x1 + len * offsets[0]),
					(horiz ? y1 + len * offsets[0] : v),
					(horiz ? v : x1 + len * offsets[1]),
					(horiz ? y1 + len * offsets[1] : v));
				if (cur == major) {
					this._wrapper.text(gt, (horiz ? v : x1 - size), (horiz ? y1 + 2 * size : v),
						(axis._labels ? axis._labels[count++] : '' + cur));
				}
				major += (cur == major ? axis._ticks.major : 0);
				minor += (cur == minor ? axis._ticks.minor : 0);
			}
		}
	},

	/* Calculate offsets based on axis and tick positions.
	   @param  axis         (SVGGraphAxis) the axis definition
	   @param  bottomRight  (boolean) true if this axis is appearing on the bottom or
	                        right of the chart area, false if to the top or left
	   @return  (number[2]) the array of offset multipliers (-1..+1) */
	_getTickOffsets: function(axis, bottomRight) {
		return [(axis._ticks.position == (bottomRight ? 'in' : 'out') ||
			axis._ticks.position == 'both' ? -1 : 0),
			(axis._ticks.position == (bottomRight ? 'out' : 'in') ||
			axis._ticks.position == 'both' ? +1 : 0), ];
	},

	/* Retrieve the standard percentage axis.
	   @return  (SVGGraphAxis) percentage axis */
	_getPercentageAxis: function() {
		this._percentageAxis._title = $.svg.graphing.region.percentageText;
		return this._percentageAxis;
	},

	/* Calculate the column totals across all the series. */
	_getTotals: function() {
		var totals = [];
		var numVal = (this._series.length ? this._series[0]._values.length : 0);
		for (var i = 0; i < numVal; i++) {
			totals[i] = 0;
			for (var j = 0; j < this._series.length; j++) {
				totals[i] += this._series[j]._values[i];
			}
		}
		return totals;
	},

	/* Draw the chart legend. */
	_drawLegend: function() {
		if (!this.legend._show) {
			return;
		}
		var g = this._wrapper.group(this._chartCont, {class_: 'legend'});
		var dims = this._getDims(this.legend._area);
		this._wrapper.rect(g, dims[this.X], dims[this.Y], dims[this.W], dims[this.H],
			this.legend._bgSettings);
		var horiz =  dims[this.W] > dims[this.H];
		var numSer = this._series.length;
		var offset = (horiz ? dims[this.W] : dims[this.H]) / numSer;
		var xBase = dims[this.X] + 5;
		var yBase = dims[this.Y] + ((horiz ? dims[this.H] : offset) + this.legend._sampleSize) / 2;
		for (var i = 0; i < numSer; i++) {
			var series = this._series[i];
			this._wrapper.rect(g, xBase + (horiz ? i * offset : 0),
				yBase + (horiz ? 0 : i * offset) - this.legend._sampleSize,
				this.legend._sampleSize, this.legend._sampleSize,
				{fill: series._fill, stroke: series._stroke, strokeWidth: 1});
			this._wrapper.text(g, xBase + (horiz ? i * offset : 0) + this.legend._sampleSize + 5,
				yBase + (horiz ? 0 : i * offset), series._name, this.legend._textSettings);
		}
	},

	/* Show the current value status on hover. */
	_showStatus: function(elem, label, value) {
		var status = this._onstatus;
		if (this._onstatus) {
			$(elem).hover(function() { status.apply(this, [label, value]); },
				function() { status.apply(this, ['', 0]); });
		}
	}
});

/* Details about each graph series.
   @param  graph        (SVGGraph) the owning graph
   @param  name         (string) the name of this series (optional)
   @param  values       (number[]) the list of values to be plotted
   @param  fill         (string) how the series should be displayed
   @param  stroke       (string) the colour of the (out)line for the series (optional)
   @param  strokeWidth  (number) the width of the (out)line for the series (optional)
   @param  settings     (object) additional formatting settings (optional)
   @return  (SVGGraphSeries) the new series object */
function SVGGraphSeries(graph, name, values, fill, stroke, strokeWidth, settings) {
	if (typeof name != 'string') {
		settings = strokeWidth;
		strokeWidth = stroke;
		stroke = fill;
		fill = values;
		values = name;
		name = null;
	}
	if (typeof stroke != 'string') {
		settings = strokeWidth;
		strokeWidth = stroke;
		stroke = null;
	}
	if (typeof strokeWidth != 'number') {
		settings = strokeWidth;
		strokeWidth = null;
	}
	this._graph = graph; // The owning graph
	this._name = name || ''; // The name of this series
	this._values = values || []; // The list of values for this series
	this._axis = 1; // Which axis this series applies to: 1 = primary, 2 = secondary
	this._fill = fill || 'green'; // How the series is plotted
	this._stroke = stroke || 'black'; // The colour for the (out)line
	this._strokeWidth = strokeWidth || 1; // The (out)line width
	this._settings = settings || {}; // Additional formatting settings for the series
}

$.extend(SVGGraphSeries.prototype, {

	/* Set or retrieve the name for this series.
	   @param  name    (string) the series' name
	   @return  (SVGGraphSeries) this series object or
	            (string) the series name (if no parameters) */
	name: function(name) {
		if (arguments.length == 0) {
			return this._name;
		}
		this._name = name;
		this._graph._drawGraph();
		return this;
	},

	/* Set or retrieve the values for this series.
	   @param  name    (string) the series' name (optional)
	   @param  values  (number[]) the values to be graphed
	   @return  (SVGGraphSeries) this series object or
	            (number[]) the series values (if no parameters) */
	values: function(name, values) {
		if (arguments.length == 0) {
			return this._values;
		}
		if (isArray(name)) {
			values = name;
			name = null;
		}
		this._name = name || this._name;
		this._values = values;
		this._graph._drawGraph();
		return this;
	},

	/* Set or retrieve the formatting for this series.
	   @param  fill         (string) how the values are filled when plotted
	   @param  stroke       (string) the (out)line colour (optional)
	   @param  strokeWidth  (number) the line's width (optional)
	   @param  settings     (object) additional formatting settings for the series (optional)
	   @return  (SVGGraphSeries) this series object or
	            (object) formatting settings (if no parameters) */
	format: function(fill, stroke, strokeWidth, settings) {
		if (arguments.length == 0) {
			return $.extend({fill: this._fill, stroke: this._stroke,
				strokeWidth: this._strokeWidth}, this._settings);
		}
		if (typeof stroke != 'string') {
			settings = strokeWidth;
			strokeWidth = stroke;
			stroke = null;
		}
		if (typeof strokeWidth != 'number') {
			settings = strokeWidth;
			strokeWidth = null;
		}
		this._fill = fill || this._fill;
		this._stroke = stroke || this._stroke;
		this._strokeWidth = strokeWidth || this._strokeWidth;
		$.extend(this._settings, settings || {});
		this._graph._drawGraph();
		return this;
	},

	/* Return to the parent graph. */
	end: function() {
		return this._graph;
	}
});

/* Details about each graph axis.
   @param  graph  (SVGGraph) the owning graph
   @param  title  (string) the title of the axis
   @param  min    (number) the minimum value displayed on this axis
   @param  max    (number) the maximum value displayed on this axis
   @param  major  (number) the distance between major ticks
   @param  minor  (number) the distance between minor ticks (optional)
   @return  (SVGGraphAxis) the new axis object */
function SVGGraphAxis(graph, title, min, max, major, minor) {
	this._graph = graph; // The owning graph
	this._title = title || ''; // Title of this axis
	this._titleFormat = {}; // Formatting settings for the title
	this._titleOffset = 0; // The offset for positioning the title
	this._labels = null; // List of labels for this axis - one per possible value across all series
	this._labelFormat = {}; // Formatting settings for the labels
	this._lineFormat = {stroke: 'black', strokeWidth: 1}; // Formatting settings for the axis lines
	this._ticks = {major: major || 10, minor: minor || 0, size: 10, position: 'out'}; // Tick mark options
	this._scale = {min: min || 0, max: max || 100}; // Axis scale settings
	this._crossAt = 0; // Where this axis crosses the other one
}

$.extend(SVGGraphAxis.prototype, {

	/* Set or retrieve the scale for this axis.
	   @param  min  (number) the minimum value shown
	   @param  max  (number) the maximum value shown
	   @return  (SVGGraphAxis) this axis object or
	            (object) min and max values (if no parameters) */
	scale: function(min, max) {
		if (arguments.length == 0) {
			return this._scale;
		}
		this._scale.min = min;
		this._scale.max = max;
		this._graph._drawGraph();
		return this;
	},

	/* Set or retrieve the ticks for this axis.
	   @param  major     (number) the distance between major ticks
	   @param  minor     (number) the distance between minor ticks
	   @param  size      (number) the length of the major ticks (minor are half) (optional)
	   @param  position  (string) the location of the ticks:
	                     'in', 'out', 'both' (optional)
	   @return  (SVGGraphAxis) this axis object or
	            (object) major, minor, size, and position values (if no parameters) */
	ticks: function(major, minor, size, position) {
		if (arguments.length == 0) {
			return this._ticks;
		}
		if (typeof size == 'string') {
			position = size;
			size = null;
		}
		this._ticks.major = major;
		this._ticks.minor = minor;
		this._ticks.size = size || this._ticks.size;
		this._ticks.position = position || this._ticks.position;
		this._graph._drawGraph();
		return this;
	},

	/* Set or retrieve the title for this axis.
	   @param  title   (string) the title text
	   @param  offset  (number) the distance to offset the title position (optional)
	   @param  colour  (string) how to colour the title (optional) 
	   @param  format  (object) formatting settings for the title (optional)
	   @return  (SVGGraphAxis) this axis object or
	            (object) title, offset, and format values (if no parameters) */
	title: function(title, offset, colour, format) {
		if (arguments.length == 0) {
			return {title: this._title, offset: this._titleOffset, format: this._titleFormat};
		}
		if (typeof offset != 'number') {
			format = colour;
			colour = offset;
			offset = null;
		}
		if (typeof colour != 'string') {
			format = colour;
			colour = null;
		}
		this._title = title;
		this._titleOffset = (offset != null ? offset : this._titleOffset);
		if (colour || format) {
			this._titleFormat = $.extend(format || {}, (colour ? {fill: colour} : {}));
		}
		this._graph._drawGraph();
		return this;
	},

	/* Set or retrieve the labels for this axis.
	   @param  labels  (string[]) the text for each entry
	   @param  colour  (string) how to colour the labels (optional) 
	   @param  format  (object) formatting settings for the labels (optional)
	   @return  (SVGGraphAxis) this axis object or
	            (object) labels and format values (if no parameters) */
	labels: function(labels, colour, format) {
		if (arguments.length == 0) {
			return {labels: this._labels, format: this._labelFormat};
		}
		if (typeof colour != 'string') {
			format = colour;
			colour = null;
		}
		this._labels = labels;
		if (colour || format) {
			this._labelFormat = $.extend(format || {}, (colour ? {fill: colour} : {}));
		}
		this._graph._drawGraph();
		return this;
	},

	/* Set or retrieve the line formatting for this axis.
	   @param  colour    (string) the line's colour
	   @param  width     (number) the line's width (optional)
	   @param  settings  (object) additional formatting settings for the line (optional)
	   @return  (SVGGraphAxis) this axis object or
	            (object) line formatting values (if no parameters) */
	line: function(colour, width, settings) {
		if (arguments.length == 0) {
			return this._lineFormat;
		}
		if (typeof width == 'object') {
			settings = width;
			width = null;
		}
		$.extend(this._lineFormat, {stroke: colour},
			(width ? {strokeWidth: width} : {}), settings || {});
		this._graph._drawGraph();
		return this;
	},

	/* Return to the parent graph. */
	end: function() {
		return this._graph;
	}
});

/* Details about the graph legend.
   @param  graph         (SVGGraph) the owning graph
   @param  bgSettings    (object) additional formatting settings for the legend background (optional)
   @param  textSettings  (object) additional formatting settings for the legend text (optional)
   @return  (SVGGraphLegend) the new legend object */
function SVGGraphLegend(graph, bgSettings, textSettings) {
	this._graph = graph; // The owning graph
	this._show = true; // Show the legend?
	this._area = [0.9, 0.1, 1.0, 0.9]; // The legend area: left, top, right, bottom,
		// > 1 in pixels, <= 1 as proportion
	this._sampleSize = 15; // Size of sample box
	this._bgSettings = bgSettings || {stroke: 'gray'}; // Additional formatting settings for the legend background
	this._textSettings = textSettings || {}; // Additional formatting settings for the text
}

$.extend(SVGGraphLegend.prototype, {

	/* Set or retrieve whether the legend should be shown.
	   @param  show  (boolean) true to display it, false to hide it
	   @return  (SVGGraphLegend) this legend object or
	            (boolean) show the legend? (if no parameters) */
	show: function(show) {
		if (arguments.length == 0) {
			return this._show;
		}
		this._show = show;
		this._graph._drawGraph();
		return this;
	},

	/* Set or retrieve the legend area.
	   @param  left    (number) > 1 is pixels, <= 1 is proportion of width or
	                   (number[4]) for left, top, right, bottom
	   @param  top     (number) > 1 is pixels, <= 1 is proportion of height
	   @param  right   (number) > 1 is pixels, <= 1 is proportion of width
	   @param  bottom  (number) > 1 is pixels, <= 1 is proportion of height
	   @return  (SVGGraphLegend) this legend object or
	            (number[4]) the legend area: left, top, right, bottom (if no parameters) */
	area: function(left, top, right, bottom) {
		if (arguments.length == 0) {
			return this._area;
		}
		this._area = (isArray(left) ? left : [left, top, right, bottom]);
		this._graph._drawGraph();
		return this;
	},

	/* Set or retrieve additional settings for the legend area.
	   @param  sampleSize    (number) the size of the sample box to display (optional)
	   @param  bgSettings    (object) additional formatting settings for the legend background
	   @param  textSettings  (object) additional formatting settings for the legend text (optional)
	   @return  (SVGGraphLegend) this legend object or
	            (object) bgSettings and textSettings for the legend (if no parameters) */
	settings: function(sampleSize, bgSettings, textSettings) {
		if (arguments.length == 0) {
			return {sampleSize: this._sampleSize, bgSettings: this._bgSettings,
				textSettings: this._textSettings};
		}
		if (typeof sampleSize != 'number') {
			textSettings = bgSettings;
			bgSettings = sampleSize;
			sampleSize = null;
		}
		this._sampleSize = sampleSize || this._sampleSize;
		this._bgSettings = bgSettings;
		this._textSettings = textSettings || this._textSettings;
		this._graph._drawGraph();
		return this;
	},

	/* Return to the parent graph. */
	end: function() {
		return this._graph;
	}
});

//==============================================================================

/* Round a number to a given number of decimal points. */
function roundNumber(num, dec) {
	return Math.round(num * Math.pow(10, dec)) / Math.pow(10, dec);
}

var barOptions = ['barWidth (number) - the width of each bar',
	'barGap (number) - the gap between sets of bars'];

//------------------------------------------------------------------------------

/* Draw a standard grouped column bar chart. */
function SVGColumnChart() {
}

$.extend(SVGColumnChart.prototype, {

	/* Retrieve the display title for this chart type.
	   @return  the title */
	title: function() {
		return 'Basic column chart';
	},

	/* Retrieve a description of this chart type.
	   @return  its description */
	description: function() {
		return 'Compare sets of values as vertical bars with grouped categories.';
	},

	/* Retrieve a list of the options that may be set for this chart type.
	   @return  options list */
	options: function() {
		return barOptions;
	},

	/* Actually draw the graph in this type's style.
	   @param  graph  (object) the SVGGraph object */
	drawGraph: function(graph) {
		graph._drawChartBackground(true);
		var barWidth = graph._chartOptions.barWidth || 10;
		var barGap = graph._chartOptions.barGap || 10;
		var numSer = graph._series.length;
		var numVal = (numSer ? (graph._series[0])._values.length : 0);
		var dims = graph._getDims();
		var xScale = dims[graph.W] / ((numSer * barWidth + barGap) * numVal + barGap);
		var yScale = dims[graph.H] / (graph.yAxis._scale.max - graph.yAxis._scale.min);
		this._chart = graph._wrapper.group(graph._chartCont, {class_: 'chart'});
		for (var i = 0; i < numSer; i++) {
			this._drawSeries(graph, i, numSer, barWidth, barGap, dims, xScale, yScale);
		}
		graph._drawTitle();
		graph._drawAxes(true);
		this._drawXAxis(graph, numSer, numVal, barWidth, barGap, dims, xScale);
		graph._drawLegend();
	},

	/* Plot an individual series. */
	_drawSeries: function(graph, cur, numSer, barWidth, barGap, dims, xScale, yScale) {
		var series = graph._series[cur];
		var g = graph._wrapper.group(this._chart,
			$.extend({class_: 'series' + cur, fill: series._fill, stroke: series._stroke,
			strokeWidth: series._strokeWidth}, series._settings || {}));
		for (var i = 0; i < series._values.length; i++) {
			var r = graph._wrapper.rect(g,
				dims[graph.X] + xScale * (barGap + i * (numSer * barWidth + barGap) + (cur * barWidth)),
				dims[graph.Y] + yScale * (graph.yAxis._scale.max - series._values[i]),
				xScale * barWidth, yScale * series._values[i]);
			graph._showStatus(r, series._name, series._values[i]);
		}
	},

	/* Draw the x-axis and its ticks. */
	_drawXAxis: function(graph, numSer, numVal, barWidth, barGap, dims, xScale) {
		var axis = graph.xAxis;
		if (axis._title) {
			graph._wrapper.text(graph._chartCont, dims[graph.X] + dims[graph.W] / 2,
				dims[graph.Y] + dims[graph.H] + axis._titleOffset,
				axis._title, $.extend({textAnchor: 'middle'}, axis._titleFormat || {}));
		}
		var gl = graph._wrapper.group(graph._chartCont, $.extend({class_: 'xAxis'}, axis._lineFormat));
		var gt = graph._wrapper.group(graph._chartCont, $.extend({class_: 'xAxisLabels',
			textAnchor: 'middle'}, axis._labelFormat));
		graph._wrapper.line(gl, dims[graph.X], dims[graph.Y] + dims[graph.H],
			dims[graph.X] + dims[graph.W], dims[graph.Y] + dims[graph.H]);
		if (axis._ticks.major) {
			var offsets = graph._getTickOffsets(axis, true);
			for (var i = 1; i < numVal; i++) {
				var x = dims[graph.X] + xScale * (barGap / 2 + i * (numSer * barWidth + barGap));
				graph._wrapper.line(gl, x, dims[graph.Y] + dims[graph.H] + offsets[0] * axis._ticks.size,
					x, dims[graph.Y] + dims[graph.H] + offsets[1] * axis._ticks.size);
			}
			for (var i = 0; i < numVal; i++) {
				var x = dims[graph.X] + xScale * (barGap / 2 + (i + 0.5) * (numSer * barWidth + barGap));
				graph._wrapper.text(gt, x, dims[graph.Y] + dims[graph.H] + 2 * axis._ticks.size,
					(axis._labels ? axis._labels[i] : '' + i));
			}
		}
	}
});

//------------------------------------------------------------------------------

/* Draw a stacked column bar chart. */
function SVGStackedColumnChart() {
}

$.extend(SVGStackedColumnChart.prototype, {

	/* Retrieve the display title for this chart type.
	   @return  the title */
	title: function() {
		return 'Stacked column chart';
	},

	/* Retrieve a description of this chart type.
	   @return  its description */
	description: function() {
		return 'Compare sets of values as vertical bars showing ' +
			'relative contributions to the whole for each category.';
	},

	/* Retrieve a list of the options that may be set for this chart type.
	   @return  options list */
	options: function() {
		return barOptions;
	},

	/* Actually draw the graph in this type's style.
	   @param  graph  (object) the SVGGraph object */
	drawGraph: function(graph) {
		var bg = graph._drawChartBackground(true, true);
		var dims = graph._getDims();
		if (graph._gridlines[0] && graph.xAxis._ticks.major) {
			graph._drawGridlines(bg, graph._getPercentageAxis(), true, dims, graph._gridlines[0]);
		}
		var barWidth = graph._chartOptions.barWidth || 10;
		var barGap = graph._chartOptions.barGap || 10;
		var numSer = graph._series.length;
		var numVal = (numSer ? (graph._series[0])._values.length : 0);
		var xScale = dims[graph.W] / ((barWidth + barGap) * numVal + barGap);
		var yScale = dims[graph.H];
		this._chart = graph._wrapper.group(graph._chartCont, {class_: 'chart'});
		this._drawColumns(graph, numSer, numVal, barWidth, barGap, dims, xScale, yScale);
		graph._drawTitle();
		graph._wrapper.text(graph._chartCont, 0, 0, $.svg.graphing.region.percentageText,
			$.extend({textAnchor: 'middle', transform: 'translate(' +
			(dims[graph.X] - graph.yAxis._titleOffset) + ',' +
			(dims[graph.Y] + dims[graph.H] / 2) + ') rotate(-90)'}, graph.yAxis._titleFormat || {}));
		var pAxis = $.extend({}, graph._getPercentageAxis());
		$.extend(pAxis._labelFormat, graph.yAxis._labelFormat || {});
		graph._drawAxis(pAxis, 'yAxis', dims[graph.X], dims[graph.Y],
			dims[graph.X], dims[graph.Y] + dims[graph.H]);
		this._drawXAxis(graph, numVal, barWidth, barGap, dims, xScale);
		graph._drawLegend();
	},

	/* Plot all of the columns. */
	_drawColumns: function(graph, numSer, numVal, barWidth, barGap, dims, xScale, yScale) {
		var totals = graph._getTotals();
		var accum = [];
		for (var i = 0; i < numVal; i++) {
			accum[i] = 0;
		}
		for (var s = 0; s < numSer; s++) {
			var series = graph._series[s];
			var g = graph._wrapper.group(this._chart,
				$.extend({class_: 'series' + s, fill: series._fill,
				stroke: series._stroke, strokeWidth: series._strokeWidth},
				series._settings || {}));
			for (var i = 0; i < series._values.length; i++) {
				accum[i] += series._values[i];
				var r = graph._wrapper.rect(g,
					dims[graph.X] + xScale * (barGap + i * (barWidth + barGap)),
					dims[graph.Y] + yScale * (totals[i] - accum[i]) / totals[i],
					xScale * barWidth, yScale * series._values[i] / totals[i]);
				graph._showStatus(r, series._name,
					roundNumber(series._values[i] / totals[i] * 100, 2));
			}
		}
	},

	/* Draw the x-axis and its ticks. */
	_drawXAxis: function(graph, numVal, barWidth, barGap, dims, xScale) {
		var axis = graph.xAxis;
		if (axis._title) {
			graph._wrapper.text(graph._chartCont, dims[graph.X] + dims[graph.W] / 2,
				dims[graph.Y] + dims[graph.H] + axis._titleOffset,
				axis._title, $.extend({textAnchor: 'middle'}, axis._titleFormat || {}));
		}
		var gl = graph._wrapper.group(graph._chartCont, $.extend({class_: 'xAxis'}, axis._lineFormat));
		var gt = graph._wrapper.group(graph._chartCont, $.extend({class_: 'xAxisLabels',
			textAnchor: 'middle'}, axis._labelFormat));
		graph._wrapper.line(gl, dims[graph.X], dims[graph.Y] + dims[graph.H],
		dims[graph.X] + dims[graph.W], dims[graph.Y] + dims[graph.H]);
		if (axis._ticks.major) {
			var offsets = graph._getTickOffsets(axis, true);
			for (var i = 1; i < numVal; i++) {
				var x = dims[graph.X] + xScale * (barGap / 2 + i * (barWidth + barGap));
				graph._wrapper.line(gl, x, dims[graph.Y] + dims[graph.H] + offsets[0] * axis._ticks.size,
					x, dims[graph.Y] + dims[graph.H] + offsets[1] * axis._ticks.size);
			}
			for (var i = 0; i < numVal; i++) {
				var x = dims[graph.X] + xScale * (barGap / 2 + (i + 0.5) * (barWidth + barGap));
				graph._wrapper.text(gt, x, dims[graph.Y] + dims[graph.H] + 2 * axis._ticks.size,
					(axis._labels ? axis._labels[i] : '' + i));
			}
		}
	}
});

//------------------------------------------------------------------------------

/* Draw a standard grouped row bar chart. */
function SVGRowChart() {
}

$.extend(SVGRowChart.prototype, {

	/* Retrieve the display title for this chart type.
	   @return  the title */
	title: function() {
		return 'Basic row chart';
	},

	/* Retrieve a description of this chart type.
	   @return  its description */
	description: function() {
		return 'Compare sets of values as horizontal rows with grouped categories.';
	},

	/* Retrieve a list of the options that may be set for this chart type.
	   @return  options list */
	options: function() {
		return barOptions;
	},

	/* Actually draw the graph in this type's style.
	   @param  graph  (object) the SVGGraph object */
	drawGraph: function(graph) {
		var bg = graph._drawChartBackground(true, true);
		var dims = graph._getDims();
		graph._drawGridlines(bg, graph.yAxis, false, dims, graph._gridlines[0]);
		var barWidth = graph._chartOptions.barWidth || 10;
		var barGap = graph._chartOptions.barGap || 10;
		var numSer = graph._series.length;
		var numVal = (numSer ? (graph._series[0])._values.length : 0);
		var xScale = dims[graph.W] / (graph.yAxis._scale.max - graph.yAxis._scale.min);
		var yScale = dims[graph.H] / ((numSer * barWidth + barGap) * numVal + barGap);
		this._chart = graph._wrapper.group(graph._chartCont, {class_: 'chart'});
		for (var i = 0; i < numSer; i++) {
			this._drawSeries(graph, i, numSer, barWidth, barGap, dims, xScale, yScale);
		}
		graph._drawTitle();
		this._drawAxes(graph, numSer, numVal, barWidth, barGap, dims, yScale);
		graph._drawLegend();
	},

	/* Plot an individual series. */
	_drawSeries: function(graph, cur, numSer, barWidth, barGap, dims, xScale, yScale) {
		var series = graph._series[cur];
		var g = graph._wrapper.group(this._chart,
			$.extend({class_: 'series' + cur, fill: series._fill,
			stroke: series._stroke, strokeWidth: series._strokeWidth},
			series._settings || {}));
		for (var i = 0; i < series._values.length; i++) {
			var r = graph._wrapper.rect(g,
				dims[graph.X] + xScale * (0 - graph.yAxis._scale.min),
				dims[graph.Y] + yScale * (barGap + i * (numSer * barWidth + barGap) + (cur * barWidth)),
				xScale * series._values[i], yScale * barWidth);
			graph._showStatus(r, series._name, series._values[i]);
		}
	},

	/* Draw the axes for this graph. */
	_drawAxes: function(graph, numSer, numVal, barWidth, barGap, dims, yScale) {
		// X-axis
		var axis = graph.yAxis;
		if (axis) {
			if (axis._title) {
				graph._wrapper.text(graph._chartCont, dims[graph.X] + dims[graph.W] / 2,
					dims[graph.Y] + dims[graph.H] + axis._titleOffset, axis._title, axis._titleFormat);
			}
			graph._drawAxis(axis, 'xAxis', dims[graph.X], dims[graph.Y] + dims[graph.H],
				dims[graph.X] + dims[graph.W], dims[graph.Y] + dims[graph.H]);
		}
		// Y-axis
		var axis = graph.xAxis;
		if (axis._title) {
			graph._wrapper.text(graph._chartCont, 0, 0, axis._title, $.extend({textAnchor: 'middle',
				transform: 'translate(' + (dims[graph.X] - axis._titleOffset) + ',' +
				(dims[graph.Y] + dims[graph.H] / 2) + ') rotate(-90)'}, axis._titleFormat || {}));
		}
		var gl = graph._wrapper.group(graph._chartCont, $.extend({class_: 'yAxis'}, axis._lineFormat));
		var gt = graph._wrapper.group(graph._chartCont, $.extend(
			{class_: 'yAxisLabels', textAnchor: 'end'}, axis._labelFormat));
		graph._wrapper.line(gl, dims[graph.X], dims[graph.Y], dims[graph.X], dims[graph.Y] + dims[graph.H]);
		if (axis._ticks.major) {
			var offsets = graph._getTickOffsets(axis, false);
			for (var i = 1; i < numVal; i++) {
				var y = dims[graph.Y] + yScale * (barGap / 2 + i * (numSer * barWidth + barGap));
				graph._wrapper.line(gl, dims[graph.X] + offsets[0] * axis._ticks.size, y,
					dims[graph.X] + offsets[1] * axis._ticks.size, y);
			}
			for (var i = 0; i < numVal; i++) {
				var y = dims[graph.Y] + yScale * (barGap / 2 + (i + 0.5) * (numSer * barWidth + barGap));
				graph._wrapper.text(gt, dims[graph.X] - axis._ticks.size, y,
					(axis._labels ? axis._labels[i] : '' + i));
			}
		}
	}
});

//------------------------------------------------------------------------------

/* Draw a stacked row bar chart. */
function SVGStackedRowChart() {
}

$.extend(SVGStackedRowChart.prototype, {

	/* Retrieve the display title for this chart type.
	   @return  the title */
	title: function() {
		return 'Stacked row chart';
	},

	/* Retrieve a description of this chart type.
	   @return  its description */
	description: function() {
		return 'Compare sets of values as horizontal bars showing ' +
			'relative contributions to the whole for each category.';
	},

	/* Retrieve a list of the options that may be set for this chart type.
	   @return  options list */
	options: function() {
		return barOptions;
	},

	/* Actually draw the graph in this type's style.
	   @param  graph  (object) the SVGGraph object */
	drawGraph: function(graph) {
		var bg = graph._drawChartBackground(true, true);
		var dims = graph._getDims();
		if (graph._gridlines[0] && graph.xAxis._ticks.major) {
			graph._drawGridlines(bg, graph._getPercentageAxis(), false, dims, graph._gridlines[0]);
		}
		var barWidth = graph._chartOptions.barWidth || 10;
		var barGap = graph._chartOptions.barGap || 10;
		var numSer = graph._series.length;
		var numVal = (numSer ? (graph._series[0])._values.length : 0);
		var xScale = dims[graph.W];
		var yScale = dims[graph.H] / ((barWidth + barGap) * numVal + barGap);
		this._chart = graph._wrapper.group(graph._chartCont, {class_: 'chart'});
		this._drawRows(graph, numSer, numVal, barWidth, barGap, dims, xScale, yScale);
		graph._drawTitle();
		graph._wrapper.text(graph._chartCont, dims[graph.X] + dims[graph.W] / 2,
			dims[graph.Y] + dims[graph.H] + graph.xAxis._titleOffset,
			$.svg.graphing.region.percentageText,
			$.extend({textAnchor: 'middle'}, graph.yAxis._titleFormat || {}));
		var pAxis = $.extend({}, graph._getPercentageAxis());
		$.extend(pAxis._labelFormat, graph.yAxis._labelFormat || {});
		graph._drawAxis(pAxis, 'xAxis', dims[graph.X], dims[graph.Y] + dims[graph.H],
			dims[graph.X] + dims[graph.W], dims[graph.Y] + dims[graph.H]);
		this._drawYAxis(graph, numVal, barWidth, barGap, dims, yScale);
		graph._drawLegend();
	},

	/* Plot all of the rows. */
	_drawRows: function(graph, numSer, numVal, barWidth, barGap, dims, xScale, yScale) {
		var totals = graph._getTotals();
		var accum = [];
		for (var i = 0; i < numVal; i++) {
			accum[i] = 0;
		}
		for (var s = 0; s < numSer; s++) {
			var series = graph._series[s];
			var g = graph._wrapper.group(this._chart,
				$.extend({class_: 'series' + s, fill: series._fill,
				stroke: series._stroke, strokeWidth: series._strokeWidth},
				series._settings || {}));
			for (var i = 0; i < series._values.length; i++) {
				var r = graph._wrapper.rect(g,
					dims[graph.X] + xScale * accum[i] / totals[i],
					dims[graph.Y] + yScale * (barGap + i * (barWidth + barGap)),
					xScale * series._values[i] / totals[i], yScale * barWidth);
				graph._showStatus(r, series._name,
					roundNumber(series._values[i] / totals[i] * 100, 2));
				accum[i] += series._values[i];
			}
		}
	},

	/* Draw the y-axis and its ticks. */
	_drawYAxis: function(graph, numVal, barWidth, barGap, dims, yScale) {
		var axis = graph.xAxis;
		if (axis._title) {
			graph._wrapper.text(graph._chartCont, 0, 0, axis._title, $.extend({textAnchor: 'middle',
				transform: 'translate(' + (dims[graph.X] - axis._titleOffset) + ',' +
				(dims[graph.Y] + dims[graph.H] / 2) + ') rotate(-90)'}, axis._titleFormat || {}));
		}
		var gl = graph._wrapper.group(graph._chartCont,
			$.extend({class_: 'yAxis'}, axis._lineFormat));
		var gt = graph._wrapper.group(graph._chartCont,
			$.extend({class_: 'yAxisLabels', textAnchor: 'end'}, axis._labelFormat));
		graph._wrapper.line(gl, dims[graph.X], dims[graph.Y],
			dims[graph.X], dims[graph.Y] + dims[graph.H]);
		if (axis._ticks.major) {
			var offsets = graph._getTickOffsets(axis, false);
			for (var i = 1; i < numVal; i++) {
				var y = dims[graph.Y] + yScale * (barGap / 2 + i * (barWidth + barGap));
				graph._wrapper.line(gl, dims[graph.X] + offsets[0] * axis._ticks.size, y,
					dims[graph.X] + offsets[1] * axis._ticks.size, y);
			}
			for (var i = 0; i < numVal; i++) {
				var y = dims[graph.Y] + yScale * (barGap / 2 + (i + 0.5) * (barWidth + barGap));
				graph._wrapper.text(gt, dims[graph.X] - axis._ticks.size, y,
					(axis._labels ? axis._labels[i] : '' + i));
			}
		}
	}
});

//------------------------------------------------------------------------------

/* Draw a standard line chart. */
function SVGLineChart() {
}

$.extend(SVGLineChart.prototype, {

	/* Retrieve the display title for this chart type.
	   @return  the title */
	title: function() {
		return 'Basic line chart';
	},

	/* Retrieve a description of this chart type.
	   @return  its description */
	description: function() {
		return 'Compare sets of values as continuous lines.';
	},

	/* Retrieve a list of the options that may be set for this chart type.
	   @return  options list */
	options: function() {
		return [];
	},
	
	/* Actually draw the graph in this type's style.
	   @param  graph  (object) the SVGGraph object */
	drawGraph: function(graph) {
		graph._drawChartBackground();
		var dims = graph._getDims();
		var xScale = dims[graph.W] / (graph.xAxis._scale.max - graph.xAxis._scale.min);
		var yScale = dims[graph.H] / (graph.yAxis._scale.max - graph.yAxis._scale.min);
		this._chart = graph._wrapper.group(graph._chartCont, {class_: 'chart'});
		for (var i = 0; i < graph._series.length; i++) {
			this._drawSeries(graph, i, dims, xScale, yScale);
		}
		graph._drawTitle();
		graph._drawAxes();
		graph._drawLegend();
	},

	/* Plot an individual series. */
	_drawSeries: function(graph, cur, dims, xScale, yScale) {
		var series = graph._series[cur];
		var path = graph._wrapper.createPath();
		for (var i = 0; i < series._values.length; i++) {
			var x = dims[graph.X] + i * xScale;
			var y = dims[graph.Y] + (graph.yAxis._scale.max - series._values[i]) * yScale;
			if (i == 0) {
				path.move(x, y);
			}
			else {
				path.line(x, y);
			}
		}
		var p = graph._wrapper.path(this._chart, path,
			$.extend({id: 'series' + cur, fill: 'none', stroke: series._stroke,
			strokeWidth: series._strokeWidth}, series._settings || {}));
		graph._showStatus(p, series._name, 0);
	}
});

//------------------------------------------------------------------------------

/* Draw a standard pie chart. */
function SVGPieChart() {
}

$.extend(SVGPieChart.prototype, {

	_options: ['explode (number or number[]) - indexes of sections to explode out of the pie',
		'explodeDist (number) - the distance to move an exploded section',
		'pieGap (number) - the distance between pies for multiple values'],

	/* Retrieve the display title for this chart type.
	   @return  the title */
	title: function() {
		return 'Pie chart';
	},

	/* Retrieve a description of this chart type.
	   @return  its description */
	description: function() {
		return 'Compare relative sizes of values as contributions to the whole.';
	},

	/* Retrieve a list of the options that may be set for this chart type.
	   @return  options list */
	options: function() {
		return this._options;
	},

	/* Actually draw the graph in this type's style.
	   @param  graph  (object) the SVGGraph object */
	drawGraph: function(graph) {
		graph._drawChartBackground(true, true);
		this._chart = graph._wrapper.group(graph._chartCont, {class_: 'chart'});
		var dims = graph._getDims();
		this._drawSeries(graph, dims);
		graph._drawTitle();
		graph._drawLegend();
	},

	/* Plot all the series. */
	_drawSeries: function(graph, dims) {
		var totals = graph._getTotals();
		var numSer = graph._series.length;
		var numVal = (numSer ? (graph._series[0])._values.length : 0);
		var path = graph._wrapper.createPath();
		var explode = graph._chartOptions.explode || [];
		explode = (isArray(explode) ? explode : [explode]);
		var explodeDist = graph._chartOptions.explodeDist || 10;
		var pieGap = (numVal <= 1 ? 0 : graph._chartOptions.pieGap || 10);
		var xBase = (dims[graph.W] - (numVal * pieGap) - pieGap) / numVal / 2;
		var yBase = dims[graph.H] / 2;
		var radius = Math.min(xBase, yBase) - (explode.length > 0 ? explodeDist : 0);
		var gt = graph._wrapper.group(graph._chartCont, $.extend(
			{class_: 'xAxisLabels', textAnchor: 'middle'}, graph.xAxis._labelFormat));
		var gl = [];
		for (var i = 0; i < numVal; i++) {
			var cx = dims[graph.X] + xBase + (i * (2 * Math.min(xBase, yBase) + pieGap)) + pieGap;
			var cy = dims[graph.Y] + yBase;
			var curTotal = 0;
			for (var j = 0; j < numSer; j++) {
				var series = graph._series[j];
				if (i == 0) {
					gl[j] = graph._wrapper.group(this._chart, $.extend({class_: 'series' + j,
						fill: series._fill, stroke: series._stroke,
						strokeWidth: series._strokeWidth}, series._settings || {}));
				}
				if (series._values[i] == 0) {
					continue;
				}
				var start = (curTotal / totals[i]) * 2 * Math.PI;
				curTotal += series._values[i];
				var end = (curTotal / totals[i]) * 2 * Math.PI;
				var exploding = false;
				for (var k = 0; k < explode.length; k++) {
					if (explode[k] == j) {
						exploding = true;
						break;
					}
				}
				var x = cx + (exploding ? explodeDist * Math.cos((start + end) / 2) : 0);
				var y = cy + (exploding ? explodeDist * Math.sin((start + end) / 2) : 0);
				var p = graph._wrapper.path(gl[j], path.reset().move(x, y).
					line(x + radius * Math.cos(start), y + radius * Math.sin(start)).
					arc(radius, radius, 0, (end - start < Math.PI ? 0 : 1), 1,
					x + radius * Math.cos(end), y + radius * Math.sin(end)).close());
				graph._showStatus(p, series._name,
					roundNumber((end - start) / 2 / Math.PI * 100, 2));
			}
			if (graph.xAxis) {
				graph._wrapper.text(gt, cx, dims[graph.Y] + dims[graph.H] + graph.xAxis._titleOffset,
					graph.xAxis._labels[i])
			}
		}
	}
});

//------------------------------------------------------------------------------

/* Determine whether an object is an array. */
function isArray(a) {
	return (a && a.constructor == Array);
}

// Basic chart types
$.svg.graphing.addChartType('column', new SVGColumnChart());
$.svg.graphing.addChartType('stackedColumn', new SVGStackedColumnChart());
$.svg.graphing.addChartType('row', new SVGRowChart());
$.svg.graphing.addChartType('stackedRow', new SVGStackedRowChart());
$.svg.graphing.addChartType('line', new SVGLineChart());
$.svg.graphing.addChartType('pie', new SVGPieChart());

})(jQuery)
