var test_base = "http://squirrel.flymine.org/intermine-test";
var fly_base = "http://squirrel.flymine.org/flymine";
var query_path = "/service/query/results";
var query_to_list_path = "/service/query/tolist/json";
var list_path = "/service/lists/json";
var modelPath = "/service/model/json";
var fieldPath = "/service/summaryfields";

var Query = function(query) {

    /** PRIVATE VARIABLES AND FUNCTIONS **/

    var escapeOperator = function(operator) {
        if (operator in ESCAPE_DICT) {
            return ESCAPE_DICT[operator];
        } else {
            return operator;
        }
    };

    var getConstraints = function(constraints) {
        var constraintsString = "", cl = constraints.length, i = 0;
        for (i = 0; i < cl; i++) {
            constraintsString += "  " +  makeConstraint(constraints[i]) + "\n";
        }
        return constraintsString;
    };

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
    };

    var serializeQuery = function(query) {
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


    /**
    * Dictionary for escaping illegal XML entities
    */
    var ESCAPE_DICT = {
        "<": '&lt;',
        ">": '&gt;',
        "<=": '&lt;=',
        ">=": '&gt;='
    };

    /** CONSTRUCTION **/

    _(this).extend(query);
    this.serialise = function() { return serializeQuery(this) };
    this.removeConstraint = function(con) { this.where = _(this.where).select(function(inQuestion) {return JSON.stringify(con) != JSON.stringify(inQuestion);});}; 
};

var Model = function(model) {

    /**
    * Get the ClassDescriptor for a path. If the path is a root-path, it 
    * returns the class descriptor for the class named, otherwise it returns 
    * the class the last part resolves to. If the last part is an attribute, this
    * function returns "undefined".
    */
    var _getCdForPath = function(model, path) {
        var parts = path.split(".");
        var cd = model.classes[parts.shift()];
        return _(parts).reduce(function (memo, fieldName) {
            var allFields = $.extend(true, {}, memo.attributes, memo.references, memo.collections);
            return model.classes[allFields[fieldName]["referencedType"]];
        }, cd);
    }


    /**
    * Get the full ancestry of a particular class.
    */
    var _getAncestorsOf = function(model, className) {
        var ancestors = model.classes[className]["extends"].slice();
        _(ancestors).each(function(a) {
            if (!a.match(/InterMineObject$/)) {
                ancestors = _.union(ancestors, model.getAncestorsOf(a));
            }
        });
        return ancestors;
    }

    _(this).extend(model);
    this.getCdForPath = function(path) { return _getCdForPath(this, path); };

    /**
     * Get the full ancestry for a given class name, from closest to most distant ancestor.
     */
    this.getAncestorsOf = function(className) { return _getAncestorsOf(this, className) };

    /**
    * Return the common type of two model classes, or null if there isn't one.
    */
    this.findCommonTypeOf = function(classA, classB) {
        if (classB == null || classA == null || classA == classB) {
            return classA;
        }
        var allAncestorsOfA = this.getAncestorsOf(classA);
        var allAncestorsOfB = this.getAncestorsOf(classB);
        // If one is a superclass of the other, return it.
        if (_(allAncestorsOfA).include(classB)) {
            return classB;
        }
        if (_(allAncestorsOfB).include(classA)) {
            return classA;
        }
        // Return the first common ancestor
        return _.intersection(allAncestorsOfA, allAncestorsOfB).shift();
    };

    /**
    * Return the common type of 0 or more model classes, or null if there is none.
    *
    * @param model The data model for this service.
    * @classes {String[]} classes the model classes to try and get a common type of.
    */
    this.findCommonTypeOfMultipleClasses = function(classes) {
        return _.reduce(classes, _.bind(this.findCommonTypeOf, this), classes.pop());
    };
};

var ResultTable = function(query, base, id, key) {

    var loadModel = function(base, continuation) {
        return loadResource(base, modelPath, "model", InterMine.models, Model, continuation);
    };

    var loadSummaryFields = function(base, continuation) {
        return loadResource(base, fieldPath, "classes", InterMine.summaryFields, null, continuation);
    };

    var getStorer = function(storage, base, key, cls, continuation) {
        return function(result) {
            var toStore = (cls) ? new cls(result[key]) : result[key];
            storage[base] = toStore;
            if (continuation) {
                return continuation();
            } else {
                return false;
            }
        };
    };

    var loadResource = function(base, path, key, storage, cls, continuation) {
        if (storage[base]) {
            if (continuation) {
                return continuation();
            } else {
                return false;
            }
        } else {
            var storer = getStorer(storage, base, key, cls, continuation);
            $.getJSON(base + path, storer);
            return false;
        }
    }

    /**
    * Load a table for displaying a query into an element with the given id.
    */
    var loadQuery = function(pq, base, id, token) {
        loadModel(base, function() {
            loadSummaryFields(base, function() {
                loadBox(new Query(pq), base, id, token);
            });
        });
    };

    loadQuery(query, base, id, key);

    this.id = id;
    this.getTable = function() { return $('#' + id).dataTable() };
    this.close = function() { $('#' + this.id + "_datatable_wrapper").find('.hide-button').click(); };
    this.open = function() { $('#' + this.id).find(".query-summary").click(); };

};

