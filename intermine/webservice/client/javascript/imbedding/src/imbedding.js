IMBedding = (function() {
    var baseUrl = null;
    var placeholder = '#placeholder';
    var templateResultsPath = "/service/template/results";
    var queryResultsPath = "/service/query/results";
    var getColumnClass = function(index) {
        return ((index % 2 == 0) ? "imbedded-column-even" : "imbedded-column-odd");
    };
    var getRowClass = function(index) {
        return ((index % 2 == 0) ? "imbedded-row-even" : "imbedded-row-odd");
    };
    var loadJSON = function(requestUrl) {
        console.log(requestUrl);
        var script = document.createElement("script");
        script.type = "text/javascript";
        script.src = requestUrl;
        document.getElementsByTagName("head")[0].appendChild(script);
    };
    return {
        setBaseUrl: function(url) {
            baseUrl = url;
            return baseUrl;
        },
        getBaseUrl: function() {
            return baseUrl;
        },
        loadTemplate: function(name, values, callback, target) {
            if (! baseUrl) {
                throw("baseUrl is null");
            }
            if (target) {
                placeholder = target;
            }
            if (! callback) {
                callback = "IMBedding.buildTable";
            }
            var requestUrl = baseUrl + templateResultsPath + "?name=" + escape(name) + "&";
            requestUrl += "format=jsonptable&";
            for (val in values) {
                requestUrl += escape(val) + "=" + escape(values[val]) + "&";
            }
            requestUrl += "size=10&callback=" + escape(callback);
            loadJSON(requestUrl);
        },
        loadQuery: function(xml, callback, target) {
            if (! baseUrl) {
                throw("baseUrl is null");
            }
            if (target) {
                placeholder = target;
            }
            if (! callback) {
                callback = "IMBedding.buildTable";
            }
            console.log(xml);
            var requestUrl = baseUrl + queryResultsPath + "?query=" + escape(xml) + "&";
            requestUrl += "format=jsonptable&";
            requestUrl += "size=10&callback=" + escape(callback);
            loadJSON(requestUrl);
        },
        buildTable: function(data, target) {
            target = target || placeholder;
            var container = document.createElement("div");
            container.setAttribute("class", "imbedded-table-container");
            var titlebox = document.createElement("div");
            titlebox.setAttribute("class", "imbedded-table-titlebox");
            var title = document.createElement("span");
            title.setAttribute("class", "imbedded-table-title");
            title.innerHTML = data.title + " (" + data.count + ")";
            titlebox.appendChild(title);
            container.appendChild(titlebox);

            var table = document.createElement("table");
            table.setAttribute("class", "imbedded-table");
            table.id = "imbedded-table-" + data.title + new Date().getTime();
            table.id = table.id.replace(/\s+/g, '');

            titlebox.setAttribute("onclick", 
                    "$('#" + table.id + "').fadeToggle('slow', function() {});");

            var colHeaderRow = document.createElement("tr");
            colHeaderRow.setAttribute("class", "imbedded-table-row imbedded-column-header-row");
            var colCount = data.views.length;
            for (var i = 0; i < colCount; i++) {
                var cell = document.createElement("td");
                cell.setAttribute("class", 
                        "imbedded-cell imbedded-column-header " 
                        + getColumnClass(i));
                cell.innerHTML = data.views[i];
                colHeaderRow.appendChild(cell);
            }
            table.appendChild(colHeaderRow);

            var resultCount = data.results.length;
            for (var i = 0; i < resultCount; i++) {
                var dataRow = document.createElement("tr");
                dataRow.setAttribute("class", "imbedded-table-row imbedded-data-row " + getRowClass(i));
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
            if (data.previous) {
                var prevLink = document.createElement("a");
                prevLink.setAttribute("class", "imbedded-pagelink prev");
                prevLink.href = data.previous;
                prevLink.innerHTML = "Previous";
                container.appendChild(prevLink);
            }
            if (data.next) {
                var nextLink = document.createElement("a");
                nextLink.setAttribute("class", "imbedded-pagelink next");
                nextLink.href = data.next;
                nextLink.innerHTML = "Next";
                container.appendChild(nextLink);
            }
            jQuery(target).empty().append(container);
        }
    };
})();
