var test_base = "http://squirrel.flymine.org/intermine-test";
var fly_base = "http://squirrel.flymine.org/flymine";
var query_path = "/service/query/results";

var test_query = {
    title: "Middle-Aged Employees",
    select: ["Employee.name", "Employee.age", "Employee.address.address", "Employee.department.manager.name"],
    from: "testmodel",
    where: [
        {path: "Employee.age", op: ">", value: 30},
        {path: "Employee.age", op: "<", value: 60},
        {path: "Employee.department.manager.name", op: "<", value: "M"}
    ]
};

var massive_query = {
    title: "All Genes",
    select: ["Gene.symbol", "Gene.primaryIdentifier", "Gene.length", "Gene.chromosomeLocation.start", "Gene.chromosomeLocation.end", "Gene.chromosomeLocation.strand"],
    from: "genomic"
};

var long_genes = {
    title: "Long Genes",
    select: ["Gene.symbol", "Gene.primaryIdentifier", "Gene.length", "Gene.chromosomeLocation.start", "Gene.chromosomeLocation.end", "Gene.chromosomeLocation.strand"],
    where: [
        {path: "Gene.length", op: ">", value: 25000},
        {path: "Gene.chromosomeLocation.start", op: "<", value: 10000000},
    ],
    from: "genomic"
};
var moderate_query = {
    title: "Moderate Query",
    select: ["Gene.symbol", "Gene.primaryIdentifier", "Gene.alleles.symbol", "Gene.alleles.alleleClass", "Gene.proteins.name"],
    from: "genomic",
    where: [{path: "Gene.symbol", op: "ONE OF", values: ["eve", "zen", "bib", "r", "h"]}]
};

function num_to_string(num, sep, every) {
    var num_as_string = num + "";
    var chars = num_as_string.split("");
    var ret = "";
    for (var i = chars.length - 1, c = 1; i >= 0; i--, c++) {
        var ch = chars[i];
        ret = ch + ret;
        if (c && i && (c % every == 0)) {
            ret = sep + ret;
        }
    }
    return ret;
}

function loadBox(pq, base, id) {
    var params = {format: "jsoncount", query: serializeQuery(pq)};
    var $box = jQuery('#' + id + ' div.results-box');
    jQuery('<span class="throbber"></span><span class="query-summary">' + pq.title + ':</span>').appendTo($box);
    $.ajax({
        type: "POST", 
        dataType: "json",
        data: params,
        url: base + query_path,
        success: function(json) {
            $box.find('.throbber').remove();
            jQuery("<div class='count query-summary'>" + num_to_string(json.count, ",", 3) + "</div>").prependTo($box);
            $box.find('.query-summary').click(function() {
                var $box = jQuery('#' + id + ' div.results-box');
                $box.animate({"width": "1000px", "height": "600px"}, function() {
                    var oldBackground = $box.css("background");
                    $box.css({"border-left": "0", "border-top": "0", "border-right": "0", "border-bottom": "0", background: "none"});
                    var dt_id = id + '_datatable';
                    if ($('#' + dt_id).length == 0) {
                        $box.append('<table id="' + dt_id + '"></table>');
                    }
                    $box.children('.query-summary').hide();
                    initTable(pq, dt_id, base, oldBackground);
                });
            });
        }
    });
}


$(function(){
    loadBox(test_query, test_base, "testtable");
    loadBox(massive_query, fly_base, "flytable");
    loadBox(moderate_query, fly_base, "flytable2");
    loadBox(long_genes, fly_base, "flytable3");
});

/**
    * Function to work around the decision to use the list of pairs format...
    */
function getParameter( params, name) {
    for ( var i = 0, l = params.length; i < l; i++ ) {
        if ( params[i].name == name ) {
            return params[i].value;
        }
    }
    return null;
}

/**
* Function to set parameters, when the are lists of pairs...
*/
function setParameter( params, name, value ) {
    // Overwrite existing value.
    for ( var i = 0, l = params.length; i < l; i++ ) {
        if ( params[i].name == name ) {
            params[i] = value;
            return;
        }
    }
    // Wasn't there - add it.
    params.push({"name": name, "value": value});
    return;
}