InterMine = {
    "ResultTable": ResultTable,
    "Model": Model,
    "Query": Query,
    "models": {},
    "summaryFields": {}
};

//if (typeof console == "undefined") { window.console = {"log": function() {}} };
//

/**
 * Shortcut for _(sth).chain()
 */
function __(sth) {
    return _(sth).chain();
}

function returnFalse() {
    return false;
}

function tapper(x) {console.log(x)}

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


/**
 * Adjust the view of the query by adding all the non-selected
 * attribute paths to the view. These paths are appended to the current
 * view list.
 */
function addAttributesToQuery(model, pq) {
    pq.select = _.union(pq.select, __(pq.select)
        .map(function(p) {return p.substring(0, p.lastIndexOf("."))}).uniq()
        .map(function(p) {
            return __(model.getCdForPath(p).attributes)
                .keys().without("id")
                .map(function(attr) {return p + "." + attr})
                .value();
        })
        .flatten()
        .value());
}

function loadBox(pq, base, id, token) {
    var params = {format: "jsoncount", query: pq.serialise()};
    var $tableContainer = $('#' + id).addClass("table-container");
    var $box = jQuery('<div class="results-box ui-widget-header ui-corner-all">').appendTo($tableContainer);
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
                $box.animate({"width": "62.5em", "height": "600px"}, function() {
                    var oldBackground = $box.css("background");
                    $box.removeClass("ui-widget-header");
                    $box.css({"border-left": "0", "border-top": "0", "border-right": "0", "border-bottom": "0", background: "none"});
                    var dt_id = id + '_datatable';
                    if ($('#' + dt_id).length == 0) {
                        $box.append('<table id="' + dt_id + '"></table>');
                    }
                    $box.children('.query-summary').hide();
                    initTable(pq, dt_id, base, oldBackground, token);
                });
            });
        }
    });
}

/**
 * Function to work around the decision to use the list of pairs format...
 */
