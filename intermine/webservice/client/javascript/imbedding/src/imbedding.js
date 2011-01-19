IMBedding = (function() {
    var baseUrl = null;
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
    var getStyle = function(elem, cssprop, cssprop2) {
        // IE
        if (elem.currentStyle) {
        return elem.currentStyle[cssprop];

        // other browsers
        } else if (document.defaultView && document.defaultView.getComputedStyle) {
            return document.defaultView.getComputedStyle(elem, null).getPropertyValue(cssprop2);
        // fallback
        } else {
            return null;
        }
    };
    var fitContainerToTable = function(tableId, containerId) {
        var table = document.getElementById(tableId);
        var container = document.getElementById(containerId);
        if (table.offsetWidth > container.offsetWidth) {
            container.defaultWidth = container.offsetWidth;
            container.style.width = (table.offsetWidth + 1) + 'px';
        } else if (table.offsetWidth == 0) {
            if (container.defaultWidth) {
                container.style.width = container.defaultWidth + "px";
            }
        }
    };

    var getTableResizer = function(tableId, containerId) {
        return function() {
            $('#' + tableId).fadeToggle('slow', function() {
                fitContainerToTable(tableId, containerId);
            });
        };
    };
    var buildTable = function(data, target) {
        var container = document.createElement("div");
        container.setAttribute("class", "imbedded-table-container");
        container.id = "imbedded-table-container-" + data.title + new Date().getTime() 
            + "-" + Math.floor(Math.random() * 1001);
        container.id = container.id.replace(/\s+/g, '');
        var titlebox = document.createElement("div");
        titlebox.setAttribute("class", "imbedded-table-titlebox");
        var title = document.createElement("span");
        title.setAttribute("class", "imbedded-table-title");
        title.innerHTML = data.title + " (" + data.count + ")";
        titlebox.appendChild(title);
        container.appendChild(titlebox);

        var table = document.createElement("table");
        var tableClasses = "imbedded-table";
        if (! data.previous) {
            tableClasses += " firstpage";
        }
        table.setAttribute("class", tableClasses);
        table.id = "imbedded-table-" + data.title + new Date().getTime();
        table.id = table.id.replace(/\s+/g, '');

        $(titlebox).click(getTableResizer(table.id, container.id));

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

        var resultCount = data.results.length;
        for (var i = 0; i < resultCount; i++) {
            var dataRow = document.createElement("tr");
            dataRow.setAttribute("class", "imbedded-table-row imbedded-data-row " + getRowClass(i));
            var noCell = document.createElement("td");
            noCell.setAttribute("class", "imbedded-cell " + getColumnClass(0));
            noCell.innerHTML = i + 1 + parseInt(data.start);
            dataRow.appendChild(noCell);

            for (var j = 0; j < colCount; j++) {
                var cell = data.results[i][j];
                var tableCell = document.createElement("td");
                tableCell.setAttribute("class", "imbedded-cell " + getColumnClass(j));
                if (cell.value) {
                    var a = document.createElement("a");
                    a.href = cell.url;
                    a.innerHTML = cell.value;
                    tableCell.appendChild(a);
                } else {
                    tableCell.innerHTML = "[NONE]";
                }
                dataRow.appendChild(tableCell);
            }
            table.appendChild(dataRow);
        }
        container.appendChild(table);
        var csvLink = document.createElement("a");
        csvLink.setAttribute("class", "imbedded-exportlink");
        csvLink.href = data.csv_url;
        csvLink.innerHTML = "Export as CSV file";
        container.appendChild(csvLink);
        var tsvLink = document.createElement("a");
        tsvLink.setAttribute("class", "imbedded-exportlink");
        tsvLink.href = data.tsv_url;
        tsvLink.innerHTML = "Export as TSV file";
        container.appendChild(tsvLink);
        if (data.next) {
            var nextLink = document.createElement("a");
            nextLink.setAttribute("class", "imbedded-pagelink next");
            nextLink.href = target;
            $(nextLink).click(function() {loadUrl(data.next, target)});
            nextLink.innerHTML = "Next";
            container.appendChild(nextLink);
        }
        if (data.previous) {
            var prevLink = document.createElement("a");
            prevLink.setAttribute("class", "imbedded-pagelink prev");
            prevLink.href = target;
            $(prevLink).click(function() {loadUrl(data.previous, target)});
            prevLink.innerHTML = "Previous";
            container.appendChild(prevLink);
        }
        jQuery(target).empty().append(container);
        if (data.previous) {
            fitContainerToTable(table.id, container.id);
        }
    };
    var getCallback = function(target) {
        if (target instanceof Function) {
            return target;
        } else {
            return function(data) {buildTable(data, target)};
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
    var getResults = function(url, data, target) {
        var callback = getCallback(target);
        data.format = "jsonptable";
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
                whereString += attr + '="' + whereClause[attr] + '" ';
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
            if ("logic" in source) {
                xmlString += 'logic="' + source.logic + '" ';
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

    return {
        setBaseUrl: function(url) {
            baseUrl = url;
            return baseUrl;
        },
        getBaseUrl: function() {
            return baseUrl;
        },
        loadTemplate: function(data, target) {
            if (! baseUrl) {
                throw("baseUrl is null");
            }
            var url = baseUrl + templateResultsPath; 
            return getResults(url, data, target);
        },
        loadQuery: function(query, data, target) {
            data.query = getXML(query);
            if (! baseUrl) {
                throw("baseUrl is null");
            }
            var url = baseUrl + queryResultsPath;
            return getResults(url, data, target);
        }
    };
})();