var cache = {};

/**
* Function to manage data caching
*/
function getDataFromCache(url, params, callback, id, pathquery) {
    var pipe_factor = 100;

    var query = jQuery.extend(true, {}, pathquery);
    var echo = getParameter(params, "sEcho");
    var start = getParameter(params, "iDisplayStart");
    var size = getParameter(params, "iDisplayLength");
    var end  = start + size;

    if (!cache[id]) {
        cache[id] = {lowerBound: -1};
    }

    cache[id].start = start;

    var needFreshData = false;

    if (cache[id].lowerBound < 0 || start < cache[id].lowerBound || end > cache[id].upperBound) {
        needFreshData = true;
    }

    params.push({name: "format", value: "jsondatatable"});

    var displayToView = [];
    for (var i = 0, l = getParameter(params, "iColumns"); i < l; i++) {
        displayToView[i] = getParameter(params, "mDataProp_" + i);
    }

    var noOfSortCols = getParameter(params, "iSortingCols");
    if (noOfSortCols) {
        var sort_cols = [];
        for (var i = 0; i < noOfSortCols; i++) {
            var displayed = getParameter(params, "iSortCol_" + i);
            sort_cols.push(query.select[displayToView[displayed]]);
            sort_cols.push(getParameter(params, "sSortDir_" + i));
        }
        query.sortOrder = sort_cols.join(" ");
    }

    /* Filters not yet implemented.
    var globalFilter = getParameter(params, "sSearch");
    if (globalFilter) {
        pq.filters = [{term: globalFilter}];
    }
    */

    // We need new data if the sort order has changed or if the global filter has changed. That is all.


    if (cache[id].lastRequest && !needFreshData) {
        // Compare sort-order
        var last_query = cache[id].lastQuery;
        if (query.sortOrder != last_query.sortOrder) {
            needFreshData = true;
        }
        //Compare constraints
        if (JSON.stringify(query.where) != JSON.stringify(last_query.where)) {
            needFreshData = true;
        }
        /* Filters not yet implemented - we can ignore them
        if (JSON.stringify(query.filters) != JSON.stringify(last_query.filters)) {
            needFreshData = true;
        }
        */
    }

    params.push({name: "query", value: serializeQuery(query)});
    cache[id].lastRequest = params.slice(); // Copy, don't alias.
    cache[id].lastQuery = jQuery.extend(true, {}, query); // Copy, don't alias.

    if (needFreshData) {
        if (start < cache[id].lowerBound) {
            start = start - (size * (pipe_factor - 1));
            if (start < 0) {
                start = 0;
            }
        }

        cache[id].lowerBound = start;
        cache[id].upperBound = start + (size * pipe_factor);
        cache[id].size = getParameter(params, "iDisplayLength");
        setParameter(params, "start", start);
        setParameter(params, "size", size * pipe_factor);

        $.ajax({
            type: "POST", 
            dataType: "json", 
            data: params, 
            url: url, 
            success: function(result) {
                cache[id].lastResult = jQuery.extend(true, {}, result);
                if (cache[id].lowerBound != cache[id].start) {
                    result.results.splice(0, cache[id].start - cache[id].lowerBound);
                }
                result.results.splice(cache[id].size, result.results.length);
                result.aaData = result.results;
                var box_id = id.substring(0, id.lastIndexOf("_"));
                $('#' + box_id).find('.query-summary.count').text(result.iTotalDisplayRecords);

                callback(result);
            }
        });
    } else {
        var result = jQuery.extend(true, {}, cache[id].lastResult);
        result.sEcho = echo;
        result.results.splice(0, start - cache[id].lowerBound);
        result.results.splice(size, result.results.length);
        result.aaData = result.results;
        callback(result);
    }
    return;
}

