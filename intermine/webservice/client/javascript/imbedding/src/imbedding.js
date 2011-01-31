IMBedding = (function() {
    var baseUrl = null;
    var tables = {};
    var placeholder = '#placeholder';
    var templateResultsPath = "/service/template/results";
    var queryResultsPath = "/service/query/results";
    var availableTemplatesPath = "/service/templates";
    var getColumnClass = function(index) {
        return ((index % 2 == 0) ? "imbedded-column-even" : "imbedded-column-odd");
    };
    var getRowClass = function(index) {
        return ((index % 2 == 0) ? "imbedded-row-even" : "imbedded-row-odd");
    };

    var defaultOptions = {
        onTitleClick: "collapse",
        showExportLinks: true,
        showMineLink: true
    };

    var fitContainerToTable = function(tableId, containerId) {
        var table = document.getElementById(tableId);
        var container = document.getElementById(containerId);
        if (table.offsetWidth > container.offsetWidth) {
            container.defaultWidth = container.offsetWidth;
            container.style.width = (table.offsetWidth + 1) + 'px';
        } else if (table.offsetWidth == 0) {
            if (container.defaultWidth) {
                container.style.width = container.defaultWidth + 'px';
            }
        }
    };

    var getTableResizer = function(tableId, containerId, uid) {
        return function() {
            $('#imbedded-csvlink-' + uid).toggle();
            $('#imbedded-tsvlink-' + uid).toggle();
            $('#imbedded-mineresultslink-' + uid).toggle();
            $('#' + tableId).fadeToggle('slow', function() {
                fitContainerToTable(tableId, containerId);
                updateVisibilityOfPagers(uid);
            });
        };
    };

    var hidePagers = function(uid) {
        var table = tables[uid];
        var nextLink = $("#imbedded-nextlink-" + uid);
        var prevLink = $("#imbedded-prevlink-" + uid);
        nextLink.hide();
        prevLink.hide();
    };

    var updateVisibilityOfPagers = function(uid) {
        if (! $('#imbedded-table-' + uid).is(':visible')) {
            hidePagers(uid);
            return;
        }
        var table = tables[uid];
        var nextLink = $("#imbedded-nextlink-" + uid);
        var prevLink = $("#imbedded-prevlink-" + uid);
        table.nextLink = getNextUrl(table.pagePath, table.count, table.start, table.size, table.options);
        table.prevLink = getPrevUrl(table.pagePath, table.start, table.size, table.options); 

        if (table.start > 0) {
            prevLink.show();
        } else {
            prevLink.hide();
        }
        if (table.lastRow >= table.count) {
            nextLink.hide();
        } else {
            nextLink.show();
        }
    };

    var showIMTooltip = function(x, y, content) {
     $('<div></div>').html(content)
                     .attr("id", "imtooltip")
                     .addClass("imbedded-table-tooltip")
                     .appendTo("body")
                     .css( {
        position:             'absolute',
        display:              'none',
        top:                  y + 10,
        left:                 x + 10,
      }).fadeIn(200);
     var tt = document.getElementById("imtooltip");
    };

    var buildTable = function(data, target, passedOpts) {
        var options = jQuery.extend({}, defaultOptions, passedOpts);
        var uid = new Date().getTime() + "-" + Math.floor(Math.random() * 1001);
        uid = uid.replace(/\s+/g, '');
        tables[uid] = {options: options};

        var container = document.createElement("div");
        container.setAttribute("class", "imbedded-table-container");
        container.id = "imbedded-table-container-" + uid;
            
        var titlebox = document.createElement("div");
        titlebox.setAttribute("class", "imbedded-table-titlebox");
        var title = document.createElement("a");
        title.className = "imbedded-table-title";
        title.href = '#';
        title.appendChild(document.createTextNode(data.title + " ("));
        var countDisplayer = document.createElement("span");
        countDisplayer.id = "imbedded-count-displayer-" + uid;
        title.appendChild(countDisplayer);
        title.appendChild(document.createTextNode(")"));
        titlebox.appendChild(title);
        container.appendChild(titlebox);
        var table = document.createElement("table");
        table.setAttribute("start", data.start);
        tables[uid].start = data.start;
        tables[uid].lastRow = data.size;
        tables[uid].size = data.size;
        table.setAttribute("lastrow", data.size);
        table.setAttribute("size", data.size);
        table.id = "imbedded-table-" + uid;
        $.jsonp({
            url: localiseUrl(data.count, options),
            success: function(countData) {
                countDisplayer.innerHTML = countData.count + " results";
                countDisplayer.setAttribute("count", countData.count);
                tables[uid].count = countData.count;
                updateVisibilityOfPagers(uid);
            }, 
            callbackParameter: "callback"
        });

        var tableClasses = "imbedded-table";
        if (data.start == 0) {
            tableClasses += " imbedded-initial-state";
        }
        table.className = tableClasses;
        tables[uid].pagePath = data.current + "&size=" + data.size;

        var colHeaderRow = document.createElement("tr");
        colHeaderRow.setAttribute("class", "imbedded-table-row imbedded-column-header-row");
        var colCount = data.views.length;
        var counter = document.createElement("td");
        counter.setAttribute("class", 
                    "imbedded-cell imbedded-column-header");
        colHeaderRow.appendChild(counter);
        for (var i = 0; i < colCount; i++) {
            var cell = document.createElement("td");
            cell.setAttribute("class", "imbedded-cell imbedded-column-header");
            cell.innerHTML = data.columnHeaders[i];
            colHeaderRow.appendChild(cell);
        }
        table.appendChild(colHeaderRow);
        $.jsonp({
            url: localiseUrl(tables[uid].pagePath, options),
            success: function(data) {fillInTable(uid, data, options);},
            callbackParameter: "callback"
        });

        container.appendChild(table);
        if (options.showExportLinks) {
            var csvLink = document.createElement("a");
            csvLink.id = "imbedded-csvlink-" + uid;
            csvLink.setAttribute("class", "imbedded-exportlink");
            csvLink.href = localiseUrl(data.csv_url, options);
            csvLink.innerHTML = "Export as CSV file";
            container.appendChild(csvLink);
            var tsvLink = document.createElement("a");
            tsvLink.id = "imbedded-tsvlink-" + uid;
            tsvLink.setAttribute("class", "imbedded-exportlink");
            tsvLink.href = localiseUrl(data.tsv_url, options);
            tsvLink.innerHTML = "Export as TSV file";
            container.appendChild(tsvLink);
        }
        if (options.showMineLink && options.onTitleClick != "mine") {
            var mineResultsLink = document.createElement("a");
            mineResultsLink.target = "_blank";
            mineResultsLink.id = "imbedded-mineresultslink-" + uid;
            mineResultsLink.setAttribute("class", "imbedded-exportlink");
            mineResultsLink.href = localiseUrl(data.mineResultsLink, options);
            mineResultsLink.innerHTML = "View in Mine";
            container.appendChild(mineResultsLink);
        }
        var nextLink = document.createElement("a");
        nextLink.id = "imbedded-nextlink-" + uid;
        nextLink.setAttribute("class", 
                "imbedded-pagelink next imbedded-initial-state");
        nextLink.href = target;
        tables[uid].nextLink = getNextUrl(data.current, null, data.start, data.size, options);
        $(nextLink).click(function() {
            // It is difficult to tell what the throbber looks like until we 
            // have some queries that take some time
            /*$(table).children('tbody').detach();
            var throbber = document.createElement("img");
            throbber.src = "images/throbber.gif";
            $(table).append(throbber);*/
            $.jsonp({
                url: tables[uid].nextLink,
                callbackParameter: "callback",
                success: function(data) {
                    /*$(throbber).detach();*/
                    fillInTable(uid, data, options);
                    updateVisibilityOfPagers(uid);
                }
            });
        });
        nextLink.innerHTML = "Next";
        container.appendChild(nextLink);
        var prevLink = document.createElement("a");
        prevLink.id = "imbedded-prevlink-" + uid;
        prevLink.setAttribute("class", 
                "imbedded-pagelink prev imbedded-initial-state");
        prevLink.href = target;
        $(prevLink).click(function() {
            $.jsonp({
                url: tables[uid].prevLink,
                callbackParameter: "callback",
                success: function(data) {
                    fillInTable(uid, data, options);
                    updateVisibilityOfPagers(uid);
                }
            });
        });
        prevLink.innerHTML = "Previous";
        container.appendChild(prevLink);
        if (data.start <= 0) {
            $(prevLink).hide();
        }
        jQuery(target).empty().append(container);
        if (options.onTitleClick == "collapse") {
            jQuery(titlebox).click(getTableResizer(table.id, container.id, uid));
        } else if (options.onTitleClick == "mine") {
            title.href = localiseUrl(data.mineResultsLink, options);
        }
        jQuery(title).hover(
            function(event) {
                showIMTooltip(event.pageX, event.pageY, data.description);
            },
            function(event) {
                jQuery('.imbedded-table-tooltip').remove();
            }
        );
    };

    var getNextUrl =function(basePath, count, start, size, options) {
        var nextStart = (count != null) ? Math.min(count, (start + size)) : (start + size);
        var ret = localiseUrl(basePath, options) + "&start=" + nextStart;
        return ret;
    }
        
    var getPrevUrl = function(basePath, start, size, options) {
        var nextStart = Math.max(0, (start - size));
        return localiseUrl(basePath, options) + "&start=" + nextStart;
    }

    var fillInTable = function(uid, resultSet, options) {
        var table = jQuery("#imbedded-table-" + uid);
        // Remove any data this table already contains
        table.children('tbody').detach();

        var resultCount = resultSet.results.length;
        for (var i = 0; i < resultCount; i++) {
            var dataRow = document.createElement("tr");
            dataRow.setAttribute("class", "imbedded-table-row imbedded-data-row " + getRowClass(i));
            var numCell = document.createElement("td");
            numCell.setAttribute("class", "imbedded-cell " + getColumnClass(0));
            numCell.innerHTML = i + 1 + parseInt(resultSet.start);
            dataRow.appendChild(numCell);

            var colCount = resultSet.results[i].length;

            for (var j = 0; j < colCount; j++) {
                var cell = resultSet.results[i][j];
                var tableCell = document.createElement("td");
                tableCell.setAttribute("class", "imbedded-cell " + getColumnClass(j));
                if (cell.value) {
                    var a = document.createElement("a");
                    a.target = "_blank";
                    a.href = localiseUrl(cell.url, options);
                    a.innerHTML = cell.value;
                    tableCell.appendChild(a);
                } else {
                    tableCell.innerHTML = "[NONE]";
                }
                dataRow.appendChild(tableCell);
            }
            table.append(dataRow);
        }
        tables[uid].start = resultSet.start;
        tables[uid].lastRow = resultCount + resultSet.start;
        IMBedding.afterBuildTable(uid, resultSet);
    };

    var localiseUrl = function(url, options) {
        var ret;
        if (options && "baseUrl" in options) {
            ret = options.baseUrl;
        } else {
            ret = baseUrl;
        }
        if ((! ret.match(/\/$/)) && (! url.match(/^\//))) {
            ret += "/";
        }
        return ret + url;
    }

    var getCallback = function(target, options) {
        if (target instanceof Function) {
            return target;
        } else {
            return function(data) {buildTable(data, target, options)};
        }
    };
    var handleError = function(options, textStatus) {
        if (textStatus == "error") {
            alert("Something was wrong with your query - please edit it");
        } else {
            alert(textStatus);
        }
    };
    var loadUrl = function(url, target) {
            url += "&format=jsonptable";
            return getResults(url, {}, target);
    };
    var getResults = function(url, data, target, options) {
        var callback = getCallback(target, options);
        if (! data.format) {
            data.format = "jsonptable";
        }
        $.jsonp({
            url: url, 
            data: data, 
            success: callback, 
            callbackParameter: "callback",
            error: handleError
        });
    };

    var getConstraints = function(constraints) {
        var constraintsString = "";
        for (var i = 0; i < constraints.length; i++) {
            var whereClause = constraints[i];
            var whereString = "<constraint ";
            for (attr in whereClause) {
                whereString += attr + '="' + escapeOperator(whereClause[attr]) + '" ';
            }
            whereString += "/>";
            constraintsString += whereString;
        }
        return constraintsString;
    };

    var getXML = function(source) {
        if (typeof(source) == "string") {
            return source;
        } else if (source instanceof Object) {
            var xmlString = '<query model="';
            if ("model" in source) {
                xmlString += source.model;
            } else if ("from" in source) {
                xmlString += source.from;
            } else {
                throw("No model in query");
            }

            xmlString += '" view="';
            if ("view" in source) {
                xmlString += source.view.join(" ");
            } else if ("select" in source) {
                xmlString += source.select.join(" ");
            } else {
                throw("No view in source");
            }
            xmlString += '" ';
            if ("constraintLogic" in source) {
                xmlString += 'constraintLogic="' + source.constraintLogic + '" ';
            }
            if ("sortOrder" in source) {
                xmlString += 'sortOrder="' + source.sortOrder + '" ';
            }
            xmlString += ">";
            if ("joins" in source) {
                for (var i = 0; i < source.joins.length; i++) {
                    var join = source.joins[i];
                    var joinString = '<join ';
                    
                    for (attr in join) {
                        joinString += (attr + '="' + join[attr] + '" ');
                    }
                    joinString += '/>';
                    xmlString += joinString;
                }
            }
            if ("where" in source) {
                xmlString += getConstraints(source.where);
            }
            if ("constraints" in source) {
                xmlString += getConstraints(source.constraints);
            }
            xmlString += '</query>';
            return xmlString;
        } else {
            throw "Sorry - this only takes strings or objects";
        }
    };
    var ESCAPE_DICT = {
        "<": '&lt;',
        ">": '&gt;',
        "<=": '&lt;=',
        ">=": '&gt;='
    };
    var escapeOperator = function(operator) {
        if (operator in ESCAPE_DICT) {
            return ESCAPE_DICT[operator];
        } else {
            return operator;
        }
    };

    return {
        afterBuildTable: function(uid, data) {},
        makeQueryXML: getXML,
        setBaseUrl: function(url) {
            baseUrl = url;
            return baseUrl;
        },
        getBaseUrl: function() {
            return baseUrl;
        },
        loadTemplate: function(data, target, options) {
            if ("baseUrl" in data) {
                baseUrl = data.baseUrl;
            }
            var url = localiseUrl(templateResultsPath, options); 
            return getResults(url, data, target, options);
        },
        loadQuery: function(query, data, target, options) {
            data.query = getXML(query);
            var url = localiseUrl(queryResultsPath, options);
            return getResults(url, data, target, options);
        }
    };
})();