function getParameter( params, name) {
    return __(params).select(function(p) {return p.name == name}).pluck('value').first().value();
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
    var pipe_factor = 10;

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

    if (cache[id].lowerBound < 0 || start < cache[id].lowerBound || end > cache[id].upperBound || size < 0) {
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

    // We need new data if the query is different, by different:
    //  * the constraints have changed, or
    //  * the sort order has changed, or 
    //  * the filter has changed, or 
    //  * the underlying view has changed.

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
        // Compare the underlying view (NOT the visible columns)
        if (query.select.join(" ") != last_query.select.join(" ")) {
            needFreshData = true;
        }
        /* Filters not yet implemented - we can ignore them
        if (JSON.stringify(query.filters) != JSON.stringify(last_query.filters)) {
            needFreshData = true;
        }
        */
    }

    params.push({name: "query", value: query.serialise()});
    cache[id].lastRequest = params.slice(); // Copy, don't alias.
    cache[id].lastQuery = jQuery.extend(true, {}, query); // Copy, don't alias.

    if (needFreshData) {
        if (start < cache[id].lowerBound) {
            start = start - (size * (pipe_factor - 1));
            if (start < 0 || size < 0) {
                start = 0;
            }
        }

        cache[id].lowerBound = start;
        cache[id].upperBound = start + (size * pipe_factor);
        cache[id].size = getParameter(params, "iDisplayLength");
        setParameter(params, "start", start);
        if (size > 0) {
            setParameter(params, "size", size * pipe_factor);
        } else {
            setParameter(params, "size", "");// All
        }

        $.ajax({
            type: "POST", 
            dataType: "json", 
            data: params, 
            url: url, 
            success: function(result) {
                cache[id].lastResult = jQuery.extend(true, {}, result);
                if (cache[id].lowerBound != cache[id].start && size > 0) {
                    result.results.splice(0, cache[id].start - cache[id].lowerBound);
                }
                if (size > 0) { // If we want all, do not splice the results.
                    result.results.splice(cache[id].size, result.results.length);
                }
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

/** 
 * Get a function that will remove a constraint from a pathquery.
 *
 * @param con The constraint to remove
 * @param pq The Query to remove it from
 * @param pq.where The set of constraints on the query.
 * @param dataTable The data table this all belongs to
 * @param [clicked] The (i) button that opened the popup.
 */
var getConstraintRemover = function(con, pq, dataTable, clicked) {
    return function() {
        pq.removeConstraint(con);
        refreshPopup(dataTable, clicked);
        return false;
    };
};


/**
 * Binary attribute constraint operators
 */
var binaryOps = {
    "=": "=",
    "!=": "!=",
    "<": "<",
    ">": ">",
    "<=": "<=",
    ">=": ">="
};

/**
 * Null constrain operators
 */
var nullOps = {
    "IS NULL": "is null",
    "IS NOT NULL": "is not null"
};

/**
 * Multi-value constraint opertators
 */
var multiOps = {
    "ONE OF": "one of", 
    "NONE OF": "none of"
};

/**
 * Get a function that adds a form for adding a new constraint to a query to a constraint box
 */
var getAdder = function(pq, box, path, dataTable, clicked) { return function() {
    var $new_constraint = jQuery('<span class="constraint">');
    var $select = jQuery('<select style="width: 4em">').appendTo($new_constraint);
    var ops = ["=", "!=", "<", "<=", ">", ">=", "IS NULL", "IS NOT NULL"];
    for (var i = 0, l = ops.length; i < l; i++) {
        jQuery('<option>' + ops[i] + '</option>').appendTo($select);
    }
    var $input = jQuery('<input type="text">').appendTo($new_constraint);
    $select.change(function() {
        var chosen = $(this).val();
        if (chosen.match(/NULL$/)) {
            $(this).css({width: "10em"});
            $input.hide();
        } else {
            $(this).css({width: "4em"});
            $input.show();
        }
    });
    var $save_button = newButton();            
    jQuery('<span>').addClass('ui-icon').addClass('ui-icon-circle-check').css("float", "left").appendTo($save_button);
    jQuery('<span>').text("Apply").css("float", "right").appendTo($save_button);
    
    $save_button.appendTo($new_constraint);
    $new_constraint.appendTo(box);
    $save_button.click(function() {
        var op = $select.val();
        var con = {path: path, op: op};
        if (!op.match(/NULL$/)) {
            var val = $.trim($input.val());
            if (val.length) {
                con.value = val;
            } else {
                return false;
            }
        }
        if (typeof pq["where"] == "undefined") {
            pq["where"] = [];
        }
        pq.where.push(con);
        refreshPopup(dataTable, clicked);
        return false;
    });

    return false;
}};

var itemState = {
    "pinned": 0, 
    "excluded": 1,
    "nothing": 2,
};

var getSelectorFn = function($conBox, item, state, selector, pq, clicked, dataTable, path) { return function(ev) {
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
    
    if ($con.find('.value').length < 1 && ((! "where" in pq) ||  $.grep(pq.where, function(con) {return con.path == path && con.op == op}).length < 1)) {
        $con.remove();
        return false;
    }
    var $button = $con.find('button.delete-confirm');
    if ($button.length < 1) {
        $button = jQuery('<button class="delete-confirm ui-state-default ui-corner-all constraint-deleter"><span class="ui-icon"></span></button>').appendTo($con);
    }
    $con.addClass("unapplied");
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
        refreshPopup($dataTable, clicked);
        return false;
    });
};

var formatItemForNewState = function(path, op, item, pq, $dataTable, $conBox, clicked) {
    var cssClass = op.toLowerCase().replace(" ", "-");
    var $con = $conBox.find("." + cssClass);
    if ($con.length < 1) {
        $con = jQuery('<span class="' + cssClass + ' constraint unapplied"><span class="op">' + op + '</span><span class="values"></span></span>');
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

        refreshPopup($dataTable, clicked);
        return false;
    });
}

function refreshPopup($dataTable, clicked) {
    $dataTable.fnDraw(false); 
    $("#" + $dataTable.attr("id") + "_wrapper").find(".column-name").text("Update");
    $(clicked).click()
}

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

var getValueAdder = function($con, con, $dataTable, clicked) {
    return function() {
        var $button = $(this);
        $button.find("span").removeClass("ui-icon-circle-plus").addClass("ui-icon-circle-check");
        var $input = jQuery('<input type="text">').appendTo($con);
        $button.unbind("click").click(function() {
            var newValue = $.trim($input.val());
            if (newValue.length > 0) {
                con.values.push(newValue);
                refreshPopup($dataTable, clicked);
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
            refreshPopup($dataTable, clicked);
        }
        return value;
    };
};

var getValueChanger = function(con, $dataTable, clicked, isNumeric) {
    return function(value) {
        var internalValue = isNumeric ? value.replace(",", "") : value;
        if (con.value != internalValue) {
            con.value = internalValue;
            refreshPopup($dataTable, clicked);
        }
        return value;
    };
};

/**
 * Build a constraint display box
 */
function constructConstraintElem(con, pq, $dataTable, clicked, pinned) {
    pinned = pinned || [];
    var $con = jQuery('<span class="constraint">');
    var remover = getConstraintRemover(con, pq, $dataTable, clicked);
    var $buttonContainer = newButton().click(remover).appendTo($con).addClass("delete-confirm");
    newSpan('ui-icon ui-icon-circle-close').css("float", "left").appendTo($buttonContainer);
    if (con.values) {
        var $addValueButton = newButton();
        newSpan('ui-icon ui-icon-circle-plus').css("float", "left").appendTo($addValueButton);
        $addValueButton.click(getValueAdder($con, con, $dataTable, clicked)).appendTo($con);
    }

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
        _(con.values).each(function(val) {
            newSpan("value", val).appendTo($values);
            if (con.op === "ONE OF") {
                pinned.push(val);
            }
        });
    }
    return $con;
}

/**
 * Return the callback for the (i) button on the column headers.
 */
function getColumnSummariser(id, pq, url) { return function(ev) {
    var $wrapper = $('#' + id + '_wrapper');
    var currentSummaryTitle = $wrapper.find(".column-name").text(); 

    $wrapper.find('.summary-popup').remove();
    $wrapper.find('th').removeClass("ui-state-hover").removeClass("border-collapse-fix");

    var buttonId = this.id
    var clicked = this;
    var parts = this.id.split("_", 3);
    var col = parts[1].split("-").pop();
    var colTitle = $('#' + buttonId).parent().text(); 
    var summaryPath = pq.select[col];
    var $popup = jQuery('<div class="summary-popup"></div>');
    var $colname = jQuery('<div>').addClass("column-name").text(colTitle).appendTo($popup);
    if ($colname.text() == currentSummaryTitle) {
        return false;
    }
    var $conBox = jQuery('<div>').appendTo($popup).append("<h4>Constrain this Column</h4>");
    var constraintsOnThisField = [];
    var pinned = [];
    $dataTable = $('#' + id).dataTable();
    if (pq["where"]) {
        var size = __(pq.where).select(function(con) {return con.path == summaryPath})
                               .map(function(con) {$conBox.append(constructConstraintElem(con, pq, $dataTable, clicked, pinned)); return con;})
                               .size().value();
        if (size > 0) {
            $conBox.find("h4").text(size + " Constraint" + ((size > 1) ? "s" : "") + " on this Column");
        }
    }

    var addFn = getAdder(pq, $conBox, summaryPath, $dataTable, clicked);
    addFn();

    $centreBox = jQuery('<div>');
    $centreBox.append('<p class="wait-text">Summarising...</p>');
    $popup.append($centreBox);
    var $removeButton = jQuery('<button class="summary-remover">Close</button>');
    $removeButton.appendTo($popup);
    $removeButton.click(function() {$popup.remove()});
    var $datatable = $('#' + id + '_wrapper')   
    $datatable.append($popup);
    $popup.click(function() {return false;});
    var handles = "s, se, e";

    var pos = $datatable.position();
    var pos2 = $(ev.currentTarget.parentNode).position();
    var $colTh = $(ev.currentTarget.parentNode.parentNode);
    var x = pos2.left - pos.left + 1;
    var y = $datatable.find('.fg-toolbar').outerHeight() + $datatable.find('.dataTables_scrollHeadInner').outerHeight();
    $colTh.addClass("ui-state-hover").prev().addClass("border-collapse-fix");
    $popup.css({"left": x, "top": y});
    if ($popup.width() + x > $datatable.width()) {
        $popup.css({"left": x - 1 - $popup.width() + $colTh.outerWidth()});
        console.log("Switching to sw");
        handles = "s, sw, w";
    }
    if ($popup.width() < $colTh.width()) {
        $popup.css({width: $colTh.outerWidth() - 1});
    }
    $popup.resizable({handles: handles, minHeight: $popup.height(), minWidth: $popup.width(), alsoResize: $centreBox});
    if (handles.match(/sw/)) {
        $popup.find('.ui-resizable-sw').addClass("ui-icon ui-icon-triangle-1-sw");
    }
    $centreBox.resizable();

    $.ajax({
        type: "POST",
        dataType: "json",
        data: {format: "jsonrows", query: pq.serialise(), summaryPath: summaryPath, size: 100},
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
                var $thLeft  = jQuery("<th>").text("Value");
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
            var $subHeading = jQuery('<h4 class="sub-heading">').insertBefore($centreBox);
            if (isNumeric) {
                $centreBox.empty().append($table);
            } else {
                if (sliceSize < totalSize) {
                    $subHeading.text("The " + sliceSize + " most frequently occuring of " 
                        + totalSize + " unique values");
                } else {
                    $subHeading.text(sliceSize + " unique values");
                }
                var $tableBox = jQuery('<div class="summary-table-scrollcontainer"></div>');
                $centreBox.empty().append($tableBox);
                $tableBox.append($table);
            }
            $popup.resizable("option", "minHeight", $popup.height());
        }   
    });
    return false;
}};

/**
 * Get the callback for each checkbox in the table
 *
 * @param {String} id The id of the datatable this checkbox is within
 * @param {String} base the base url of the service this table belongs to.
 */
function getItemChooser(id, base) { return function() {
    var $checkbox = $(this);
    var $wrapper = $('#' + id + '_wrapper');
    var checked = !!$checkbox.attr("checked");
    var model = InterMine.models[base];
    if (!chosenListIds[id]) {
        chosenListIds[id] = [];
        typeOfObj[id] = {};
    }
    if (checked) {
        chosenListIds[id] = _.union(chosenListIds[id], [$checkbox.attr("obj_id")]);
        typeOfObj[id][$checkbox.attr("obj_id")] = $checkbox.attr("obj_class");
    } else {
        chosenListIds[id] = _(chosenListIds[id]).without($checkbox.attr("obj_id"));
        delete typeOfObj[id][$checkbox.attr("obj_id")];
    }
    var itemCount = chosenListIds[id].length;
    $wrapper.find('.list-creation-info').children('.selected-count').text(itemCount);
    var types = __(typeOfObj[id]).values().compact().uniq().value();
    var type = (types.length) ? model.findCommonTypeOfMultipleClasses(types) : "item";
    $wrapper.find('.list-creation-info').children('.selected-types').text(pluralise(type, itemCount));
    $wrapper.find('button.list-maker').attr("disabled", chosenListIds[id].length < 1);
    $wrapper.find('.list-chooser').each(function(elem) {
        var $thisCheckbox = $(this);
        var thisType = $thisCheckbox.attr("obj_class");
        var commonType = model.findCommonTypeOfMultipleClasses(__(typeOfObj[id]).values().compact().uniq().union([thisType]).value());
        if ($thisCheckbox.attr("obj_id") == $checkbox.attr("obj_id")) {
            $thisCheckbox.attr("checked", checked);
        }
        $thisCheckbox.attr("disabled", !commonType);
    });
    updateListPreview(id, base);
}};

function updateListPreview(id, base) {
    var itemCount = chosenListIds[id].length;
    var model = InterMine.models[base];
    var defaultViews = InterMine.summaryFields[base];
    var types = __(typeOfObj[id]).values().compact().uniq().value();
    var type = (types.length) ? model.findCommonTypeOfMultipleClasses(types) : "item";
    var $wrapper = $('#' + id + "_wrapper");

    if (itemCount > 0) {
        var query = new Query({
            // Don't include any references in the default view: they may be null and return no row
            select: _(defaultViews[type]).select(function(v) { return !v.match(/\.[^\.]+\./); }),
            from: model.name,
            where: [
                {path: type + ".id", op: "ONE OF", values: chosenListIds[id].slice()}
            ]
        });
        if (query.select.length == 0) {
            query.select = [type + ".id"];
        }
        $.ajax({
            url: base + query_path,
            type: "POST",
            dataType: "json",
            data: {query: query.serialise(), format: "jsonrows"},
            success: function(resultset) {
                $ul = $wrapper.find('.list-items').empty();
                _(resultset.results).each(function(row) {
                    var $li = $('<li>').appendTo($ul);
                    __(row).keys().each(function(idx) {
                        $li.append($('<span>').attr("title", resultset.columnHeaders[idx]).text(row[idx].value));
                        $li.append(" ");
                    });
                });
            }
        });
    } else {
        $wrapper.find(".list-items").empty();
    }
}



/**
 * Very naïve English word pluralisation algorithm
 *
 * @param {String} word The word to pluralise.
 * @param {Number} count The number of items this word represents.
 */
function pluralise(word, count) {
    return (count == 1) 
        ? word 
        : ((word.match(/(s|x|ch)$/)) 
                ? word + "es" 
                : (word.match(/[^aeiou]y$/) 
                    ? word.replace(/y$/, 'ies')
                    : word + "s"));
}


function getListMaker(base, id, $h3, token, remover) { return function() {
    var model = InterMine.models[base];
    var ids = chosenListIds[id].slice();
    var types = __(typeOfObj[id]).values().compact().uniq().value();
    var type = model.findCommonTypeOfMultipleClasses(types);
    var typePath = type + ".id";
    var query = new Query({
        select: [typePath],
        from: model.name, 
        where: [
            { path: typePath, op: "ONE OF", values: ids }
        ]
    });
    var listUploadUrl = base + query_to_list_path;
    var data = {
        query: query.serialise(),
        listName: $h3.find('.new-list-name').text(),
        format: "json",
        token: token
    };
    $.ajax({
        type: "POST",
        dataType: "json",
        data: data,
        url: listUploadUrl,
        success: function(result) {
            alert("Created new list named " + result.listName + " with " + result.listSize + " items");
            remover();
        }
    });
}}

function getListPopupRemover($popup, $dt, id, $button) {return function() {
    $popup.remove(); 
    listCreationState[id] = false;
    $dt.find('td input').hide();
    $button.attr("disabled", false);
}};

tableInitialised = {};
var numericTypes = ["int", "Integer", "double", "Double", "float", "Float"];
var listCreationState = {};
var chosenListIds = {};
var typeOfObj = {};

function initTable(pq, id, base, bkg, token) {
    var initialViewLength = pq.select.length;
    var model = InterMine.models[base];
    addAttributesToQuery(model, pq);
    var setup_params = {format: "jsontable", query: pq.serialise()};
    var url = base + query_path; 
    if (tableInitialised[id]) {
        $('#' + id + '_wrapper').show().parent().parent().find('.constraints-hider').slideDown();
    } else {
        $.ajax( {
            dataType: "json",
            type: "POST",
            url: url,
            data: setup_params,
            success: function(result) {
                var $dataTable = $('#' + id).dataTable( {
                    "aLengthMenu": [[10, 15, 20, 25, 50, 100, -1], [10, 15, 20, 25, 50, 100, "All"]],
                    "iDisplayLength": 20,
                    "bProcessing": true,
                    "oLanguage": {sProcessing: 'Loading data...'},
                    "bJQueryUI": true,
                    "sDom": '<"H"<"title"><"toolbar">RC<"clear">lfr>t<"F"ip>',
                    "sScrollY": "450px",
                    "bServerSide": true,
                    "sAjaxSource": url,
                    "sAjaxDataProp": "results",
                    "sPaginationType": "full_numbers",
                    "fnDrawCallback": function() {
                        var $dataTable = $('#' + id + '_wrapper');
                        var $constraints = $('#' + id + '_constraints').empty();
                        $dataTable.find('.summary_img').unbind("click").click(getColumnSummariser(id, pq, url));
                        $dataTable.find('.list-chooser').unbind("click").click(getItemChooser(id, base));
                        if (listCreationState[id]) {
                            $dataTable.find('.list-chooser').show().each(function(idx, elem) {
                                $(elem).attr("checked", _(chosenListIds[id]).include($(elem).attr("obj_id")));
                            });
                        }
                        __(pq.where).groupBy(function(con) {return con.path}).each(function(constraints, path) {
                            var title = result.columnHeaders[_(pq.select).indexOf(path)];
                            $("<h4>").text(title).appendTo($constraints);
                            var $groupBox = $('<div>').appendTo($constraints);
                            _(constraints).each(function(con) {
                                $groupBox.append(constructConstraintElem(con, pq, $('#' + id).dataTable(), $dataTable));
                            });
                        });
                        if (typeof pq.where == "undefined") {
                            pq.where = [];
                        }
                        $constraints.parent().find(".constraint-count").text(pq.where.length);
                        if (pq.where.length != 1) {
                            $constraints.parent().find(".plural-s").show();
                        } else {
                            $constraints.parent().find(".plural-s").hide();
                        }
                    },
                    "aoColumns": jQuery.map(pq.select, function(elem, idx) { 
                        var ret = {
                            "bVisible": idx < initialViewLength,
                            "fnRender": function(obj) {
                                var col = obj.iDataColumn; 
                                if (typeof obj.aData[col] === "string") {
                                    return obj.aData[col];
                                } else {
                                    var ret = "<input class='list-chooser' type='checkbox' style='display:none;' obj_id='" + obj.aData[col].id + "' obj_class='" + obj.aData[col]["class"] + "'>";
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
                                + "' class='summary_img ui-icon ui-icon-info' title='Get column summary'></span>",
                            "sName": elem,
                            "bUseRendered": false,
                            "sType": (jQuery.inArray(result.viewTypes[idx], numericTypes) >= 0) ? "numeric" : "string",
                            "mDataProp": idx // + ".value", 
                        };
                        return ret;
                    }),
                    "fnServerData": function(src, data, callback) { getDataFromCache(src, data, callback, id, pq);}
                });

                var $button = jQuery('<button class="hide-button">Hide table</button>');
                $button.click(function() {
                    var $datatable = $('#' + id + '_wrapper').hide();
                    $datatable.parent().parent().find('.constraints-hider').slideUp();
                    var $box = $datatable.parent().addClass("ui-widget-header");
                    $box.children(".query-summary").show();
                    var borderStyle = "1px solid #AAAAAA";
                    $box.css({"border-left": borderStyle, "border-top": borderStyle, "border-right": borderStyle, "border-bottom": borderStyle, background: bkg});
                    $box.animate({width: "50%", height: "50px"});
                });
                var $dt = $('#' + id + '_wrapper');
                $dt.find('div.toolbar').append($button);

                var $xmlButton = jQuery('<button>').text("View as XML").click(function() {
                    var visibleQuery = getVisibleQuery(pq, $dataTable, id);
                    var xml = visibleQuery.serialise();
                    var $popup = jQuery('<div class="summary-popup"></div>');
                    var $h3 = jQuery('<h3>').addClass("main-heading").text("XML for " + visibleQuery.title);
                    var $topBox = jQuery('<div class="summary-header"></div>').appendTo($popup).append($h3);
                    var $centreBox = jQuery('<div>').appendTo($popup);
                    var $pre = jQuery('<pre>').addClass("xml").text(xml).appendTo($centreBox);
                    var $removeButton = jQuery('<button class="summary-remover">Close</button>');
                    $removeButton.appendTo($popup);
                    $removeButton.click(function() {$popup.remove()});
                    $dt.append($popup);
                    $popup.click(function() {return false;});
                    $popup.draggable({handle: ".main-heading"}).resizable({minWidth: $popup.width(), minHeight: $popup.height(), alsoResize: $popup.find("pre")});
                    return false;
                });
                $dt.find('div.toolbar').append($xmlButton);

                var $tsvButton = jQuery('<button>').text("Download As TSV").click(function() {
                    var tsvQuery = getVisibleQuery(pq, $dataTable, id);
                    var params = [
                        {name: "query", value: tsvQuery.serialise()},
                        {name: "format", value: "tab"}
                    ];
                    var tsvUrl = url + "?" + jQuery.param(params);
                    window.location = tsvUrl;
                    return false;
                });
                $dt.find('div.toolbar').append($tsvButton);

                var $listButton = jQuery('<button>').text("Create List").click(function() {
                    chosenListIds[id] = [];
                    typeOfObj[id] = {};
                    var $that = $(this).attr("disabled", true);
                    listCreationState[id] = true;
                    $dt.find('td input').attr("checked", false);
                    var $popup = jQuery('<div class="summary-popup list-popup"></div>');
                    var $h3 = jQuery('<h3>').addClass("main-heading");
                    $('<span class="list-creation-info"><span class="new-list-name">New List</span> (<span class="selected-count">0</span> <span class="selected-types">items</span> selected) </span>').appendTo($h3);
                    $h3.find('.new-list-name').editable(function(value, settings) {return value}, {onblur: "submit", submit: "OK", tooltip: "Click to edit"});
                    var $topBox = jQuery('<div class="summary-header"></div>').appendTo($popup).append($h3);
                    var $centreBox = jQuery('<div>').addClass("list-items-scroller").appendTo($popup);
                    var $ul = jQuery('<ul>').addClass("list-items").appendTo($centreBox);
                    var $makerButton = $('<button class="list-maker" disabled>Make List</button>').addClass("summary-remover").appendTo($popup);
                    var $removeButton = jQuery('<button class="summary-remover">Cancel</button>');
                    $removeButton.appendTo($popup);
                    var remover = getListPopupRemover($popup, $dt, id, $that);
                    $makerButton.click(getListMaker(base, id, $h3, token, remover));
                    $dt.unbind("click");
                    $removeButton.click(remover);
                    $dt.append($popup);
                    $popup.click(returnFalse).draggable({handle: ".summary-header"}).resizable({
                        minWidth: $popup.width(), 
                        minHeight: $popup.height(), 
                        alsoResize: $centreBox
                    });

                    // Get an unused name if the default is already taken.
                    $.getJSON(base + list_path, {token: token}, function(result) {
                        var listNames =_(result.lists).pluck('name');
                        var origName = $h3.find('.new-list-name').text();
                        var currentName = origName;
                        var counter = 2;
                        while (_(listNames).include(currentName)) {
                            currentName = origName + " " + counter;
                            counter++;
                        }
                        if (currentName != origName) {
                            $h3.find('.new-list-name').text(currentName);
                        }
                    });
                    __(pq.select).map(function(p) {return p.substring(0, p.lastIndexOf("."))})
                                 .uniq()
                                 .map(function(p) {return {path: p, type: InterMine.models[base].getCdForPath(p).name}})
                                 .each(function(o) {
                        $('<li>').append("<button>Add all " + pluralise(o.type, 2) + "</button>").appendTo($ul).addClass("column-list-adder").click(function() {
                            var query = $.extend(true, {}, pq);
                            query.select = [o.path + ".id"];
                            $ul.empty().append($('<span class="throbber"></span>').css("float", "left"));

                            $.ajax({
                                type: "POST",
                                dataType: "json",
                                url: base + query_path,
                                data: {token: token, format: "jsonrows", query: query.serialise()},
                                success: function(result) {
                                    var typeMap = {};
                                    var idSet = [];
                                    _(result.results).each(function(row) {
                                        idSet.push(row[0].value);
                                        typeMap[row[0].value] = o.type;
                                    });
                                    console.log(typeMap);
                                    chosenListIds[id] = idSet;;
                                    typeOfObj[id] = typeMap;
                                    $h3.find('.selected-count').text(idSet.length);
                                    $h3.find('.selected-types').text(pluralise(o.type, idSet.length));
                                    $makerButton.attr("disabled", idSet.length < 1);
                                    updateListPreview(id, base);
                                }
                            });
                        });
                    });
                    $ul.append('<span>Or choose individual items by selecting them from the table</span>');
                    
                    $dt.find('td input').show();
                    return false;
                });
                $dt.find('div.toolbar').append($listButton);

                $dt.find('div.title').append('<span class="query-summary">' + pq.title + '</span>');

                $dt.click(function() {$dt.find('.summary-popup').remove(); $dt.find('th').removeClass("ui-state-hover").removeClass("border-collapse-fix");});

                $originalDiv = $dt.parent().parent();
                $('<div class="constraints-hider"><h3 style="display: inline; cursor: pointer;"><span class="constraint-count"></span> Constraint<span class="plural-s">s</span> on ' + pq.title + '</h3><div id="' + id + '_constraints" style="display:none;">Loading...</div></div>').prependTo($originalDiv);
                $originalDiv.find('.constraints-hider h3').unbind('click').before('<span class="ui-icon ui-icon-triangle-1-e" style="display: inline-block;">').click(function() { 
                    $(this).next().slideToggle('fast', function() {
                        console.log(this);
                        if ($(this).is(':hidden')) {
                            $(this).parent().find('.ui-icon').removeClass("ui-icon-triangle-1-s").addClass("ui-icon-triangle-1-e");
                        } else {
                            $(this).parent().find('.ui-icon').removeClass("ui-icon-triangle-1-e").addClass("ui-icon-triangle-1-s");
                        }
                    });
                });

                tableInitialised[id] = true;
            }
        });
    }
};

/**
 * Get the representation of the query that is currently being 
 * displayed in the datatable (ie, with the right view and sort-order).
 */
function getVisibleQuery(pq, $dataTable, id) {
    var visibleQuery = jQuery.extend(true, {}, pq);
    var settings = $dataTable.fnSettings();
    var new_view = [];
    for (var i = 0; i < settings.aoColumns.length; i++) {
        var col = settings.aoColumns[i];
        var path = col.sName;
        var isVisible = col.bVisible;
        if (isVisible) {
            new_view.push(path);
        }
    }
    visibleQuery.select = new_view;
    if (id in cache && "lastQuery" in cache[id]) {
        visibleQuery.sortOrder = cache[id].lastQuery.sortOrder;
    }
    return visibleQuery;
}