var getConstraintRemover = function(con, pq, dataTable, clicked) {
    return function() {
        pq.where = jQuery.grep(pq.where, function(inQuestion) {return JSON.stringify(con) != JSON.stringify(inQuestion);}); 
        dataTable.fnDraw(false); 
        var id = dataTable.attr("id");
        $(clicked).click();
        return false;
    };
};

var ESCAPE_DICT = {
    "<": '&lt;',
    ">": '&gt;',
    "<=": '&lt;=',
    ">=": '&gt;='
};

var binaryOps = {
    "=": "=",
    "!=": "!=",
    "<": "<",
    ">": ">",
    "<=": "<=",
    ">=": ">="
};

var nullOps = {
    "IS NULL": "is null",
    "IS NOT NULL": "is not null"
};

var multiOps = {
    "ONE OF": "one of", 
    "NONE OF": "none of"
};

var escapeOperator = function(operator) {
    if (operator in ESCAPE_DICT) {
        return ESCAPE_DICT[operator];
    } else {
        return operator;
    }
};

var getAdder = function(pq, button, path, dataTable, clicked) { return function() {
    var $new_constraint = jQuery('<span class="constraint">');
    var $select = jQuery('<select>').appendTo($new_constraint);
    var ops = ["=", "!=", "<", "<=", ">", ">=", "IS NULL", "IS NOT NULL"];
    for (var i = 0, l = ops.length; i < l; i++) {
        jQuery('<option>' + ops[i] + '</option>').appendTo($select);
    }
    var $input = jQuery('<input type="text" size="10">').appendTo($new_constraint);
    $select.change(function() {
        var chosen = $(this).val();
        $input.attr("disabled", !!chosen.match(/NULL$/));
    });
    var $save_button = newButton();            
    jQuery('<span>').addClass('ui-icon').addClass('ui-icon-circle-check').css("float", "left").appendTo($save_button);
    
    $save_button.appendTo($new_constraint);
    $new_constraint.insertBefore(button);
    $save_button.click(function() {
        var op = $select.val();
        var con = {path: path, op: op};
        if (!op.match(/NULL$/)) {
            con["value"] = $input.val();
        }
        if (typeof pq["where"] == "undefined") {
            pq["where"] = [];
        }
        pq.where.push(con);
        var id = dataTable.attr("id");
        dataTable.fnDraw(false); 
        $(clicked).click()
        return false;
    });

    var $cancelButton = newButton();
    newSpan('ui-icon ui-icon-circle-close').css("float", "left").appendTo($cancelButton);
    $cancelButton.appendTo($new_constraint).click(function() {
        $new_constraint.remove();
    });
    return false;
}};

var makeConstraint = function(whereClause) {
    var i = 0, len = 0, whereString = "<constraint ", attr = null, values = null;
    for (attr in whereClause) {
        if (attr === "values") {
            values = whereClause[attr];
        } else {
            whereString += attr + '="' + escapeOperator(whereClause[attr]) + '" ';
        }
    }
    if (values) {
        whereString += ">\n";
        len = values.length;
        for (i = 0;i < len;i++) {
            whereString += "    <value>" + values[i] + "</value>\n";
        }
        whereString += "  </constraint>";
    } else {
        whereString += "/>";
    }
    return whereString;
}

var itemState = {
    "pinned": 0, 
    "excluded": 1,
    "nothing": 2,
};

