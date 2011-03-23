/*
**    Copyright (C) 2003-2008 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/

// setup namespace if not already defined
if(!org) {
    var org = {};
    if(!org.systemsbiology)
        org.systemsbiology = {};
    if(!org.systemsbiology.visualization)
        org.systemsbiology.visualization = {};
}


// ---------------------------------------------------------------------------------------------------------------------
// - BioHeatMap
// -
// -   Description: Draws a gene expression style heatmap using Canvas
// -   Author: dburdick
// -   Version: 1.0.1
// -
// ---------------------------------------------------------------------------------------------------------------------
org.systemsbiology.visualization.BioHeatMap = Class.create({

    // --------------------------------------------------------
    // PUBLIC METHODS
    // --------------------------------------------------------
    initialize: function(container) {
        this.containerElement = container;

        // private variables
        //  - defaults
        this._font = "sans";
        this._useRowNames = true;
        //this._minimumFontHeight = 7;
        this._fontHeight = 10;
        this._cellSpacing = 0; // NOT SUPPORTED YET!
        this._cellWidth = 15;
        this._cellHeight = 15;
        this._canvasWidth = null;
        this._canvasHeight = null;
        this._cellBorder = false;
        this._drawHeatmapBorder = true;
        this._fixedCanvasSize = false;

        // color defaults
        this._maxColors = 64; // number of colors
        this._backgroundColor = {r:0, g:0, b:0, a:1};
        this._maxColor = {r:255, g:0, b:0, a:1};
        this._minColor = {r:0, g:255, b:0, a:1};
        this._emptyDataColor = {r:100, g:100, b:100, a:1};
        this._passThroughBlack = true;

        this._verticalPadding = 10;
        this._horizontalPadding = 10;
        this._columnLabelBottomPadding = 3;
        this._rowLabelRightPadding = 3;

        // tooltip support
        this._displayCellTooltips = true;
        this._tooltipDelay = 200;
        this.tooltipElement = null;

        // events
        this._selected = []; // list of selections
        this._selectedState = false;

        // - calculated
        this._columnLabelHeight = 0; // how tall is the tallest column label
        this._rowLabelWidth = 0; // how wide is the widest row label
        this._heatMapHeight = 0;
        this._heatMapWidth = 0;
        this._heatMapTopLeftPoint = {x:0, y:0};
        this._heatMapBottomRightPoint = {x:0, y:0};
        this._numDataColumns = 0;
        this._numColumns = 0;
        this._numRows = 0;
        this._dataRange = {min:null, max:null};
        this._colorStep = {r:null, g:null, b:null, a:null};

        // this.ctx - the canvas context object
        this._debug = false;
        this._drawError = false;
        this.data = null;
        this.options = null;
        this.canvas = null;
    },

    // Main drawing logic.
    // Parameter data is of type google.visualization.DataTable.
    // Parameter options is a name/value map of options.
    draw: function(data, max, min, options) {
    	options = options || {};
    	this.data = data;
        this.options = options;
        this._setupCanvas();
        this._setupTooltipElement(options);
        var canvas = this.canvas;
        var ctx = this.ctx;

        if (ctx) {
            CanvasTextFunctions.enable(ctx); // this adds text functions to the ctx
            this._clearCanvas(ctx, canvas);
            this._setOptionDefaults(options);
            this._calcDataRange(data, max, min); // calc min/max of data

            // create color range object
            this._discreteColorRange = new org.systemsbiology.visualization.DiscreteColorRange(this._maxColors,this._dataRange,{maxColor: this._maxColor,
                                                                                                          minColor: this._minColor,
                                                                                                          emptyDataColor: this._emptyDataColor,
                                                                                                          passThroughBlack: this._passThroughBlack});

            // Calculate default positions and lengths
            if (this._fixedCanvasSize) {
                this._setDefaultsByHeatMapSize(data, options);
            } else {
                this._setDefaultsByCellSize(data, options);
            }
            this._heatMapTopLeftPoint.x = this._horizontalPadding;
            this._heatMapTopLeftPoint.y = this._verticalPadding;
            this._heatMapBottomRightPoint.x = this._heatMapTopLeftPoint.x + this._heatMapWidth;
            this._heatMapBottomRightPoint.y = this._heatMapTopLeftPoint.y + this._heatMapHeight;

            // set canvas size and draw bounding box
            this.canvas.style.width = this._canvasWidth;  //IE
            this.canvas.style.height = this._canvasHeight;//IE
            this.canvas.setAttribute('width', this._canvasWidth); //FF
            this.canvas.setAttribute('height', this._canvasHeight);//FF

            if(this._drawHeatmapBorder)
                ctx.strokeRect(0, 0, canvas.width, canvas.height);
            var heatMap = this;
            this.canvas.onclick = this._getMouseXY(function(lp, mp) { heatMap._onClickEvent(lp, mp) }); // mouse click event handler
            this.canvas.onmousemove = this._getMouseXY(function(lp, mp) { heatMap._onMoveEvent(lp, mp) }); // mouse move event handler

            this._logCalculated(); // log some values to the console

            // Draw heatmap
            var colStartIndex = this._useRowNames ? 1 : 0;
            var rowNameIndex = 0;
            this._drawColumLabels(data, colStartIndex); // draw column names
            this._drawRowLabels(data, rowNameIndex);// draw row names if present
            for (var row = 0; row < this._numRows; row++) {
                for (var col = colStartIndex; col < data.getNumberOfColumns(); col++) {
                    this._drawHeatMapCell(data, row, col);
                }
            }

            // TODO : DRAW a legend
        }
    },

    // ------------------------------------
    // Google Eventing Methods
    // ------------------------------------

    // gets the current selected rows,cols or cells
    getSelection: function() {
        return this._selected;
    },

    // sets the current selected rows, cols or cells
    setSelection: function(selections) {
        var validSelections = [];
        this._clearSelection();
        if (selections && selections.length) {
            // put heatmap into selected state & save context
            this._selectedState = true;
            // TODO : figure out visuals saving:
            // this.ctx.save();

            // process each selection
            for (var i = 0; i < selections.length; i++) {
                var thisSelection = selections[i];
                // store canvas state before highlight
                if (thisSelection.row >= 0 || thisSelection.column >= 0) {
                    validSelections[validSelections.length || 0] = thisSelection;
                    if (thisSelection.row >= 0 && thisSelection.column >= 0) {
                        // TODO : draw selection box around cell
                        // this._drawHeatMapCellBorder(thisSelection.row,thisSelection.column);
                    }
                }
            }
        }
        this._selected = validSelections;
    },


    // --------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------
    // creates an IE/FF friendly instance of the canvas tag
    _setupCanvas: function() {
        this.containerElement.innerHTML = '';
        var canvasObj;
        try {
            canvasObj = document.createElement('<canvas>');
        } catch (e) {
        }
        if (!canvasObj || !canvasObj.name) { // Not in IE, then
            canvasObj = document.createElement('canvas')
        }
        this.containerElement.appendChild(canvasObj);

        if (canvasObj.getContext)
            this.canvas = canvasObj; // FF or safari
        else
            this.canvas = excanvas(canvasObj); // IE

        if (this.canvas.getContext) {
            this.ctx = this.canvas.getContext("2d");
        }
    },

    _setupTooltipElement: function(options) {
        this.tooltipElement = document.createElement('div');
        document.body.appendChild(this.tooltipElement);
        this.tooltipElement.style.display = 'none';
        this.tooltipElement.style.position = 'absolute';
        this.tooltipElement.className = options.tooltipClass || "bioHeatMapTooltip";
    },

    // ------------------------------------
    // Drawing Functions
    // ------------------------------------
    _clearCanvas: function(ctx, canvas) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    },

    _drawColumLabels: function(data, colStartIndex) {
        for (var col = colStartIndex; col < data.getNumberOfColumns(); col++) {
            var colName = data.getColumnLabel(col);
            var bottomLeftPoint = this._getCellXYTopLeft(0, col);
            bottomLeftPoint.y = bottomLeftPoint.y - this._columnLabelBottomPadding;
            var centerX = Math.round(this._cellWidth / 2 + (this._fontHeight / 2) - 1);
            this.drawVerticalColumnText(colName, this.ctx, bottomLeftPoint.x + centerX, bottomLeftPoint.y, this._font, this._fontHeight);
        }
    },

    _drawRowLabels: function(data, rowNameIndex) {
        if (this._useRowNames) {
            for (var row = 0; row < this._numRows; row++) {
                var rowName = this._getValueFormattedOrNot(row, rowNameIndex, data);
                var topLeftPoint = this._getCellXYTopLeft(row, rowNameIndex);
                topLeftPoint.x += this._rowLabelWidth - this._rowLabelRightPadding;
                topLeftPoint.y += this._cellHeight / 2 + this._fontHeight / 2;
                this.ctx.drawTextRight(this._font, this._fontHeight, topLeftPoint.x, topLeftPoint.y, rowName);
            }
        }
    },

    _drawHeatMapCell: function(data, row, col) {
        var cellValue = this._getValueFormattedOrNot(row, col, data);
        var topLeftPoint = this._getCellXYTopLeft(row, col);
        var fillString = this._discreteColorRange.getCellColorString(cellValue);
        this.ctx.fillStyle = fillString;
        this.ctx.fillRect(topLeftPoint.x, topLeftPoint.y, this._cellWidth, this._cellHeight);
        if (this._cellBorder) {
            this.ctx.strokeRect(topLeftPoint.x, topLeftPoint.y, this._cellWidth, this._cellHeight);
        }
    },

    _drawHeatMapCellBorder: function(row, col) {
        // todo : add color choice
        var topLeftPoint = this._getCellXYTopLeft(row, col);
        this.ctx.strokeRect(topLeftPoint.x, topLeftPoint.y, this._cellWidth, this._cellHeight);
    },

    // draw text rotated vertically at point x,y
    drawVerticalColumnText: function(columnText, ctx, x, y, font, fontSize, fontColor) {
        var ang = 270;
        if (!fontColor) {
            fontColor = 'black';
        }
        ctx.save();
        // relocate to draw spot
        ctx.translate(x, y);
        ctx.rotate(ang * 2 * Math.PI / 360); // rotate to vertical
        ctx.strokeStyle = fontColor;
        ctx.drawText(font, fontSize, 0, 0, columnText);
        ctx.restore();
    },


    // ------------------------------------
    // Data Manipulation and Color Methods
    // ------------------------------------

    // maps data ranges to colors
    _calcDataRange: function(data, max, min) {
        if (max != null && min != null) {
            this._dataRange.max = max;
            this._dataRange.min = min;
        } else {
            var dataRange = this._dataRange;
            // determine the data range if needed
            var colStartIndex = this._useRowNames ? 1 : 0; // skip row label
            if ((!this._dataRange.min && this._dataRange.min != 0) || (!this._dataRange.max && this._dataRange.max != 0)) {
                for (var col = colStartIndex; col < data.getNumberOfColumns(); col++) {
                    var colRange = data.getColumnRange(col);
                    if (dataRange.min > colRange.min)
                        dataRange.min = colRange.min;
                    if (dataRange.max < colRange.max)
                        dataRange.max = colRange.max;
                }
            }
        }
    },

    // ------------------------------------
    // Setup Helper Functions
    // ------------------------------------

    // sets default variables based on the options
    _setOptionDefaults: function(options) {
        // PUBLIC options
        if (options.noRowNames)
            this._useRowNames = false;
        if(options.startColor)
            this._minColor = options.startColor;
        if(options.endColor)
            this._maxColor = options.endColor;
        if(options.emptyDataColor)
            this._emptyDataColor = options.emptyDataColor;
        if(options.numberOfColors)
            this._maxColors = options.numberOfColors;

        if(options.passThroughBlack != null && options.passThroughBlack == false)
            this._passThroughBlack = false;
        else if (options.passThroughBlack == true)
            this._passThroughBlack = true;

        if(options.useRowLabels != null && options.useRowLabels == false)
            this._useRowNames = false;
        else if (options.useRowLabels == true)
            this._useRowNames = true;

        // height/width stuff
        if(options.cellWidth)
            this._cellWidth = options.cellWidth;
        if(options.cellHeight)
            this._cellHeight = options.cellHeight;
        if(options.mapWidth)
            this._canvasWidth = options.mapWidth;
        if(options.mapHeight)
            this._canvasHeight = options.mapHeight;
        if(options.mapHeight && options.mapWidth)
            this._fixedCanvasSize=true;
        if(options.fontHeight>0)
            this._fontHeight = options.fontHeight;

        // padding
        if (options.horizontalPadding)
            this._horizontalPadding = options.horizontalPadding;
        if (options.horizontalPadding)
            this._verticalPadding = options.horizontalPadding;
        if (options.cellBorder)
            this._cellBorder = options.cellBorder;

        if(options.drawBorder != null && options.drawBorder == false)
            this._drawHeatmapBorder = false;
        else if(options.drawBorder == true)
            this._drawHeatmapBorder = true;

        if(options.tooltipDelay)
          this._tooltipDelay = options.tooltipDelay
        if(options.displayCellTooltips != null && options.displayCellTooltips == false)
          this._displayCellTooltips = false;

        // TODO : more OPTIONAL PARAMETERS?
        // - Row normalize the data to average of 0 and variance +/-1?
    },

    // calculates default variables based on a specified cell size
    _setDefaultsByCellSize: function(data) {
        // set h/w if available. otherwise use defaults
        if (this.options.cellHeight) {
            this._cellHeight = this.options.cellHeight;
        }
        if (this.options.cellWidth) {
            this._cellWidth = this.options.cellWidth;
        }
        this._calcDataDefaults(data);
        this._calcMaxRowLabelWidth(data);
        this._calcMaxColumnLabelHeight(data);

        // calculate the w/h of the heatmap without row or column labels
        this._heatMapHeight = this._cellHeight * this._numRows;
        this._heatMapWidth = this._cellWidth * this._numDataColumns;

        // calculate the canvas's width/height
        this._canvasWidth = this._rowLabelWidth + this._heatMapWidth + (2 * this._horizontalPadding);
        this._canvasHeight = this._columnLabelHeight + this._heatMapHeight + (2 * this._verticalPadding);
        this._checkCellAndFontSizes();
    },

    // calculates default variables based on a specified heatmap size
    _setDefaultsByHeatMapSize: function(data) {
        // check for reasonable bounds, otherwise call default method
        if (!this._canvasHeight>0 || !this._canvasWidth>0)
            this._setDefaultsByCellSize(data, options);

        this._calcDataDefaults(data);
        this._calcMaxRowLabelWidth(data);
        this._calcMaxColumnLabelHeight(data);

        // calculate the width of the heatmap
        this._heatMapHeight = this._canvasHeight - this._columnLabelHeight - (2 * this._verticalPadding);
        this._heatMapWidth = this._canvasWidth - this._rowLabelWidth - (2 * this._horizontalPadding);

        // calculate the cell dimension
        var maxCellWidth = Math.floor(this._heatMapWidth / this._numDataColumns);
        var maxCellHeight = Math.floor(this._heatMapHeight / this._numRows);
        var cellDimension = 0;
        if (maxCellWidth < maxCellHeight) {
            cellDimension = maxCellWidth;
        } else {
            cellDimension = maxCellHeight;
        }
        this._cellWidth = cellDimension;
        this._cellHeight = cellDimension;

//        if (cellDimension < this._minimumFontHeight) {
//            this._displayError("Canvas size is too small to render properly.");
//        }
        this._checkCellAndFontSizes();
    },


    // checks to make sure font and cell sizes will play nice together on the screen
    _checkCellAndFontSizes: function() {
//        if (this._cellHeight < this._minimumFontHeight || this._cellWidth < this._minimumFontHeight) {
//            this._displayError("Cell size is too small to render properly.");
//        } else if (this._fontHeight > this._cellHeight || this._fontHeight > this._cellWidth) {
//            this._displayError("Font size is too large to render properly.");
//        }
    },


    // ------------------------------------
    // Calculation Helper Functions
    // ------------------------------------

    // sets the row/col counts and (future: set data ranges)
    _calcDataDefaults: function(data) {
        this._numRows = data.getNumberOfRows();
        this._numColumns = data.getNumberOfColumns();
        this._numDataColumns = data.getNumberOfColumns();
        if (this._useRowNames) {
            this._numDataColumns--;
        }
    },

    // determines the max width of the row labels
    _calcMaxRowLabelWidth: function(data) {
        var rowLabelIndex = 0;
        for (var row = 0; row < data.getNumberOfRows(); row++) {
            var rowName = this._getValueFormattedOrNot(row, rowLabelIndex, data);
            var rowNameWidth = this.ctx.measureText(this._font, this._fontHeight, rowName);
            if (rowNameWidth > this._rowLabelWidth) {
                this._rowLabelWidth = rowNameWidth;
            }
        }
        this._rowLabelWidth = Math.ceil(this._rowLabelWidth);
    },

    // determines the max height of the column labels
    _calcMaxColumnLabelHeight: function(data) {
        var colStartIndex = this._useRowNames ? 1 : 0;
        for (var col = colStartIndex; col < data.getNumberOfColumns(); col++) {
            var colName = data.getColumnLabel(col);
            var colNameWidth = this.ctx.measureText(this._font, this._fontHeight, colName);
            if (colNameWidth > this._columnLabelHeight) {
                this._columnLabelHeight = colNameWidth;
            }
        }
        this._columnLabelHeight = Math.ceil(this._columnLabelHeight);
    },

    // returns which col contains this point
    _getColFromXY: function(point) {
        // calc col
        var xDist = point.x - this._heatMapTopLeftPoint.x;
        if (xDist >= 0) {
            if (this._useRowNames) {
                if (xDist <= this._rowLabelWidth) {
                    // clicked on rowname
                    return 0;
                } else {
                    // replace row label width with cell width
                    xDist -= this._rowLabelWidth;
                    xDist += this._cellWidth;
                }
            }
            var xCol = Math.floor(xDist / this._cellWidth);
            if (xDist % this._cellWidth > 0)
                xCol++;
            xCol--; // for zero indexing
            if (xCol < this._numColumns)
                return xCol;
        }
    },

    // returns which row contains this point. returns -1 for column header selection
    _getRowFromXY: function(point) {
        // calc row
        var yDist = point.y - this._heatMapTopLeftPoint.y;
        if (yDist >= 0) {
            if (yDist - this._columnLabelHeight < 0) {
                // user selected column header.
                return -1;
            } else {
                yDist -= this._columnLabelHeight;
                var yRow = Math.floor(yDist / this._cellHeight);
                if (yDist % this._cellHeight > 0)
                    yRow++;
                yRow--; // for zero indexing
                if (yRow < this._numRows)
                    return yRow;
            }
        }
    },

    // returns which cell(row/col) contains this point
    _getCellFromXY: function(point) {
        // if within heatmap
        var row = this._getRowFromXY(point);
        var col = this._getColFromXY(point);
        var cell = {row:row,col:col};

        //    var cell = {row:this._getRowFromXY(point),
        //                col:this._getColFromXY(point)};
        if ((cell.row >= 0 || cell.row == -1) && cell.col >= 0)
            return cell;
    },


    // ------------------------------------
    // Random helper Functions
    // ------------------------------------

    // returns the top left position (x,y) of a cell (row,col)
    _getCellXYTopLeft: function(row, col) {
        var point = {x:0,y:0};
        if (col != 0 && this._useRowNames) {
            point.x = this._rowLabelWidth;
            col--; // because we've added the "special" first column already
        }
        point.x = point.x + this._heatMapTopLeftPoint.x + col * this._cellWidth;
        point.y = this._heatMapTopLeftPoint.y + this._columnLabelHeight + row * this._cellHeight;
        return point;
    },

    _displayError: function(message) {
        alert("BioHeatMapError: " + message);
        this._drawError = true;
    },


    // Utility function to escape HTML special characters
    _escapeHtml: function(text) {
        if (text == null)
            return '';
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;')
                .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    },

    randInt: function(min, max) {
        if (max) {
            return Math.floor(Math.random() * (max - min + 1)) + min;
        } else {
            return Math.floor(Math.random() * (min + 1));
        }
    },

    _getValueFormattedOrNot: function(row, col, data) {
        var value = data.getFormattedValue(row, col);
        if (!value) {
            value = data.getValue(row, col);
        }
        return value;
    },


    // ------------------------------------
    // Events
    // ------------------------------------

    _onClickEvent: function(point) {
        var cell = this._getCellFromXY(point);
        if (cell) {
            var row = cell.row;
            var col = cell.col;

            if (col >= 0 && row == -1) {
                // ignore column header above row names when using row names
                if (!this._useRowNames || col != 0) {
                    this._log("column header selected, col:" + col);
                    this.setSelection([{column:col}]);
                    google.visualization.events.trigger(this, 'select', null);
                }
            } else if (col == 0 && row >= 0 && this._useRowNames) {
                this._log("row label selected, row:" + row);
                this.setSelection([{row:row}]);
                google.visualization.events.trigger(this, 'select', null);
            } else if (col >= 0 && row >= 0) {
                this._log("Selcted Cell - r:" + cell.row + ", c:" + cell.col);
                this.setSelection([{row:row, column:col}]);
                google.visualization.events.trigger(this, 'select', null);
            } else {
                this.setSelection([]);
            }
        } else {
            this.setSelection([]);
        }
    },

    // mouse move handler to implement tooltip behaviour
    _onMoveEvent: function(localPos, mousePos) {
        // only run if displaying tooltips is enabled
        if (this._displayCellTooltips) {

        // all movements clear the current timer
        var tooltipElement = this.tooltipElement;
        var timerId = tooltipElement.timerId;
        if(timerId) clearTimeout(timerId);

        var cell = this._getCellFromXY(localPos);
        if (cell && cell.row != -1) {
            var props = this.data.getProperties(cell.row, cell.col);
            if(props && props.tooltip) {
                var tooltip = props.tooltip.replace(/\n/g, '<br/>'); // line-breaks for newline characters
                var tooltipDelay = this._tooltipDelay;
                var showTooltip = function() {
                    tooltipElement.style.display = "block";
                    tooltipElement.innerHTML = tooltip;
                    tooltipElement.style.left = mousePos.x + "px";
                    tooltipElement.style.top = (mousePos.y - tooltipElement.offsetHeight) + "px";
                };
                tooltipElement.timerId = setTimeout(showTooltip, tooltipDelay);

                return;
            }
        }

        // fall-through for if no tooltip should be displayed
        tooltipElement.style.display = "none";
        }
    },

    _clearSelection: function() {
        if (this._selectionState) {
            //        this.ctx.restore();
            // TODO: find a way to remove drawn selections without totally redrawing?
            this.draw(this.data, this.options);
        }
        this._selected = [];
        this._selectedState = false;
    },

    // ------------------------------------
    // Mouse functions
    // ------------------------------------

    // note: modified to take a callback
    _getMouseXY: function(handler) {
        var bioHeatMap = this;
        return function(e) {
  //          var t = this;
            if(!e) e = window.event; // for IE

            // absolute mouse position on the page
            var mouseX = 0;
            var mouseY = 0;
            if (e.pageX || e.pageY) {
                mouseX = e.pageX;
                mouseY = e.pageY;
            }
            else if (e.clientX || e.clientY) {
                mouseX = e.clientX + document.body.scrollLeft
                        + document.documentElement.scrollLeft;
                mouseY = e.clientY + document.body.scrollTop
                        + document.documentElement.scrollTop;
            }

            // target element
            var t;
	        if (e.target) t = e.target;
	        else if (e.srcElement) t = e.srcElement;
	        if (t.nodeType == 3) // defeat Safari bug
		    t = t.parentNode;

            // target element location in page
            var curleft = 0;
            var curtop = 0;
            if (t.offsetParent) {
                do {
                    curleft += t.offsetLeft;
                    curtop += t.offsetTop;
                } while (t = t.offsetParent)
            }

            // x,y relative to target
            this._clickPosition = {x: mouseX - curleft, y: mouseY - curtop}; // query: this is altering the canvas - was this the intention? Retained to match old code
            var localPos = {x: mouseX - curleft, y: mouseY - curtop};
            var mousePos = {x : mouseX, y: mouseY};
            handler(localPos, mousePos);
            return localPos;
        }
    },


    // ------------------------------------
    // Logging
    // ------------------------------------

    _log: function(message) {
        if (this._debug) {
            console.log(message);
        }
    },

    _logCalculated: function() {
        this._log("canvas size - w:" + this.canvas.width + ", h:" + this.canvas.height);
        this._log("calculated values - "
                + "\n_cellWidth:" + this._cellWidth
                + "  _cellHeight:" + this._cellHeight
                + "\n_columnLabelHeight:" + this._columnLabelHeight
                + "\n_rowLabelWidth:" + this._rowLabelWidth
                + "\n_heatMapHeight:" + this._heatMapHeight
                + "  _heatMapWidth:" + this._heatMapWidth
                + "\n_heatMapTopLeftPoint - x:" + this._heatMapTopLeftPoint.x + ", y:" + this._heatMapTopLeftPoint.y
                + "\n_numDataColumns:" + this._numDataColumns
                + "\n_numColumns:" + this._numColumns
                + "\n_numRows:" + this._numRows
                + "\n_dataRange - min:" + this._dataRange.min +", max:" + this._dataRange.max
                + "\n_colorStep - r:" + this._colorStep.r +", g:" + this._colorStep.g +", b:" + this._colorStep.b +", a:" + this._colorStep.a
                );
    }
});



// avoid console errors w/ no firebug
if (!window.console || !console.firebug)
{
    var names = ["log", "debug", "info", "warn", "error", "assert", "dir", "dirxml",
        "group", "groupEnd", "time", "timeEnd", "count", "trace", "profile", "profileEnd"];

    window.console = {};
    for (var i = 0; i < names.length; ++i)
        window.console[names[i]] = function() {
        }
}




// ---------------------------------------------------------------------------------------------------------------------
// - ColorRange Class
// -
// -   Description: Defines a range of colors from start to end, depending on the options.
// -   Author: dburdick
// -
// ---------------------------------------------------------------------------------------------------------------------

org.systemsbiology.visualization.DiscreteColorRange = Class.create({
    // --------------------------------
    // constants
    // --------------------------------
	MINCOLORS: 2,
	PASS_THROUGH_BLACK_MINCOLORS: 2,
	NO_PASS_THROUGHBLACK_MINCOLORS: 2,
	MINRGB: 0,
	MAXRGB: 255,
	BLACK_RGBA: {r:0, g:0, b:0, a:1},

    // --------------------------------
    // Private Attributes
    // --------------------------------

    // setable option defaults
    _maxColors: 64, // number of colors to divide space into
    _backgroundColor: {r:0, g:0, b:0, a:1},
    _maxColor: {r:255, g:0, b:0, a:1},
    _minColor: {r:0, g:255, b:0, a:1},
    _emptyDataColor: {r:100, g:100, b:100, a:1},
    _passThroughBlack: true,

    // other
    _dataRange: {min:null, max:null},
    _debug: false,
    _colorRange: null,

    // calculated
    _colorStep: {r:null, g:null, b:null, a:null},
    _dataStep: null,
    _maxDataSpace: null,


    // --------------------------------
    // Public Methods
    // --------------------------------

    // constructor
    initialize: function(maxColors,dataRange,options) {
        // check required parameters
        if(maxColors>=1 && dataRange) {
            this._maxColors = maxColors;
            this._dataRange = dataRange;
        } else {
            throw('Error in org.systemsbiology.visualization.DiscreteColorRange instantiation. required parameters not provided');
        }

        // set optional parameters
        if (options) {
            if(options.maxColor)
                this._maxColor = this.niceRGBAColor(options.maxColor);
            if(options.minColor)
                this._minColor = this.niceRGBAColor(options.minColor);
            if(options.emptyDataColor)
                this._emptyDataColor = this.niceRGBAColor(options.emptyDataColor);
            if(options.passThroughBlack!=null && options.passThroughBlack == false) {
                this._passThroughBlack = false;
            }
        }
        // setup color space
        this._colorRange = new Array();
        this._setupColorRange();
    },

    // when given an RBGA object it returns a canvas-formatted string for that color
    // if the RGBA is empty or ill-defined it returns a string for the empty data color
    getCellColorString: function(dataValue) {
        var colorValue = this.getCellColorRGBA(dataValue);
        var colorString;
        if (colorValue.r >= 0 && colorValue.g >= 0 && colorValue.b >= 0 && colorValue.a >= 0) {
            colorString = this.getRgbaColorString(colorValue);
        } else {
            colorString = this.getRgbaColorString(this._emptyDataColor);
        }

//        this._log("Value="+dataValue+", colorString="+colorString);
        return colorString;
    },

    // returns an RBGA object with the color for the given dataValue
    getCellColorRGBA: function(dataValue) {
        if(dataValue == null) {
            return this._emptyDataColor;
        }

        var dataBin = dataValue / this._dataStep;
        var binOffset = this._dataRange.min/this._dataStep;
        var newDataBin = (dataBin - binOffset);
        // round
        if(newDataBin<0)
            newDataBin = Math.ceil(newDataBin);
        else
            newDataBin = Math.floor(newDataBin);


        this._log('value: '+dataValue + ' bin: '+dataBin + ' new bin: '+ newDataBin);

        // assure bounds
        if(newDataBin<0)
            newDataBin=0;
        if(newDataBin>=this._colorRange.length)
            newDataBin = (this._colorRange.length)-1;
        return this._colorRange[newDataBin];
    },

    // returns the Hex color for the given dataValue
    getCellColorHex: function(dataValue) {
        var rgba = this.getCellColorRGBA(dataValue);
        return "#" + this._RGBtoHex(rgba.r, rgba.g, rgba.b);
    },

    getRgbaColorString: function(rgba) {
        if (rgba.r >= 0 && rgba.g >= 0 && rgba.b >= 0 && rgba.a >= 0) {
            return "rgba(" + rgba.r + "," + rgba.g + "," + rgba.b + "," + rgba.a + ")";
        }
    },

    // makes sure each value of the RGBA is in a reasonable range
    niceRGBAColor: function(rgbaColor) {
        var newRgbaColor = {r:null, g:null, b:null, a:null};
        newRgbaColor.r = this.niceIndividualColor(rgbaColor.r);
        newRgbaColor.g = this.niceIndividualColor(rgbaColor.g);
        newRgbaColor.b = this.niceIndividualColor(rgbaColor.b);
        if (rgbaColor.a < 0)
            newRgbaColor.a = 0;
        else if (rgbaColor.a > 1)
            newRgbaColor.a = 1;
        else
            newRgbaColor.a = rgbaColor.a
        return newRgbaColor;
    },

    // keeps a value between MINRGB and MAXRGB
    niceIndividualColor: function(individualColor) {
        if(individualColor<this.MINRGB)
            return this.MINRGB;
        if(individualColor>this.MAXRGB)
            return this.MAXRGB;
        return Math.floor(individualColor);
    },

    // --------------------------------
    // Private Methods
    // --------------------------------

    // maps data ranges to colors
    _setupColorRange: function() {
        var dataRange = this._dataRange;
        var maxColors = this._maxColors;
        var centerColor = this.BLACK_RGBA;
        var colorStep;

        if (maxColors > 256)
            maxColors = 256;
        if (maxColors < 1) {
            maxColors = 1;
        }
        this._maxDataSpace = Math.abs(dataRange.min) + Math.abs(dataRange.max);
        this._dataStep = this._maxDataSpace / maxColors;

        if(this._passThroughBlack) {
            // determine the color step for each attribute of the color
            colorStep = {
                r: 2*this._calcColorStep(this._minColor.r, centerColor.r, maxColors),
                g: 2*this._calcColorStep(this._minColor.g, centerColor.g, maxColors),
                b: 2*this._calcColorStep(this._minColor.b, centerColor.b, maxColors),
                a: 2*this._calcColorStep(this._minColor.a, centerColor.a, maxColors)
            };
            this._addColorsToRange(this._minColor,colorStep,maxColors/2);

            colorStep = {
                r: 2*this._calcColorStep(centerColor.r, this._maxColor.r, maxColors),
                g: 2*this._calcColorStep(centerColor.g, this._maxColor.g, maxColors),
                b: 2*this._calcColorStep(centerColor.b, this._maxColor.b, maxColors),
                a: 2*this._calcColorStep(centerColor.a, this._maxColor.a, maxColors)
            };
            this._addColorsToRange(centerColor,colorStep,(maxColors/2)+1);

        } else {
            // single continue range
            colorStep = {
                r: this._calcColorStep(this._minColor.r, this._maxColor.r, maxColors),
                g: this._calcColorStep(this._minColor.g, this._maxColor.g, maxColors),
                b: this._calcColorStep(this._minColor.b, this._maxColor.b, maxColors),
                a: this._calcColorStep(this._minColor.a, this._maxColor.a, maxColors)
            };
            this._addColorsToRange(this._minColor,colorStep,maxColors);
        }

        // calc data step
        this._maxDataSpace = Math.abs(dataRange.min) + Math.abs(dataRange.max);
        this._dataStep = this._maxDataSpace / maxColors;

        this._log('dataStep: '+this._dataStep);

    },

    _calcColorStep: function(minColor, maxColor, numberColors) {
        if (numberColors <= 0) return;
        var numColors = numberColors==1 ? 1 : numberColors-1;
        return ((maxColor - minColor) / numColors);
    },

    // append colors to the end of the color Range, splitting the number of colors up evenly
    _addColorsToRange: function(startColor,colorStep,numberColors) {
        var currentColor = this.niceRGBAColor(startColor);
        for(var i=0; i<numberColors; i++) {
            this._colorRange[this._colorRange.length] = currentColor;
            currentColor = this.niceRGBAColor({
                r: currentColor.r + colorStep.r,
                g: currentColor.g + colorStep.g,
                b: currentColor.b + colorStep.b,
                a: currentColor.a + colorStep.a
            });

        }
    },

    _log: function(message) {
        if (this._debug) {
            console.log(message);
        }
    },

    _RGBtoHex: function(R,G,B) {
        return this._toHex(R)+this._toHex(G)+this._toHex(B);
    },

    _toHex: function(N) {
        if (N == null) return "00";
        N = parseInt(N);
        if (N == 0 || isNaN(N)) return "00";
        N = Math.max(0, N);
        N = Math.min(N, 255);
        N = Math.round(N);
        return "0123456789ABCDEF".charAt((N - N % 16) / 16)
                + "0123456789ABCDEF".charAt(N % 16);
    }

});










// EXTERNAL SCRIPTS:


// ---------------------------------------------------------------------------------------------------------------------
// - CanvasText.js
// -
// -   Description: Draws text on the canvas natively
// -
// -   This code is released to the public domain by Jim Studt, 2007.
// -   He may keep some sort of up to date copy at http://www.federated.com/~jim/canvastext/
// -
// ---------------------------------------------------------------------------------------------------------------------

//
//
var CanvasTextFunctions = { };

CanvasTextFunctions.letters = {
    ' ': { width: 16, points: [] },
    '!': { width: 10, points: [[5,21],[5,7],[-1,-1],[5,2],[4,1],[5,0],[6,1],[5,2]] },
    '"': { width: 16, points: [[4,21],[4,14],[-1,-1],[12,21],[12,14]] },
    '#': { width: 21, points: [[11,25],[4,-7],[-1,-1],[17,25],[10,-7],[-1,-1],[4,12],[18,12],[-1,-1],[3,6],[17,6]] },
    '$': { width: 20, points: [[8,25],[8,-4],[-1,-1],[12,25],[12,-4],[-1,-1],[17,18],[15,20],[12,21],[8,21],[5,20],[3,18],[3,16],[4,14],[5,13],[7,12],[13,10],[15,9],[16,8],[17,6],[17,3],[15,1],[12,0],[8,0],[5,1],[3,3]] },
    '%': { width: 24, points: [[21,21],[3,0],[-1,-1],[8,21],[10,19],[10,17],[9,15],[7,14],[5,14],[3,16],[3,18],[4,20],[6,21],[8,21],[10,20],[13,19],[16,19],[19,20],[21,21],[-1,-1],[17,7],[15,6],[14,4],[14,2],[16,0],[18,0],[20,1],[21,3],[21,5],[19,7],[17,7]] },
    '&': { width: 26, points: [[23,12],[23,13],[22,14],[21,14],[20,13],[19,11],[17,6],[15,3],[13,1],[11,0],[7,0],[5,1],[4,2],[3,4],[3,6],[4,8],[5,9],[12,13],[13,14],[14,16],[14,18],[13,20],[11,21],[9,20],[8,18],[8,16],[9,13],[11,10],[16,3],[18,1],[20,0],[22,0],[23,1],[23,2]] },
    '\'': { width: 10, points: [[5,19],[4,20],[5,21],[6,20],[6,18],[5,16],[4,15]] },
    '(': { width: 14, points: [[11,25],[9,23],[7,20],[5,16],[4,11],[4,7],[5,2],[7,-2],[9,-5],[11,-7]] },
    ')': { width: 14, points: [[3,25],[5,23],[7,20],[9,16],[10,11],[10,7],[9,2],[7,-2],[5,-5],[3,-7]] },
    '*': { width: 16, points: [[8,21],[8,9],[-1,-1],[3,18],[13,12],[-1,-1],[13,18],[3,12]] },
    '+': { width: 26, points: [[13,18],[13,0],[-1,-1],[4,9],[22,9]] },
    ',': { width: 10, points: [[6,1],[5,0],[4,1],[5,2],[6,1],[6,-1],[5,-3],[4,-4]] },
    '-': { width: 26, points: [[4,9],[22,9]] },
    '.': { width: 10, points: [[5,2],[4,1],[5,0],[6,1],[5,2]] },
    '/': { width: 22, points: [[20,25],[2,-7]] },
    '0': { width: 20, points: [[9,21],[6,20],[4,17],[3,12],[3,9],[4,4],[6,1],[9,0],[11,0],[14,1],[16,4],[17,9],[17,12],[16,17],[14,20],[11,21],[9,21]] },
    '1': { width: 20, points: [[6,17],[8,18],[11,21],[11,0]] },
    '2': { width: 20, points: [[4,16],[4,17],[5,19],[6,20],[8,21],[12,21],[14,20],[15,19],[16,17],[16,15],[15,13],[13,10],[3,0],[17,0]] },
    '3': { width: 20, points: [[5,21],[16,21],[10,13],[13,13],[15,12],[16,11],[17,8],[17,6],[16,3],[14,1],[11,0],[8,0],[5,1],[4,2],[3,4]] },
    '4': { width: 20, points: [[13,21],[3,7],[18,7],[-1,-1],[13,21],[13,0]] },
    '5': { width: 20, points: [[15,21],[5,21],[4,12],[5,13],[8,14],[11,14],[14,13],[16,11],[17,8],[17,6],[16,3],[14,1],[11,0],[8,0],[5,1],[4,2],[3,4]] },
    '6': { width: 20, points: [[16,18],[15,20],[12,21],[10,21],[7,20],[5,17],[4,12],[4,7],[5,3],[7,1],[10,0],[11,0],[14,1],[16,3],[17,6],[17,7],[16,10],[14,12],[11,13],[10,13],[7,12],[5,10],[4,7]] },
    '7': { width: 20, points: [[17,21],[7,0],[-1,-1],[3,21],[17,21]] },
    '8': { width: 20, points: [[8,21],[5,20],[4,18],[4,16],[5,14],[7,13],[11,12],[14,11],[16,9],[17,7],[17,4],[16,2],[15,1],[12,0],[8,0],[5,1],[4,2],[3,4],[3,7],[4,9],[6,11],[9,12],[13,13],[15,14],[16,16],[16,18],[15,20],[12,21],[8,21]] },
    '9': { width: 20, points: [[16,14],[15,11],[13,9],[10,8],[9,8],[6,9],[4,11],[3,14],[3,15],[4,18],[6,20],[9,21],[10,21],[13,20],[15,18],[16,14],[16,9],[15,4],[13,1],[10,0],[8,0],[5,1],[4,3]] },
    ':': { width: 10, points: [[5,14],[4,13],[5,12],[6,13],[5,14],[-1,-1],[5,2],[4,1],[5,0],[6,1],[5,2]] },
    ',': { width: 10, points: [[5,14],[4,13],[5,12],[6,13],[5,14],[-1,-1],[6,1],[5,0],[4,1],[5,2],[6,1],[6,-1],[5,-3],[4,-4]] },
    '<': { width: 24, points: [[20,18],[4,9],[20,0]] },
    '=': { width: 26, points: [[4,12],[22,12],[-1,-1],[4,6],[22,6]] },
    '>': { width: 24, points: [[4,18],[20,9],[4,0]] },
    '?': { width: 18, points: [[3,16],[3,17],[4,19],[5,20],[7,21],[11,21],[13,20],[14,19],[15,17],[15,15],[14,13],[13,12],[9,10],[9,7],[-1,-1],[9,2],[8,1],[9,0],[10,1],[9,2]] },
    '@': { width: 27, points: [[18,13],[17,15],[15,16],[12,16],[10,15],[9,14],[8,11],[8,8],[9,6],[11,5],[14,5],[16,6],[17,8],[-1,-1],[12,16],[10,14],[9,11],[9,8],[10,6],[11,5],[-1,-1],[18,16],[17,8],[17,6],[19,5],[21,5],[23,7],[24,10],[24,12],[23,15],[22,17],[20,19],[18,20],[15,21],[12,21],[9,20],[7,19],[5,17],[4,15],[3,12],[3,9],[4,6],[5,4],[7,2],[9,1],[12,0],[15,0],[18,1],[20,2],[21,3],[-1,-1],[19,16],[18,8],[18,6],[19,5]] },
    'A': { width: 18, points: [[9,21],[1,0],[-1,-1],[9,21],[17,0],[-1,-1],[4,7],[14,7]] },
    'B': { width: 21, points: [[4,21],[4,0],[-1,-1],[4,21],[13,21],[16,20],[17,19],[18,17],[18,15],[17,13],[16,12],[13,11],[-1,-1],[4,11],[13,11],[16,10],[17,9],[18,7],[18,4],[17,2],[16,1],[13,0],[4,0]] },
    'C': { width: 21, points: [[18,16],[17,18],[15,20],[13,21],[9,21],[7,20],[5,18],[4,16],[3,13],[3,8],[4,5],[5,3],[7,1],[9,0],[13,0],[15,1],[17,3],[18,5]] },
    'D': { width: 21, points: [[4,21],[4,0],[-1,-1],[4,21],[11,21],[14,20],[16,18],[17,16],[18,13],[18,8],[17,5],[16,3],[14,1],[11,0],[4,0]] },
    'E': { width: 19, points: [[4,21],[4,0],[-1,-1],[4,21],[17,21],[-1,-1],[4,11],[12,11],[-1,-1],[4,0],[17,0]] },
    'F': { width: 18, points: [[4,21],[4,0],[-1,-1],[4,21],[17,21],[-1,-1],[4,11],[12,11]] },
    'G': { width: 21, points: [[18,16],[17,18],[15,20],[13,21],[9,21],[7,20],[5,18],[4,16],[3,13],[3,8],[4,5],[5,3],[7,1],[9,0],[13,0],[15,1],[17,3],[18,5],[18,8],[-1,-1],[13,8],[18,8]] },
    'H': { width: 22, points: [[4,21],[4,0],[-1,-1],[18,21],[18,0],[-1,-1],[4,11],[18,11]] },
    'I': { width: 8, points: [[4,21],[4,0]] },
    'J': { width: 16, points: [[12,21],[12,5],[11,2],[10,1],[8,0],[6,0],[4,1],[3,2],[2,5],[2,7]] },
    'K': { width: 21, points: [[4,21],[4,0],[-1,-1],[18,21],[4,7],[-1,-1],[9,12],[18,0]] },
    'L': { width: 17, points: [[4,21],[4,0],[-1,-1],[4,0],[16,0]] },
    'M': { width: 24, points: [[4,21],[4,0],[-1,-1],[4,21],[12,0],[-1,-1],[20,21],[12,0],[-1,-1],[20,21],[20,0]] },
    'N': { width: 22, points: [[4,21],[4,0],[-1,-1],[4,21],[18,0],[-1,-1],[18,21],[18,0]] },
    'O': { width: 22, points: [[9,21],[7,20],[5,18],[4,16],[3,13],[3,8],[4,5],[5,3],[7,1],[9,0],[13,0],[15,1],[17,3],[18,5],[19,8],[19,13],[18,16],[17,18],[15,20],[13,21],[9,21]] },
    'P': { width: 21, points: [[4,21],[4,0],[-1,-1],[4,21],[13,21],[16,20],[17,19],[18,17],[18,14],[17,12],[16,11],[13,10],[4,10]] },
    'Q': { width: 22, points: [[9,21],[7,20],[5,18],[4,16],[3,13],[3,8],[4,5],[5,3],[7,1],[9,0],[13,0],[15,1],[17,3],[18,5],[19,8],[19,13],[18,16],[17,18],[15,20],[13,21],[9,21],[-1,-1],[12,4],[18,-2]] },
    'R': { width: 21, points: [[4,21],[4,0],[-1,-1],[4,21],[13,21],[16,20],[17,19],[18,17],[18,15],[17,13],[16,12],[13,11],[4,11],[-1,-1],[11,11],[18,0]] },
    'S': { width: 20, points: [[17,18],[15,20],[12,21],[8,21],[5,20],[3,18],[3,16],[4,14],[5,13],[7,12],[13,10],[15,9],[16,8],[17,6],[17,3],[15,1],[12,0],[8,0],[5,1],[3,3]] },
    'T': { width: 16, points: [[8,21],[8,0],[-1,-1],[1,21],[15,21]] },
    'U': { width: 22, points: [[4,21],[4,6],[5,3],[7,1],[10,0],[12,0],[15,1],[17,3],[18,6],[18,21]] },
    'V': { width: 18, points: [[1,21],[9,0],[-1,-1],[17,21],[9,0]] },
    'W': { width: 24, points: [[2,21],[7,0],[-1,-1],[12,21],[7,0],[-1,-1],[12,21],[17,0],[-1,-1],[22,21],[17,0]] },
    'X': { width: 20, points: [[3,21],[17,0],[-1,-1],[17,21],[3,0]] },
    'Y': { width: 18, points: [[1,21],[9,11],[9,0],[-1,-1],[17,21],[9,11]] },
    'Z': { width: 20, points: [[17,21],[3,0],[-1,-1],[3,21],[17,21],[-1,-1],[3,0],[17,0]] },
    '[': { width: 14, points: [[4,25],[4,-7],[-1,-1],[5,25],[5,-7],[-1,-1],[4,25],[11,25],[-1,-1],[4,-7],[11,-7]] },
    '\\': { width: 14, points: [[0,21],[14,-3]] },
    ']': { width: 14, points: [[9,25],[9,-7],[-1,-1],[10,25],[10,-7],[-1,-1],[3,25],[10,25],[-1,-1],[3,-7],[10,-7]] },
    '^': { width: 16, points: [[6,15],[8,18],[10,15],[-1,-1],[3,12],[8,17],[13,12],[-1,-1],[8,17],[8,0]] },
    '_': { width: 16, points: [[0,-2],[16,-2]] },
    '`': { width: 10, points: [[6,21],[5,20],[4,18],[4,16],[5,15],[6,16],[5,17]] },
    'a': { width: 19, points: [[15,14],[15,0],[-1,-1],[15,11],[13,13],[11,14],[8,14],[6,13],[4,11],[3,8],[3,6],[4,3],[6,1],[8,0],[11,0],[13,1],[15,3]] },
    'b': { width: 19, points: [[4,21],[4,0],[-1,-1],[4,11],[6,13],[8,14],[11,14],[13,13],[15,11],[16,8],[16,6],[15,3],[13,1],[11,0],[8,0],[6,1],[4,3]] },
    'c': { width: 18, points: [[15,11],[13,13],[11,14],[8,14],[6,13],[4,11],[3,8],[3,6],[4,3],[6,1],[8,0],[11,0],[13,1],[15,3]] },
    'd': { width: 19, points: [[15,21],[15,0],[-1,-1],[15,11],[13,13],[11,14],[8,14],[6,13],[4,11],[3,8],[3,6],[4,3],[6,1],[8,0],[11,0],[13,1],[15,3]] },
    'e': { width: 18, points: [[3,8],[15,8],[15,10],[14,12],[13,13],[11,14],[8,14],[6,13],[4,11],[3,8],[3,6],[4,3],[6,1],[8,0],[11,0],[13,1],[15,3]] },
    'f': { width: 12, points: [[10,21],[8,21],[6,20],[5,17],[5,0],[-1,-1],[2,14],[9,14]] },
    'g': { width: 19, points: [[15,14],[15,-2],[14,-5],[13,-6],[11,-7],[8,-7],[6,-6],[-1,-1],[15,11],[13,13],[11,14],[8,14],[6,13],[4,11],[3,8],[3,6],[4,3],[6,1],[8,0],[11,0],[13,1],[15,3]] },
    'h': { width: 19, points: [[4,21],[4,0],[-1,-1],[4,10],[7,13],[9,14],[12,14],[14,13],[15,10],[15,0]] },
    'i': { width: 8, points: [[3,21],[4,20],[5,21],[4,22],[3,21],[-1,-1],[4,14],[4,0]] },
    'j': { width: 10, points: [[5,21],[6,20],[7,21],[6,22],[5,21],[-1,-1],[6,14],[6,-3],[5,-6],[3,-7],[1,-7]] },
    'k': { width: 17, points: [[4,21],[4,0],[-1,-1],[14,14],[4,4],[-1,-1],[8,8],[15,0]] },
    'l': { width: 8, points: [[4,21],[4,0]] },
    'm': { width: 30, points: [[4,14],[4,0],[-1,-1],[4,10],[7,13],[9,14],[12,14],[14,13],[15,10],[15,0],[-1,-1],[15,10],[18,13],[20,14],[23,14],[25,13],[26,10],[26,0]] },
    'n': { width: 19, points: [[4,14],[4,0],[-1,-1],[4,10],[7,13],[9,14],[12,14],[14,13],[15,10],[15,0]] },
    'o': { width: 19, points: [[8,14],[6,13],[4,11],[3,8],[3,6],[4,3],[6,1],[8,0],[11,0],[13,1],[15,3],[16,6],[16,8],[15,11],[13,13],[11,14],[8,14]] },
    'p': { width: 19, points: [[4,14],[4,-7],[-1,-1],[4,11],[6,13],[8,14],[11,14],[13,13],[15,11],[16,8],[16,6],[15,3],[13,1],[11,0],[8,0],[6,1],[4,3]] },
    'q': { width: 19, points: [[15,14],[15,-7],[-1,-1],[15,11],[13,13],[11,14],[8,14],[6,13],[4,11],[3,8],[3,6],[4,3],[6,1],[8,0],[11,0],[13,1],[15,3]] },
    'r': { width: 13, points: [[4,14],[4,0],[-1,-1],[4,8],[5,11],[7,13],[9,14],[12,14]] },
    's': { width: 17, points: [[14,11],[13,13],[10,14],[7,14],[4,13],[3,11],[4,9],[6,8],[11,7],[13,6],[14,4],[14,3],[13,1],[10,0],[7,0],[4,1],[3,3]] },
    't': { width: 12, points: [[5,21],[5,4],[6,1],[8,0],[10,0],[-1,-1],[2,14],[9,14]] },
    'u': { width: 19, points: [[4,14],[4,4],[5,1],[7,0],[10,0],[12,1],[15,4],[-1,-1],[15,14],[15,0]] },
    'v': { width: 16, points: [[2,14],[8,0],[-1,-1],[14,14],[8,0]] },
    'w': { width: 22, points: [[3,14],[7,0],[-1,-1],[11,14],[7,0],[-1,-1],[11,14],[15,0],[-1,-1],[19,14],[15,0]] },
    'x': { width: 17, points: [[3,14],[14,0],[-1,-1],[14,14],[3,0]] },
    'y': { width: 16, points: [[2,14],[8,0],[-1,-1],[14,14],[8,0],[6,-4],[4,-6],[2,-7],[1,-7]] },
    'z': { width: 17, points: [[14,14],[3,0],[-1,-1],[3,14],[14,14],[-1,-1],[3,0],[14,0]] },
    '{': { width: 14, points: [[9,25],[7,24],[6,23],[5,21],[5,19],[6,17],[7,16],[8,14],[8,12],[6,10],[-1,-1],[7,24],[6,22],[6,20],[7,18],[8,17],[9,15],[9,13],[8,11],[4,9],[8,7],[9,5],[9,3],[8,1],[7,0],[6,-2],[6,-4],[7,-6],[-1,-1],[6,8],[8,6],[8,4],[7,2],[6,1],[5,-1],[5,-3],[6,-5],[7,-6],[9,-7]] },
    '|': { width: 8, points: [[4,25],[4,-7]] },
    '}': { width: 14, points: [[5,25],[7,24],[8,23],[9,21],[9,19],[8,17],[7,16],[6,14],[6,12],[8,10],[-1,-1],[7,24],[8,22],[8,20],[7,18],[6,17],[5,15],[5,13],[6,11],[10,9],[6,7],[5,5],[5,3],[6,1],[7,0],[8,-2],[8,-4],[7,-6],[-1,-1],[8,8],[6,6],[6,4],[7,2],[8,1],[9,-1],[9,-3],[8,-5],[7,-6],[5,-7]] },
    '~': { width: 24, points: [[3,6],[3,8],[4,11],[6,12],[8,12],[10,11],[14,8],[16,7],[18,7],[20,8],[21,10],[-1,-1],[3,8],[4,10],[6,11],[8,11],[10,10],[14,7],[16,6],[18,6],[20,7],[21,10],[21,12]] }
};

CanvasTextFunctions.letter = function (ch)
{
    return CanvasTextFunctions.letters[ch];
}

CanvasTextFunctions.ascent = function( font, size)
{
    return size;
}

CanvasTextFunctions.descent = function( font, size)
{
    return 7.0*size/25.0;
}

CanvasTextFunctions.measure = function( font, size, str)
{
    var total = 0;
    var len = str.length;

    for ( i = 0; i < len; i++) {
  var c = CanvasTextFunctions.letter( str.charAt(i));
  if ( c) total += c.width * size / 25.0;
    }
    return total;
}

CanvasTextFunctions.draw = function(ctx,font,size,x,y,str)
{
    var total = 0;
    var len = str.length;
    var mag = size / 25.0;

    ctx.save();
    ctx.lineCap = "round";
    ctx.lineWidth = 2.0 * mag;

    for ( i = 0; i < len; i++) {
  var c = CanvasTextFunctions.letter( str.charAt(i));
  if ( !c) continue;

  ctx.beginPath();

  var penUp = 1;
  var needStroke = 0;
  for ( j = 0; j < c.points.length; j++) {
      var a = c.points[j];
      if ( a[0] == -1 && a[1] == -1) {
    penUp = 1;
    continue;
      }
      if ( penUp) {
    ctx.moveTo( x + a[0]*mag, y - a[1]*mag);
    penUp = false;
      } else {
    ctx.lineTo( x + a[0]*mag, y - a[1]*mag);
      }
  }
  ctx.stroke();
  x += c.width*mag;
    }
    ctx.restore();
    return total;
}

CanvasTextFunctions.enable = function( ctx)
{
    ctx.drawText = function(font,size,x,y,text) { return CanvasTextFunctions.draw( ctx, font,size,x,y,text); };
    ctx.measureText = function(font,size,text) { return CanvasTextFunctions.measure( font,size,text); };
    ctx.fontAscent = function(font,size) { return CanvasTextFunctions.ascent(font,size); }
    ctx.fontDescent = function(font,size) { return CanvasTextFunctions.descent(font,size); }

    ctx.drawTextRight = function(font,size,x,y,text) {
  var w = CanvasTextFunctions.measure(font,size,text);
  return CanvasTextFunctions.draw( ctx, font,size,x-w,y,text);
    };
    ctx.drawTextCenter = function(font,size,x,y,text) {
  var w = CanvasTextFunctions.measure(font,size,text);
  return CanvasTextFunctions.draw( ctx, font,size,x-w/2,y,text);
    };
}





//---------------------------------------------------------------------------------------------------------------------
// excanvas.js

// Copyright 2006 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


// Known Issues:
//
// * Patterns are not implemented.
// * Radial gradient are not implemented. The VML version of these look very
//   different from the canvas one.
// * Clipping paths are not implemented.
// * Coordsize. The width and height attribute have higher priority than the
//   width and height style values which isn't correct.
// * Painting mode isn't implemented.
// * Canvas width/height should is using content-box by default. IE in
//   Quirks mode will draw the canvas using border-box. Either change your
//   doctype to HTML5
//   (http://www.whatwg.org/specs/web-apps/current-work/#the-doctype)
//   or use Box Sizing Behavior from WebFX
//   (http://webfx.eae.net/dhtml/boxsizing/boxsizing.html)
// * Optimize. There is always room for speed improvements.

// only add this code if we do not already have a canvas implementation
var excanvas = function(canvas){if (arguments.length == 1) return canvas;}
if (!window.CanvasRenderingContext2D) {

excanvas = function (canvasObject) {

  // alias some functions to make (compiled) code shorter
  var m = Math;
  var mr = m.round;
  var ms = m.sin;
  var mc = m.cos;

  // this is used for sub pixel precision
  var Z = 10;
  var Z2 = Z / 2;

  var G_vmlCanvasManager_ = {
    init: function (opt_doc, canvasObject) {
      var doc = opt_doc || document;
      if (/MSIE/.test(navigator.userAgent) && !window.opera) {
        var self = this;
        if (typeof canvasObject != "undefined")
        {
           return self.init_(doc, canvasObject);
        }
        else
        {
            doc.attachEvent("onreadystatechange", function () {self.init_(doc);});
        }
      }
    },

    init_: function (doc, canvasObject)
    {
      if (typeof canvasObject != "undefined")
      {
          if (!canvasObject.getContext) {
           return this.initElement(canvasObject);
          }
        return;
      }
      if (doc.readyState == "complete")
      {
        // create xmlns
        if (!doc.namespaces["g_vml_"]) {
          doc.namespaces.add("g_vml_", "urn:schemas-microsoft-com:vml");
        }

        // setup default css
        var ss = doc.createStyleSheet();
        ss.cssText = "canvas{display:inline-block;/*overflow:hidden;*/" +
            // default size is 300x150 in Gecko and Opera
            "text-align:left;width:300px;height:150px}" +
            "g_vml_\\:*{behavior:url(#default#VML)}";

        // find all canvas elements
        var els = doc.getElementsByTagName("canvas");
        for (var i = 0; i < els.length; i++) {
          if (!els[i].getContext) {
            this.initElement(els[i]);
          }
        }
      }
    },

    fixElement_: function (el) {
      // in IE before version 5.5 we would need to add HTML: to the tag name
      // but we do not care about IE before version 6
      var outerHTML = el.outerHTML;

      var newEl = el.ownerDocument.createElement(outerHTML);
      // if the tag is still open IE has created the children as siblings and
      // it has also created a tag with the name "/FOO"
      if (outerHTML.slice(-2) != "/>") {
        var tagName = "/" + el.tagName;
        var ns;
        // remove content
        while ((ns = el.nextSibling) && ns.tagName != tagName) {
          ns.removeNode();
        }
        // remove the incorrect closing tag
        if (ns) {
          ns.removeNode();
        }
      }
      if (el.parentNode == null)
      {
        return el;
      }

      el.parentNode.replaceChild(newEl, el);
      return newEl;
    },

    /**
     * Public initializes a canvas element so that it can be used as canvas
     * element from now on. This is called automatically before the page is
     * loaded but if you are creating elements using createElement you need to
     * make sure this is called on the element.
     * @param {HTMLElement} el The canvas element to initialize.
     * @return {HTMLElement} the element that was created.
     */
    initElement: function (el) {
      el = this.fixElement_(el);
      el.getContext = function () {
        if (this.context_) {
          return this.context_;
        }
        return this.context_ = new CanvasRenderingContext2D_(this);
      };

      // do not use inline function because that will leak memory
      el.attachEvent('onpropertychange', onPropertyChange);
      el.attachEvent('onresize', onResize);

      var attrs = el.attributes;
      if (attrs.width && attrs.width.specified) {
        // TODO: use runtimeStyle and coordsize
        // el.getContext().setWidth_(attrs.width.nodeValue);
        el.style.width = attrs.width.nodeValue + "px";
      } else {
        el.width = el.clientWidth;
      }
      if (attrs.height && attrs.height.specified) {
        // TODO: use runtimeStyle and coordsize
        // el.getContext().setHeight_(attrs.height.nodeValue);
        el.style.height = attrs.height.nodeValue + "px";
      } else {
        el.height = el.clientHeight;
      }
      //el.getContext().setCoordsize_()
      return el;
    }
  };

  function onPropertyChange(e) {
    var el = e.srcElement;

    switch (e.propertyName) {
      case 'width':
        el.style.width = el.attributes.width.nodeValue + "px";
        //el.getContext().clearRect();
        break;
      case 'height':
        el.style.height = el.attributes.height.nodeValue + "px";
        //el.getContext().clearRect();
        break;
    }
  }

  function onResize(e) {
    var el = e.srcElement;
    if (el.firstChild) {
      el.firstChild.style.width =  el.clientWidth + 'px';
      el.firstChild.style.height = el.clientHeight + 'px';
    }
  }

  var newCanvas = G_vmlCanvasManager_.init(null, canvasObject);

  // precompute "00" to "FF"
  var dec2hex = [];
  for (var i = 0; i < 16; i++) {
    for (var j = 0; j < 16; j++) {
      dec2hex[i * 16 + j] = i.toString(16) + j.toString(16);
    }
  }

  function createMatrixIdentity() {
    return [
      [1, 0, 0],
      [0, 1, 0],
      [0, 0, 1]
    ];
  }

  function matrixMultiply(m1, m2) {
    var result = createMatrixIdentity();

    for (var x = 0; x < 3; x++) {
      for (var y = 0; y < 3; y++) {
        var sum = 0;

        for (var z = 0; z < 3; z++) {
          sum += m1[x][z] * m2[z][y];
        }

        result[x][y] = sum;
      }
    }
    return result;
  }

  function copyState(o1, o2) {
    o2.fillStyle     = o1.fillStyle;
    o2.lineCap       = o1.lineCap;
    o2.lineJoin      = o1.lineJoin;
    o2.lineWidth     = o1.lineWidth;
    o2.miterLimit    = o1.miterLimit;
    o2.shadowBlur    = o1.shadowBlur;
    o2.shadowColor   = o1.shadowColor;
    o2.shadowOffsetX = o1.shadowOffsetX;
    o2.shadowOffsetY = o1.shadowOffsetY;
    o2.strokeStyle   = o1.strokeStyle;
    o2.arcScaleX_    = o1.arcScaleX_;
    o2.arcScaleY_    = o1.arcScaleY_;
  }

  function processStyle(styleString) {
    var str, alpha = 1;

    styleString = String(styleString);
    if (styleString.substring(0, 3) == "rgb") {
      var start = styleString.indexOf("(", 3);
      var end = styleString.indexOf(")", start + 1);
      var guts = styleString.substring(start + 1, end).split(",");

      str = "#";
      for (var i = 0; i < 3; i++) {
        str += dec2hex[Number(guts[i])];
      }

      if ((guts.length == 4) && (styleString.substr(3, 1) == "a")) {
        alpha = guts[3];
      }
    } else {
      str = styleString;
    }

    return [str, alpha];
  }

  function processLineCap(lineCap) {
    switch (lineCap) {
      case "butt":
        return "flat";
      case "round":
        return "round";
      case "square":
      default:
        return "square";
    }
  }

  /**
   * This class implements CanvasRenderingContext2D interface as described by
   * the WHATWG.
   * @param {HTMLElement} surfaceElement The element that the 2D context should
   * be associated with
   */
   function CanvasRenderingContext2D_(surfaceElement) {
    this.m_ = createMatrixIdentity();

    this.mStack_ = [];
    this.aStack_ = [];
    this.currentPath_ = [];

    // Canvas context properties
    this.strokeStyle = "#000";
    this.fillStyle = "#000";

    this.lineWidth = 1;
    this.lineJoin = "miter";
    this.lineCap = "butt";
    this.miterLimit = Z * 1;
    this.globalAlpha = 1;
    this.canvas = surfaceElement;

    var el = surfaceElement.ownerDocument.createElement('div');
    el.style.width =  surfaceElement.clientWidth + 'px';
    el.style.height = surfaceElement.clientHeight + 'px';
    el.style.overflow = 'hidden';
    el.style.position = 'absolute';
    surfaceElement.appendChild(el);

    this.element_ = el;
    this.arcScaleX_ = 1;
    this.arcScaleY_ = 1;
  };

  var contextPrototype = CanvasRenderingContext2D_.prototype;
  contextPrototype.clearRect = function() {
    this.element_.innerHTML = "";
    this.currentPath_ = [];
  };

  contextPrototype.beginPath = function() {
    // TODO: Branch current matrix so that save/restore has no effect
    //       as per safari docs.

    this.currentPath_ = [];
  };

  contextPrototype.moveTo = function(aX, aY) {
    this.currentPath_.push({type: "moveTo", x: aX, y: aY});
    this.currentX_ = aX;
    this.currentY_ = aY;
  };

  contextPrototype.lineTo = function(aX, aY) {
    this.currentPath_.push({type: "lineTo", x: aX, y: aY});
    this.currentX_ = aX;
    this.currentY_ = aY;
  };

  contextPrototype.bezierCurveTo = function(aCP1x, aCP1y,
                                            aCP2x, aCP2y,
                                            aX, aY) {
    this.currentPath_.push({type: "bezierCurveTo",
                           cp1x: aCP1x,
                           cp1y: aCP1y,
                           cp2x: aCP2x,
                           cp2y: aCP2y,
                           x: aX,
                           y: aY});
    this.currentX_ = aX;
    this.currentY_ = aY;
  };

  contextPrototype.quadraticCurveTo = function(aCPx, aCPy, aX, aY) {
    // the following is lifted almost directly from
    // http://developer.mozilla.org/en/docs/Canvas_tutorial:Drawing_shapes
    var cp1x = this.currentX_ + 2.0 / 3.0 * (aCPx - this.currentX_);
    var cp1y = this.currentY_ + 2.0 / 3.0 * (aCPy - this.currentY_);
    var cp2x = cp1x + (aX - this.currentX_) / 3.0;
    var cp2y = cp1y + (aY - this.currentY_) / 3.0;
    this.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, aX, aY);
  };

  contextPrototype.arc = function(aX, aY, aRadius,
                                  aStartAngle, aEndAngle, aClockwise) {
    aRadius *= Z;
    var arcType = aClockwise ? "at" : "wa";

    var xStart = aX + (mc(aStartAngle) * aRadius) - Z2;
    var yStart = aY + (ms(aStartAngle) * aRadius) - Z2;

    var xEnd = aX + (mc(aEndAngle) * aRadius) - Z2;
    var yEnd = aY + (ms(aEndAngle) * aRadius) - Z2;

    // IE won't render arches drawn counter clockwise if xStart == xEnd.
    if (xStart == xEnd && !aClockwise) {
      xStart += 0.125; // Offset xStart by 1/80 of a pixel. Use something
                       // that can be represented in binary
    }

    this.currentPath_.push({type: arcType,
                           x: aX,
                           y: aY,
                           radius: aRadius,
                           xStart: xStart,
                           yStart: yStart,
                           xEnd: xEnd,
                           yEnd: yEnd});

  };

  contextPrototype.rect = function(aX, aY, aWidth, aHeight) {
    this.moveTo(aX, aY);
    this.lineTo(aX + aWidth, aY);
    this.lineTo(aX + aWidth, aY + aHeight);
    this.lineTo(aX, aY + aHeight);
    this.closePath();
  };

  contextPrototype.strokeRect = function(aX, aY, aWidth, aHeight) {
    // Will destroy any existing path (same as FF behaviour)
    this.beginPath();
    this.moveTo(aX, aY);
    this.lineTo(aX + aWidth, aY);
    this.lineTo(aX + aWidth, aY + aHeight);
    this.lineTo(aX, aY + aHeight);
    this.closePath();
    this.stroke();
  };

  contextPrototype.fillRect = function(aX, aY, aWidth, aHeight) {
    // Will destroy any existing path (same as FF behaviour)
    this.beginPath();
    this.moveTo(aX, aY);
    this.lineTo(aX + aWidth, aY);
    this.lineTo(aX + aWidth, aY + aHeight);
    this.lineTo(aX, aY + aHeight);
    this.closePath();
    this.fill();
  };

  contextPrototype.createLinearGradient = function(aX0, aY0, aX1, aY1) {
    var gradient = new CanvasGradient_("gradient");
    return gradient;
  };

  contextPrototype.createRadialGradient = function(aX0, aY0,
                                                   aR0, aX1,
                                                   aY1, aR1) {
    var gradient = new CanvasGradient_("gradientradial");
    gradient.radius1_ = aR0;
    gradient.radius2_ = aR1;
    gradient.focus_.x = aX0;
    gradient.focus_.y = aY0;
    return gradient;
  };

  contextPrototype.drawImage = function (image, var_args) {
    var dx, dy, dw, dh, sx, sy, sw, sh;

    // to find the original width we overide the width and height
    var oldRuntimeWidth = image.runtimeStyle.width;
    var oldRuntimeHeight = image.runtimeStyle.height;
    image.runtimeStyle.width = 'auto';
    image.runtimeStyle.height = 'auto';

    // get the original size
    var w = image.width;
    var h = image.height;

    // and remove overides
    image.runtimeStyle.width = oldRuntimeWidth;
    image.runtimeStyle.height = oldRuntimeHeight;

    if (arguments.length == 3) {
      dx = arguments[1];
      dy = arguments[2];
      sx = sy = 0;
      sw = dw = w;
      sh = dh = h;
    } else if (arguments.length == 5) {
      dx = arguments[1];
      dy = arguments[2];
      dw = arguments[3];
      dh = arguments[4];
      sx = sy = 0;
      sw = w;
      sh = h;
    } else if (arguments.length == 9) {
      sx = arguments[1];
      sy = arguments[2];
      sw = arguments[3];
      sh = arguments[4];
      dx = arguments[5];
      dy = arguments[6];
      dw = arguments[7];
      dh = arguments[8];
    } else {
      throw "Invalid number of arguments";
    }

    var d = this.getCoords_(dx, dy);

    var w2 = sw / 2;
    var h2 = sh / 2;

    var vmlStr = [];

    var W = 10;
    var H = 10;

    // For some reason that I've now forgotten, using divs didn't work
    vmlStr.push(' <g_vml_:group',
                ' coordsize="', Z * W, ',', Z * H, '"',
                ' coordorigin="0,0"' ,
                ' style="width:', W, ';height:', H, ';position:absolute;');

    // If filters are necessary (rotation exists), create them
    // filters are bog-slow, so only create them if abbsolutely necessary
    // The following check doesn't account for skews (which don't exist
    // in the canvas spec (yet) anyway.

    if (this.m_[0][0] != 1 || this.m_[0][1]) {
      var filter = [];

      // Note the 12/21 reversal
      filter.push("M11='", this.m_[0][0], "',",
                  "M12='", this.m_[1][0], "',",
                  "M21='", this.m_[0][1], "',",
                  "M22='", this.m_[1][1], "',",
                  "Dx='", mr(d.x / Z), "',",
                  "Dy='", mr(d.y / Z), "'");

      // Bounding box calculation (need to minimize displayed area so that
      // filters don't waste time on unused pixels.
      var max = d;
      var c2 = this.getCoords_(dx + dw, dy);
      var c3 = this.getCoords_(dx, dy + dh);
      var c4 = this.getCoords_(dx + dw, dy + dh);

      max.x = Math.max(max.x, c2.x, c3.x, c4.x);
      max.y = Math.max(max.y, c2.y, c3.y, c4.y);

      vmlStr.push("padding:0 ", mr(max.x / Z), "px ", mr(max.y / Z),
                  "px 0;filter:progid:DXImageTransform.Microsoft.Matrix(",
                  filter.join(""), ", sizingmethod='clip');")
    } else {
      vmlStr.push("top:", mr(d.y / Z), "px;left:", mr(d.x / Z), "px;")
    }

    vmlStr.push(' ">' ,
                '<g_vml_:image src="', image.src, '"',
                ' style="width:', Z * dw, ';',
                ' height:', Z * dh, ';"',
                ' cropleft="', sx / w, '"',
                ' croptop="', sy / h, '"',
                ' cropright="', (w - sx - sw) / w, '"',
                ' cropbottom="', (h - sy - sh) / h, '"',
                ' />',
                '</g_vml_:group>');

    this.element_.insertAdjacentHTML("BeforeEnd",
                                    vmlStr.join(""));
  };

  contextPrototype.stroke = function(aFill) {
    var lineStr = [];
    var lineOpen = false;
    var a = processStyle(aFill ? this.fillStyle : this.strokeStyle);
    var color = a[0];
    var opacity = a[1] * this.globalAlpha;

    var W = 10;
    var H = 10;

    lineStr.push('<g_vml_:shape',
                 ' fillcolor="', color, '"',
                 ' filled="', Boolean(aFill), '"',
                 ' style="position:absolute;width:', W, ';height:', H, ';"',
                 ' coordorigin="0 0" coordsize="', Z * W, ' ', Z * H, '"',
                 ' stroked="', !aFill, '"',
                 ' strokeweight="', this.lineWidth, '"',
                 ' strokecolor="', color, '"',
                 ' path="');

    var newSeq = false;
    var min = {x: null, y: null};
    var max = {x: null, y: null};

    for (var i = 0; i < this.currentPath_.length; i++) {
      var p = this.currentPath_[i];

      if (p.type == "moveTo") {
        lineStr.push(" m ");
        var c = this.getCoords_(p.x, p.y);
        lineStr.push(mr(c.x), ",", mr(c.y));
      } else if (p.type == "lineTo") {
        lineStr.push(" l ");
        var c = this.getCoords_(p.x, p.y);
        lineStr.push(mr(c.x), ",", mr(c.y));
      } else if (p.type == "close") {
        lineStr.push(" x ");
      } else if (p.type == "bezierCurveTo") {
        lineStr.push(" c ");
        var c = this.getCoords_(p.x, p.y);
        var c1 = this.getCoords_(p.cp1x, p.cp1y);
        var c2 = this.getCoords_(p.cp2x, p.cp2y);
        lineStr.push(mr(c1.x), ",", mr(c1.y), ",",
                     mr(c2.x), ",", mr(c2.y), ",",
                     mr(c.x), ",", mr(c.y));
      } else if (p.type == "at" || p.type == "wa") {
        lineStr.push(" ", p.type, " ");
        var c  = this.getCoords_(p.x, p.y);
        var cStart = this.getCoords_(p.xStart, p.yStart);
        var cEnd = this.getCoords_(p.xEnd, p.yEnd);

        lineStr.push(mr(c.x - this.arcScaleX_ * p.radius), ",",
                     mr(c.y - this.arcScaleY_ * p.radius), " ",
                     mr(c.x + this.arcScaleX_ * p.radius), ",",
                     mr(c.y + this.arcScaleY_ * p.radius), " ",
                     mr(cStart.x), ",", mr(cStart.y), " ",
                     mr(cEnd.x), ",", mr(cEnd.y));
      }


      // TODO: Following is broken for curves due to
      //       move to proper paths.

      // Figure out dimensions so we can do gradient fills
      // properly
      if(c) {
        if (min.x == null || c.x < min.x) {
          min.x = c.x;
        }
        if (max.x == null || c.x > max.x) {
          max.x = c.x;
        }
        if (min.y == null || c.y < min.y) {
          min.y = c.y;
        }
        if (max.y == null || c.y > max.y) {
          max.y = c.y;
        }
      }
    }
    lineStr.push(' ">');

    if (typeof this.fillStyle == "object") {
      var focus = {x: "50%", y: "50%"};
      var width = (max.x - min.x);
      var height = (max.y - min.y);
      var dimension = (width > height) ? width : height;

      focus.x = mr((this.fillStyle.focus_.x / width) * 100 + 50) + "%";
      focus.y = mr((this.fillStyle.focus_.y / height) * 100 + 50) + "%";

      var colors = [];

      // inside radius (%)
      if (this.fillStyle.type_ == "gradientradial") {
        var inside = (this.fillStyle.radius1_ / dimension * 100);

        // percentage that outside radius exceeds inside radius
        var expansion = (this.fillStyle.radius2_ / dimension * 100) - inside;
      } else {
        var inside = 0;
        var expansion = 100;
      }

      var insidecolor = {offset: null, color: null};
      var outsidecolor = {offset: null, color: null};

      // We need to sort 'colors' by percentage, from 0 > 100 otherwise ie
      // won't interpret it correctly
      this.fillStyle.colors_.sort(function (cs1, cs2) {
        return cs1.offset - cs2.offset;
      });

      for (var i = 0; i < this.fillStyle.colors_.length; i++) {
        var fs = this.fillStyle.colors_[i];

        colors.push( (fs.offset * expansion) + inside, "% ", fs.color, ",");

        if (fs.offset > insidecolor.offset || insidecolor.offset == null) {
          insidecolor.offset = fs.offset;
          insidecolor.color = fs.color;
        }

        if (fs.offset < outsidecolor.offset || outsidecolor.offset == null) {
          outsidecolor.offset = fs.offset;
          outsidecolor.color = fs.color;
        }
      }
      colors.pop();

      lineStr.push('<g_vml_:fill',
                   ' color="', outsidecolor.color, '"',
                   ' color2="', insidecolor.color, '"',
                   ' type="', this.fillStyle.type_, '"',
                   ' focusposition="', focus.x, ', ', focus.y, '"',
                   ' colors="', colors.join(""), '"',
                   ' opacity="', opacity, '" />');
    } else if (aFill) {
      lineStr.push('<g_vml_:fill color="', color, '" opacity="', opacity, '" />');
    } else {
      lineStr.push(
        '<g_vml_:stroke',
        ' opacity="', opacity,'"',
        ' joinstyle="', this.lineJoin, '"',
        ' miterlimit="', this.miterLimit, '"',
        ' endcap="', processLineCap(this.lineCap) ,'"',
        ' weight="', this.lineWidth, 'px"',
        ' color="', color,'" />'
      );
    }

    lineStr.push("</g_vml_:shape>");

    this.element_.insertAdjacentHTML("beforeEnd", lineStr.join(""));

    this.currentPath_ = [];
  };

  contextPrototype.fill = function() {
    this.stroke(true);
  }

  contextPrototype.closePath = function() {
    this.currentPath_.push({type: "close"});
  };

  /**
   * @private
   */
  contextPrototype.getCoords_ = function(aX, aY) {
    return {
      x: Z * (aX * this.m_[0][0] + aY * this.m_[1][0] + this.m_[2][0]) - Z2,
      y: Z * (aX * this.m_[0][1] + aY * this.m_[1][1] + this.m_[2][1]) - Z2
    }
  };

  contextPrototype.save = function() {
    var o = {};
    copyState(this, o);
    this.aStack_.push(o);
    this.mStack_.push(this.m_);
    this.m_ = matrixMultiply(createMatrixIdentity(), this.m_);
  };

  contextPrototype.restore = function() {
    copyState(this.aStack_.pop(), this);
    this.m_ = this.mStack_.pop();
  };

  contextPrototype.translate = function(aX, aY) {
    var m1 = [
      [1,  0,  0],
      [0,  1,  0],
      [aX, aY, 1]
    ];

    this.m_ = matrixMultiply(m1, this.m_);
  };

  contextPrototype.rotate = function(aRot) {
    var c = mc(aRot);
    var s = ms(aRot);

    var m1 = [
      [c,  s, 0],
      [-s, c, 0],
      [0,  0, 1]
    ];

    this.m_ = matrixMultiply(m1, this.m_);
  };

  contextPrototype.scale = function(aX, aY) {
    this.arcScaleX_ *= aX;
    this.arcScaleY_ *= aY;
    var m1 = [
      [aX, 0,  0],
      [0,  aY, 0],
      [0,  0,  1]
    ];

    this.m_ = matrixMultiply(m1, this.m_);
  };

  /******** STUBS ********/
  contextPrototype.clip = function() {
    // TODO: Implement
  };

  contextPrototype.arcTo = function() {
    // TODO: Implement
  };

  contextPrototype.createPattern = function() {
    return new CanvasPattern_;
  };

  // Gradient / Pattern Stubs
  function CanvasGradient_(aType) {
    this.type_ = aType;
    this.radius1_ = 0;
    this.radius2_ = 0;
    this.colors_ = [];
    this.focus_ = {x: 0, y: 0};
  }

  CanvasGradient_.prototype.addColorStop = function(aOffset, aColor) {
    aColor = processStyle(aColor);
    this.colors_.push({offset: 1-aOffset, color: aColor});
  };

  function CanvasPattern_() {}

  // set up externs
  G_vmlCanvasManager = G_vmlCanvasManager_;
  CanvasRenderingContext2D = CanvasRenderingContext2D_;
  CanvasGradient = CanvasGradient_;
  CanvasPattern = CanvasPattern_;

    return newCanvas;
}

} // if

excanvas();