var getSelectorFn = function($conBox, item, state, selector, pq, clicked, dataTable, path) { return function() {
    var $span = $(this).find('span');
    $span.removeClass("ui-icon-bullet ui-icon-radio-off ui-icon-circle-close");
    var newState;
    if (state == itemState.nothing) {
        if ($conBox.find(".one-of").length) {
            newState = itemState.pinned;
        } else if ($conBox.find('.none-of').length) {
            newState = itemState.excluded;
        } 
    } else {
        if (($conBox.find(".one-of").length || $conBox.find(".none-of").length) && $conBox.find('.values .value').length > 1 ) {
            newState = itemState.nothing; 
        }
    }
    if (typeof newState == "undefined") {
        newState = (state + 1) % 3;
    }

    if (newState == itemState.pinned) {
        $span.addClass("ui-icon-bullet");
    } else if (newState == itemState.excluded) {
        $span.addClass("ui-icon-circle-close");
    } else {
        $span.addClass("ui-icon-radio-off");
    }

    $(selector).unbind('click').click(getSelectorFn($conBox, item, newState, selector, pq, clicked, dataTable, path));

    if (state == itemState.pinned) {
        removeItemFromMultiValue($conBox, "ONE OF", item, path, pq, dataTable, clicked);
    } else if (state == itemState.excluded) {
        removeItemFromMultiValue($conBox, "NONE OF", item, path, pq, dataTable, clicked);
    } 
    
    if (newState == itemState.excluded) {
        formatItemForNewState(path, "NONE OF", item, pq, dataTable, $conBox, clicked);
    } else if (newState == itemState.pinned) {
        formatItemForNewState(path, "ONE OF", item, pq, dataTable, $conBox, clicked);
    }
}};

var removeItemFromMultiValue =  function($conBox, op, item, path, pq, $dataTable, clicked) {
    var cssClass = op.toLowerCase().replace(" ", "-");
    var $con = $conBox.find("." + cssClass);
    $con.find('.value').each(function(val) {
        var $span = $(this);
        if ($span.text() == item) {
            $span.remove();
        }
    });
    var $button = $con.find('button.delete-confirm');
    if ($button.length < 1) {
        $button = jQuery('<button class="delete-confirm ui-state-default ui-corner-all constraint-deleter"><span class="ui-icon"></span></button>').appendTo($con);
    }
    $button.find('span').removeClass('ui-icon-circle-close').addClass('ui-icon-circle-check');
    $button.unbind('click').click(function() {
        var con = {path: path, op: op, values: $con.find('.value').map(function(i, e) { return $(e).text()}).get()};
        if (typeof pq["where"] == "undefined") {
            pq["where"] = [];
        }

        var needsInserting = con.values.length > 0;
        for (var i = 0, l = pq.where.length; i < l; i++) {
            var iterCon = pq.where[i];
            if (iterCon.path == path && iterCon.op == op) {
                if (con.values > 0) {
                    pq.where[i] = con;
                    needsInserting = false;
                } else {
                    pq.where.splice(i, 1);
                }
            }
        }
        if (needsInserting) {
            pq.where.push(con);
        }
        $dataTable.fnDraw(false); 
        $(clicked).click()
        return false;
    });
};

var formatItemForNewState = function(path, op, item, pq, $dataTable, $conBox, clicked) {
    var cssClass = op.toLowerCase().replace(" ", "-");
    var $con = $conBox.find("." + cssClass);
    if ($con.length < 1) {
        $con = jQuery('<span class="' + cssClass + ' constraint"><span class="op">' + op + '</span><span class="values"></span></span>');
        $con.appendTo($conBox);
    }
    var $new_con_value = jQuery('<span class="value">').text(item);
    $con.find(".values").append($new_con_value);
    var $button = $con.find('button.delete-confirm');
    if ($button.length < 1) {
        $button = jQuery('<button class="delete-confirm ui-state-default ui-corner-all constraint-deleter"><span class="ui-icon"></span></button>').appendTo($con);
    }
    $button.find('span').removeClass('ui-icon-circle-close').addClass('ui-icon-circle-check');
    $button.unbind('click').click(function() {
        var con = {path: path, op: op, values: $con.find('.value').map(function(i, e) { return $(e).text()}).get()};
        if (typeof pq["where"] == "undefined") {
            pq["where"] = [];
        }
        if (op == "ONE OF") { // Can safely remove all other constraints...
            pq.where = jQuery.grep(pq.where, function(con) {return con.path != path || con.op == op});
        }
        var needsInserting = true;
        for (var i = 0, l = pq.where.length; i < l; i++) {
            var iterCon = pq.where[i];
            if (iterCon.path == path && iterCon.op == op) {
                pq.where[i] = con;
                needsInserting = false;
            }
        }
        if (needsInserting) {
            pq.where.push(con);
        }

        $dataTable.fnDraw(false); 
        $(clicked).click()
        return false;
    });
}


var getConstraints = function(constraints) {
    var constraintsString = "", cl = constraints.length, i = 0;
    for (i = 0; i < cl; i++) {
        constraintsString += "  " +  makeConstraint(constraints[i]) + "\n";
    }
    return constraintsString;
};

var newButton = function() {
    return jQuery('<button class="ui-state-default ui-corner-all constraint-deleter">')
                .bind('mouseover', function() {$(this).addClass('ui-state-hover');})
                .bind('mouseout', function() {$(this).removeClass('ui-state-hover')});
};

var newSpan = function(cls, text) {
    if (text) {
        return jQuery('<span>').addClass(cls).text(text);
    } else {
        return jQuery('<span>').addClass(cls);
    }
        
};

function serializeQuery(query) {

    var xmlString = '<query '

    if ("title" in query) {
        xmlString += 'title="' + query.title + '" ';
    }

    xmlString += 'model="';
    if ("model" in query) {
        xmlString += query.model;
    } else if ("from" in query) {
        xmlString += query.from;
    } else {
        throw("No model in query");
    }

    xmlString += '" view="';
    if ("view" in query) {
        xmlString += query.view.join(" ");
    } else if ("select" in query) {
        xmlString += query.select.join(" ");
    } else {
        throw("No view in query");
    }
    xmlString += '" ';
    if ("constraintLogic" in query) {
        xmlString += 'constraintLogic="' + query.constraintLogic + '" ';
    }
    if ("sortOrder" in query) {
        xmlString += 'sortOrder="' + query.sortOrder + '" ';
    }
    xmlString += ">\n";
    if ("joins" in query) {
        for (var i = 0; i < query.joins.length; i++) {
            var join = query.joins[i];
            var joinString = '  <join ';
            
            for (attr in join) {
                joinString += (attr + '="' + join[attr] + '" ');
            }
            joinString += "/>\n";
            xmlString += joinString;
        }
    }
    if ("where" in query) {
        xmlString += getConstraints(query.where);
    }
    if ("constraints" in query) {
        xmlString += getConstraints(query.constraints);
    }
    xmlString += '</query>';
    return xmlString;
};

var getValueAdder = function($con, con, $dataTable, clicked) {
    return function() {
        var $button = $(this);
        $button.find("span").removeClass("ui-icon-circle-plus").addClass("ui-icon-circle-check");
        var $input = jQuery('<input type="text">').insertBefore($button);
        $button.unbind("click").click(function() {
            var newValue = $.trim($input.val());
            if (newValue.length > 0) {
                con.values.push(newValue);
                $dataTable.fnDraw(false);
                $(clicked).click();
            }
            $input.remove();
            $button.click(getValueAdder($con, con, $dataTable, clicked)).find("span").removeClass("ui-icon-circle-check").addClass("ui-icon-circle-plus");
            return false;
        });
        return false;
    };
};


var getOpChanger = function(con, $dataTable, clicked) {
    return function(value, settings) {
        if (con.op != value) {
            con.op = value;
            $dataTable.fnDraw(false);
            $(clicked).click();
        }
        return value;
    };
};

var getValueChanger = function(con, $dataTable, clicked, isNumeric) {
    return function(value) {
        var internalValue = isNumeric ? value.replace(",", "") : value;
        if (con.value != internalValue) {
            con.value = internalValue;
            $dataTable.fnDraw(false);
            $(clicked).click();
        }
        return value;
    };
};

tableInitialised = {};

function initTable(pq, id, base, bkg) {
    var numericTypes = ["int", "Integer", "double", "Double", "float", "Float"];
    var setup_params = {format: "jsontable", query: serializeQuery(pq)};
    var url = base + query_path; 
    if (tableInitialised[id]) {
        $('#' + id + '_wrapper').show();
    } else {
        $.ajax( {
            dataType: "json",
            type: "POST",
            url: url,
            data: setup_params,
            success: function(result) {
                var $dataTable = $('#' + id).dataTable( {
                    "aLengthMenu": [[10, 15, 25, 50, 100, -1], [10, 15, 25, 50, 100, "All"]],
                    "iDisplayLength": 15,
                    "bProcessing": true,
                    "oLanguage": {sProcessing: 'Loading data...'},
                    "bJQueryUI": true,
                    "sDom": '<"H"<"title"><"toolbar">RC<"clear">lfr>t<"F"ip>',
                    "sScrollY": "395px",
                    "bServerSide": true,
                    "sAjaxSource": url,
                    "sAjaxDataProp": "results",
                    "sPaginationType": "full_numbers",
                    "aoColumns": jQuery.map(pq.select, function(elem, idx) { 
                        var ret = {
                            "fnRender": function(obj) {
                                var col = obj.iDataColumn; 
                                if (typeof obj.aData[col] === "string") {
                                    return obj.aData[col];
                                } else {
                                    var ret = "";
                                    if (obj.aData[col].value == null) {
                                        ret += '<span class="null-value">';
                                    }
                                    ret += '<a href="' + base + obj.aData[col].url + '">';
                                    if (obj.aData[col].value == null) {
                                        ret += 'no value</a></span>';
                                    } else {
                                        ret += obj.aData[col].value + '</a>';
                                    }
                                    return ret
                                }
                            },
                            "sTitle": result.columnHeaders[idx].split(" > ").slice(1).join(" > ") 
                                + "<span id='summary_col-" + idx + "_" + id 
                                + "' class='summary_img ui-icon ui-icon-info' ></span>",
                            "sName": elem,
                            "bUseRendered": false,
                            "sType": (jQuery.inArray(result.viewTypes[idx], numericTypes) >= 0) ? "numeric" : "string",
                            "mDataProp": idx // + ".value", 
                        };
                        return ret;
                    }),
                    "fnServerData": function(src, data, callback) { getDataFromCache(src, data, callback, id, pq);}
                });

                $('#' + id + '_wrapper').find('.summary_img').click(function() {

                    console.log("Removing old popups");
                    $('#' + id + '_wrapper').find('.summary-popup').remove();
                    var buttonId = this.id
                    var clicked = this;
                    var parts = this.id.split("_", 3);
                    var col = parts[1].split("-").pop();
                    var colTitle = $('#' + buttonId).parent().text(); 
                    var summaryPath = pq.select[col];
                    var $popup = jQuery('<div class="summary-popup"></div>');
                    var $h3 = jQuery('<h3>').addClass("main-heading").text("Summary for " + colTitle);
                    var $topRightCloser = newSpan('ui-icon ui-icon-squaresmall-minus').css("float", "right");
                    var $topBox = jQuery('<div class="summary-header"></div>').appendTo($popup).append($h3);
                    var $conBox = jQuery('<div>').appendTo($popup).append("<h4>Constraints</h4>");
                    var constraintsOnThisField = [];
                    var pinned = [];
                    if (pq["where"]) {
                        for (var i = 0, l = pq.where.length; i < l; i++) {
                            if (pq.where[i].path == summaryPath) {
                                constraintsOnThisField.push(pq.where[i]);
                            }
                        }
                        for (var i = 0, l = constraintsOnThisField.length; i < l; i++) {
                            var con = constraintsOnThisField[i];
                            var $con = jQuery('<span class="constraint">');

                            var opDict;
                            if (con.op.match(/ OF/)) {
                                opDict = jQuery.extend({}, multiOps, true);
                            } else if (con.op.match(/NULL$/)) {
                                opDict = jQuery.extend({}, nullOps, true);
                            } else {
                                opDict = jQuery.extend({}, binaryOps, true);
                            }
                            opDict.selected = con.op;

                            newSpan("op", con.op).appendTo($con).editable(getOpChanger(con, $dataTable, clicked), 
                                {onblur: "submit", indicator: "Saving...", tooltip: "Click to edit", type: "select", data: opDict, submit: "OK"}
                            );
                            $con.addClass(con.op.toLowerCase().replace(" ", "-")); // "NONE OF" becomes "none-of"
                            if (con.value) {
                                var valIsNumber = (typeof con.value == "number");
                                newSpan("value", valIsNumber ? num_to_string(con.value, ",", 3) : con.value).appendTo($con).editable(getValueChanger(con, $dataTable, clicked, valIsNumber), {onblur: "submit", submit: "OK"});
                            }
                            if (con.values) {
                                var $values = newSpan("values").appendTo($con);
                                for (var j = 0; j < con.values.length; j++) {
                                    newSpan("value", con.values[j]).appendTo($values);
                                    if (con.op === "ONE OF") {
                                        pinned.push(con.values[j]);
                                    }
                                }
                                var $addValueButton = newButton();
                                newSpan('ui-icon ui-icon-circle-plus').css("float", "left").appendTo($addValueButton);
                                $addValueButton.click(getValueAdder($con, con, $dataTable, clicked)).appendTo($con);
                            }
                            var remover = getConstraintRemover(con, pq, $dataTable, clicked);

                            var $buttonContainer = newButton().click(remover).appendTo($con).addClass("delete-confirm");
                            newSpan('ui-icon ui-icon-circle-close').css("float", "left").appendTo($buttonContainer);
                            $conBox.append($con);
                        }
                    }

                    var $adder = newButton().appendTo($conBox);
                    var addFn = getAdder(pq, $adder, summaryPath, $dataTable, clicked);
                    $adder.click(addFn);

                    newSpan('ui-icon ui-icon-circle-plus').css("float", "right").appendTo($adder);

                    $centreBox = jQuery('<div>');
                    $centreBox.append('<p class="wait-text">Summarising...</p>');
                    $popup.append($centreBox);
                    var $removeButton = jQuery('<button class="summary-remover">Close</button>');
                    $removeButton.appendTo($popup);
                    $removeButton.click(function() {$popup.remove()});
                    $topRightCloser.click(function() {$popup.remove()});
                    var $datatable = $('#' + id + '_wrapper')   
                    $datatable.append($popup);
                    $popup.click(function() {return false;});
                    $popup.draggable({handle: ".main-heading"}).resizable({minHeight: $popup.height(), minWidth: $popup.width(), alsoResize: $centreBox});
                    $centreBox.resizable();

                    $.ajax({
                        type: "POST",
                        dataType: "json",
                        data: {format: "jsonrows", query: serializeQuery(pq), summaryPath: summaryPath, size: 100},
                        url: url,
                        success: function(results) {
                            var $table = jQuery('<table class="col-summary"></table>');
                            var propNames = [];
                            var isNumeric = false;
                            var top100 = results.results.slice();
                            var sliceSize = top100.length, totalSize = results.uniqueValues;
                            if (totalSize == 1 && typeof top100[0]["min"] != 'undefined') { 
                                // we have numeric summary results.
                                isNumeric = true;
                                propNames = ["min", "max", "average", "stdev"];
                                for (var i = 0, l = propNames.length; i < l; i++) {
                                    var $row = jQuery('<tr></tr>');
                                    var $left = jQuery('<td></td>');
                                    $left.text(propNames[i]);
                                    var $right = jQuery('<td class="summary-number"></td>');
                                    var value = parseFloat(results.results[0][propNames[i]]).toPrecision(2);
                                    $right.text(value);
                                    $row.append($left);
                                    $row.append($right);
                                    $table.append($row);
                                }
                            } else {
                                $centreBox.addClass("centre-box");
                                var $thead = jQuery('<thead>');
                                var $thRank  = jQuery("<th>").text("Rank");
                                var $thLeft  = jQuery("<th>").text("Item");
                                var $thRight = jQuery("<th>").text("Count");
                                $thead.append($thRank).append($thLeft).append($thRight);
                                $table.append($thead);
                                
                                for (var i = 0; i < sliceSize; i++) {
                                    var $row = jQuery('<tr></tr>');
                                    var $rank = jQuery('<td class="rank-cell">').text(i + 1);
                                    $row.append($rank);
                                    var $left = jQuery('<td><table class="inner-table"><tr></tr></table></td>');
                                    var item = top100[i].item;
                                    var isPinned = (jQuery.inArray(item, pinned) >= 0);
                                    var icon = isPinned ? "bullet" : "radio-off" ;
                                    var state = isPinned ? itemState.pinned : itemState.nothing;
                                    var $selector = jQuery('<td class="icon-cell"><span class="ui-icon ui-icon-' + icon + '"></span></td>').appendTo($left.find('tr'));
                                    var selectorFn = getSelectorFn($conBox, item, state, $selector, pq, clicked, $dataTable, summaryPath);
                                    $selector.click(selectorFn);
                                    jQuery('<td></td>').append((item == null) ? '<span class="null-value">no value</span>' : '<span class="item-value">' + item + '</span>').appendTo($left.find('tr'));
                                    $row.append($left);
                                    var $right = jQuery('<td class="summary-number"></td>');
                                    $right.text(top100[i].count);
                                    $row.append($right);
                                    $table.append($row);
                                }
                            }
                            var $subHeading = jQuery('<h3 class="sub-heading">');
                            if (! isNumeric && sliceSize < totalSize ) {
                                $subHeading.text("Showing the " + sliceSize + " most frequently occuring values of " 
                                    + totalSize + " unique values");
                                $topBox.after($subHeading);
                            }
                            if (isNumeric) {
                                $centreBox.empty().append($table);
                            } else {
                                var $tableBox = jQuery('<div class="summary-table-scrollcontainer"></div>');
                                $centreBox.empty().append($tableBox);
                                $tableBox.append($table);
                            }
                            $popup.resizable("option", "minHeight", $popup.height());
                        }
                    });
                    return false;
                });
                var $button = jQuery('<button>Hide table</button>');
                $button.click(function() {
                    var $datatable = $('#' + id + '_wrapper').hide();
                    var $box = $datatable.parent();
                    $box.children(".query-summary").show();
                    var borderStyle = "1px solid #AAAAAA";
                    $box.css({"border-left": borderStyle, "border-top": borderStyle, "border-right": borderStyle, "border-bottom": borderStyle, background: bkg});
                    $box.animate({width: "50%", height: "50px"});
                });
                var $dt = $('#' + id + '_wrapper');
                $dt.find('div.toolbar').append($button);

                var $xmlButton = jQuery('<button>').text("View as XML").click(function() {
                    var xml = serializeQuery(pq);
                    var $popup = jQuery('<div class="summary-popup"></div>');
                    var $h3 = jQuery('<h3>').addClass("main-heading").text("XML for " + pq.title);
                    var $topBox = jQuery('<div class="summary-header"></div>').appendTo($popup).append($h3);
                    var $centreBox = jQuery('<div>').appendTo($popup);
                    var $pre = jQuery('<pre>').addClass("xml").text(xml).appendTo($centreBox);
                    var $removeButton = jQuery('<button class="summary-remover">Close</button>');
                    $removeButton.appendTo($popup);
                    $removeButton.click(function() {$popup.remove()});
                    $dt.append($popup);
                    $popup.click(function() {return false;});
                    $popup.draggable({handle: ".main-heading"});
                    return false;
                });
                $dt.find('div.toolbar').append($xmlButton);

                var $tsvButton = jQuery('<button>').text("Download As TSV").click(function() {
                    var params = [
                        {name: "query", value: serializeQuery(pq)},
                        {name: "format", value: "tab"}
                    ];
                    var tsvUrl = url + "?" + jQuery.param(params);
                    window.location = tsvUrl;
                    return false;
                });
                $dt.find('div.toolbar').append($tsvButton);

                $dt.find('div.title').append('<span class="query-summary">' + pq.title + '</span>');

                $dt.click(function() {$dt.find('.summary-popup').remove()});



                tableInitialised[id] = true;
            }
        });
    }
};
