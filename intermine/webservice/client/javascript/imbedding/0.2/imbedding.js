IMBedding = (function() {
    // Add in a console in case there is no global one.
    if (typeof(console) == undefined) {
        console = {log: function() {}};
    }
    var baseUrl = null;
    var tables = {};
    var defaultTableSize = 10;
    var defaultQuery = {from: "genomic"};
    var placeholder = '#placeholder';
    var templateResultsPath = "/service/template/results";
    var queryResultsPath = "/service/query/results";
    var availableTemplatesPath = "/service/templates/json";
    var possibleValuesPath = "/service/path/values";
    var modelPath = "/service/model/json";
    var getColumnClass = function(index) {
        return ((index % 2 == 0) ? "imbedded-column-even" : "imbedded-column-odd");
    };
    var getRowClass = function(index) {
        return ((index % 2 == 0) ? "imbedded-row-even" : "imbedded-row-odd");
    };

    var defaultTemplateCallback = function(data) {
        window.im_templates = data.templates;
    };
    var defaultModelCallback = function(data) {
        window.im_model = data.model;
    };

    var addCommas = function(nStr, separator) {
        nStr += '';
        x = nStr.split('.');
        x1 = x[0];
        x2 = x.length > 1 ? '.' + x[1] : '';
        var rgx = /(\d+)(\d{3})/;
        while (rgx.test(x1)) {
            x1 = x1.replace(rgx, '$1' + separator + '$2');
        }
        return x1 + x2;
    }

    var mungeHeader = function(header) {
        var parts = header.split(" > ");
        if (parts.length == 1) {
            return parts[0];
        } else {
            var ret = parts.slice(parts.length - 2, parts.length).join(" ");
            return ret;
        }
    };

    var cellCreater = function(cell, localiser) {
        var a = document.createElement("a");
        a.target = "_blank";
        if (cell.url) {
            a.href = localiser(cell.url);
        }
        a.innerHTML = cell.value;
        return a;
    };

    var defaultOptions = {
        formatCount: true,
        thousandsSeparator: ",",
        additionText: "Load [x] more rows",
        afterBuildTable: function(table) {},
        afterTableUpdate: function(table, resultSet) {},
        allRowsText: "Show remaining rows",
        collapseHelpText: "hide table",
        countText: "[x] rows",
        createCellContent: cellCreater,
        defaultQueryName: "Query Results",
        emptyCellText: "[NONE]",
        errorHandler: function(error, statusCode) {console.log("Error:", error, statusCode); alert("Sorry - this table could not be loaded:\n" + error);},
        expandHelpText: "show table",
        exportCSVText: "Export as CSV file",
        exportTSVText: "Export as TSV file",
        mineLinkText: "View in Mine",
        nextText: "Next",
        onTitleClick: "collapse",
        onTableToggle: function(table) {},
        openOnLoad: false,
        previousText: "Previous",
        queryTitleText: null,
        replaceRightArrows: true,
        rightArrowStyle: "\u21E8", // block right arrow (unicode hex representation)
        resultsDescriptionText: null,
        showAdditionsLink: true,
        showAllCeiling: 75,
        showAllLink: true,
        showCount: true,
        showExportLinks: true,
        showMineLink: true,
        throbberSrc: "images/throbber.gif",
        titleHoverCursor: "pointer",
        headerMunger: mungeHeader
    };

    var localiseUrl = function(url, options) {
        if (url.match(/^(http|https|ftp):\/\//)) {
            return url;
        }
        var ret;
        if (options && "baseUrl" in options) {
            ret = options.baseUrl;
        } else {
            ret = baseUrl;
        }
        if ((! ret.match(/\/$/)) && (! url.match(/^\//))) {
            ret += "/";
        } else if (url.match(/^\//)) {
            ret = ret.replace(/\/$/, '');
        }

        return ret + url;
    };

    var showIMTooltip = function(x, y, content) {
     jQuery('<div></div>').html(content)
                     .attr("id", "imtooltip")
                     .addClass("imbedded-table-tooltip")
                     .appendTo("body")
                     .css( {
        position:             'absolute',
        display:              'none',
        top:                  y + 10,
        left:                 x + 10
      }).fadeIn(200);
     var tt = document.getElementById("imtooltip");
    };

    var Table = function(data, passedOpts) {

        this._constructor = function(data, passedOpts) {
            var outer = this;
            this.options = jQuery.extend({}, defaultOptions, passedOpts);


            this.uid = new Date().getTime() + "-" + Math.floor(Math.random() * 1001);
            this.uid.replace(/\s+/g, '');
            this.start = data.start;
            this.lastRow = data.size;
            this.size = data.size;
            this.pageSize = data.size;
            this.additionSize = data.size;
            this.pagePath = data.current;
            this.isFilledIn = false;
            this.count = null;

            this.container = jQuery('<div id="imbedded-table-container-' 
                    + this.uid + '" class="imbedded-table-container"></div>');
            this.titlebox = jQuery('<div id="imbedded-table-titlebox-' 
                    + this.uid + '" class="imbedded-table-titlebox"></div>');
            var queryTitle = this.options.queryTitleText || data.title || this.options.defaultQueryName;
            if (this.options.replaceRightArrows) {
                queryTitle = queryTitle.replace("-->", this.options.rightArrowStyle);
            }
            this.title = jQuery('<a id="imbedded-table-title-' 
                    + this.uid + '" class="imbedded-table-title">' + queryTitle + '</a>');
            this.expandHelp = jQuery('<span id="imbedded-table-expand-help-' 
                    + this.uid + '" class="imbedded-table-expand-help">' 
                    + this.options.expandHelpText + '</span>');
            this.countDisplayer = jQuery('<span id="imbedded-count-displayer-' 
                    + this.uid + '" class="imbedded-count-displayer"></span>');
            this.nextUrl = this.getNextUrl();
            this.nextLink = jQuery('<a id="imbedded-nextlink-' 
                    + this.uid + '" class="imbedded-pagelink imbedded-next">' 
                    + this.options.nextText + '</a>')
                    .mouseover(function() { jQuery(this).css({cursor: "pointer"}) })
                    .click(function() {
                        jQuery.jsonp({
                            url: outer.nextUrl,
                            callbackParameter: "callback",
                            success: function(data) {outer.changePage(data)}
                        });
                    });

            this.prevLink = jQuery('<a id="imbedded-prevlink-' 
                    + this.uid + '" class="imbedded-pagelink imbedded-prev">' 
                    + this.options.previousText + '</a>')
                    .mouseover(function() { jQuery(this).css({cursor: "pointer"}) });
            this.prevLink.click(function() {
                jQuery.jsonp({
                    url: outer.prevUrl,
                    callbackParameter: "callback",
                    success: function(data) {outer.changePage(data)}
                });
            });

            this.scrollContainer = jQuery('<div class="imbedded-scroll-container"></div>');
            this.table = jQuery('<table style="display: none;" id="imbedded-table-' 
                    + this.uid + '" class="imbedded-table"></table>');

            this.colHeaderRow = jQuery('<tr class="imbedded-table-row imbedded-column-header-row"></tr>');
            this.colHeaderRow.append('<td class="imbedded-cell imbedded-column-header"></td>');
            for (var i = 0; i < data.views.length; i++) {
                var cell = document.createElement("td");
                cell.setAttribute("class", "imbedded-cell imbedded-column-header");
                cell.innerHTML = this.options.headerMunger(data.columnHeaders[i], i, this.uid);
                cell.setAttribute("title", data.columnHeaders[i]);
                this.colHeaderRow.append(cell);
            }

            this.csvLink = jQuery('<a id="imbedded-csvlink-' + this.uid 
                    + '" class="imbedded-exportlink">'
                    + this.options.exportCSVText + '</a>')
                    .attr("href", this.localiseUrl(data.csv_url));

            this.tsvLink = jQuery('<a id="imbedded-tsvlink-' + this.uid 
                    + '" class="imbedded-exportlink">' 
                    + this.options.exportTSVText + '</a>')
                    .attr("href", this.localiseUrl(data.tsv_url));

            this.mineLink = jQuery('<a id="imbedded-mineresultslink-' + this.uid 
                    + '" class="imbedded-exportlink imbedded-mineresultslink">'
                    + this.options.mineLinkText + '</a>')
                    .attr("href", this.localiseUrl(data.mineResultsLink));

            var additionInner = this.options.additionText.replace("[x]", " " + data.size + " ");
            this.additionLink = jQuery('<a id="imbedded-adder-' + this.uid 
                + '" class="imbedded-addition-link imbedded-pagelink">' + additionInner + '</a>')
                .mouseover(function() { jQuery(this).css({cursor: "pointer"}) })
                .click(function() {outer.loadMoreRows()});

            this.showAllLink = jQuery('<a id="imbedded-showall-' 
                    + this.uid + '" class="imbedded-showall-link imbedded-pagelink">'
                    + this.options.allRowsText + '</a>')
                    .mouseover(function() { jQuery(this).css({cursor: "pointer"}) })
                    .click(function() {outer.loadMoreRows(true)});

            jQuery().add(this.csvLink).add(this.tsvLink).add(this.mineLink).attr({target: '_blank'});

            // Pager links that go forwards
            this.morePagers = jQuery().add(this.nextLink).add(this.showAllLink).add(this.additionLink);
            // all pager links
            this.pagers = jQuery().add(this.prevLink).add(this.morePagers);

            jQuery.jsonp({
                url: this.localiseUrl(data.count),
                success: function(countData) {
                    if (outer.options.showCount) {
                        var displayCount = (outer.options.formatCount) 
                            ? addCommas(countData.count, outer.options.thousandsSeparator)
                            : countData.count;
                        var count = outer.options.countText.replace("[x]", displayCount);
                        outer.countDisplayer.text("(" + count + ")");
                    }
                    outer.count = countData.count;
                    if (countData.count == 0) {
                        outer.title.unbind("click");
                        outer.expandHelp.remove();
                        outer.title.css({cursor: "default"});
                        if (outer.options.openOnLoad) {
                            outer.resizeTable();
                        }
                    }
                    outer.updateVisibilityOfPagers();
                }, 
                callbackParameter: "callback"
            });

            if (data.start <= 0) {
                this.prevLink.hide();
            }

            if (! this.options.openOnLoad) {
                jQuery().add(this.prevLink)
                        .add(this.nextLink)
                        .add(this.csvLink)
                        .add(this.tsvLink)
                        .add(this.mineLink)
                        .hide();
            }

            if (this.options.showCount) {
                this.title.append(this.countDisplayer);
            }

            // Slot it all together
            this.titlebox.append(this.title);
            this.scrollContainer.append(this.table);
            this.container.append(this.titlebox)
                        .append(this.nextLink)
                        .append(this.prevLink)
                        .append(this.scrollContainer);

            if (this.options.showExportLinks) {
                this.container.append(this.csvLink)
                            .append(this.tsvLink);
            }

            if (this.options.showMineLink && this.options.onTitleClick != "mine") {
                this.container.append(this.mineLink);
            }
            if (this.options.showAdditionsLink) {
                this.container.append(this.additionLink);
            }
            if (this.options.showAllLink) {
                this.container.append(this.showAllLink);
            }

            if (this.options.onTitleClick == "collapse") {
                this.title.append(this.expandHelp);
                this.title.click(function() {outer.resizeTable()})
                          .mouseover(function() { jQuery(this).css({cursor: "pointer"}) });
            } else if (this.options.onTitleClick == "mine") {
                this.title.attr({href: this.localiseUrl(data.mineResultsLink), target: "_blank"});
            } else if (this.options.onTitleClick instanceof Function) {
                this.title.click(this.options.onTitleClick)
                          .mouseover(function() { 
                            jQuery(this).css({cursor: outer.options.titleHoverCursor}) 
                          });
            }


            var queryDescription = this.options.resultsDescriptionText || data.description;
            this.titlebox.hover(
                function(event) {
                    showIMTooltip(event.pageX, event.pageY, queryDescription);
                },
                function(event) {
                    jQuery('.imbedded-table-tooltip').remove();
                }
            );

            if (this.options.openOnLoad) {
                this.resizeTable();
            } else {
                var throbber = document.createElement("img");
                throbber.src = this.options.throbberSrc;
                this.table.append(throbber);
            }
        };

        this.changePage = function(data, append) {
            this.fillInTable(data, append);
            this.updateVisibilityOfPagers();
            this.fitContainerToTable();
        };

        this.toggleExpandHelpText = function() {
            if (this.expandHelp.text() === this.options.expandHelpText) {
                this.expandHelp.text(this.options.collapseHelpText);
            } else {
                this.expandHelp.text(this.options.expandHelpText);
            }
        };

        // Make sure that the table container 
        // is large enough to fit the table it contains.
        this.resizeTable = function() {
            var outer = this;
            var action = function() {
                outer.toggleExpandHelpText();
                outer.table.slideToggle(function() {
                    outer.fitContainerToTable();
                    if (jQuery(outer.table).is(':visible')) {
                        outer.csvLink.show();
                        outer.tsvLink.show();
                        outer.mineLink.show();
                    } else {
                        outer.csvLink.hide();
                        outer.tsvLink.hide();
                        outer.mineLink.hide();
                    }
                    if (outer.csvLink.is(':visible')) {
                        outer.csvLink.css({display: 'inline'});
                    }
                    if (outer.tsvLink.is(':visible')) {
                        outer.tsvLink.css({display: 'inline'});
                    }
                    if (outer.mineLink.is(':visible')) {
                        outer.mineLink.css({display: 'inline'});
                    }
                    outer.updateVisibilityOfPagers();
                });
                outer.options.onTableToggle(outer);
            };
            var url = this.localiseUrl(this.getPageUrl());
            if (! this.isFilledIn) {
                this.container.css({cursor: "wait"});
                jQuery.jsonp({
                    url: url,
                    success: function(data) {
                        outer.fillInTable(data);
                        action();
                        outer.container.css({cursor: "default"});
                    },
                    callbackParameter: "callback"
                });
            } else {
                action();
            }
        };


        this.updateVisibilityOfPagers = function() {
            if (! this.table.is(':visible')) {
                this.hidePagers();
                return;
            }
            this.nextUrl = this.getNextUrl();
            this.prevUrl = this.getPrevUrl();


            if (this.start > 0) {
                this.prevLink.show();
            } else {
                this.prevLink.hide();
            }
            if (this.lastRow >= this.count) {
                this.morePagers.hide();
            } else {
                this.morePagers.show();
            }
            if (this.count > this.options.showAllCeiling) {
                this.showAllLink.hide();
            }
        };

        this.hidePagers = function() {
            this.pagers.hide();
        };

        this.loadMoreRows = function(showAll) {
            var outer = this;
            var noOfRowsToGet = this.additionSize;
            var append = true;
            var url = this.getNextUrl(noOfRowsToGet, showAll);
            jQuery.jsonp({
                url: url,
                success: function(data) {
                    outer.size += noOfRowsToGet;
                    outer.changePage(data, append);
                },
                callbackParameter: "callback"
            });
        };

        this.containerNeedsExpanding = function() {
            return this.table.attr("offsetWidth") >= this.scrollContainer.attr("offsetWidth");
        };

        this.expandContainer = function() {
            if (! this.defaultContainerwidth) {
                this.defaultContainerwidth = this.scrollContainer.attr("offsetWidth") + 'px';
            }
            this.scrollContainer.css({width: (this.table.attr("offsetWidth") + 1) + 'px'});
        };

        this.tableIsHidden = function() {
            return this.table.attr("offsetWidth") == 0;
        };

        // Perform the resize
        this.fitContainerToTable = function() {
            this.scrollContainer.css({width: this.container.attr("clientWidth") + 'px'});
        };

        // get a page url using the base url 
        // and the given page size (or the default one)
        this.getPageUrl = function(size) {
            size = size || this.size;
            return this.pagePath + "&size=" + size;
        };

        // Construct the url to page forwards to
        this.getNextUrl = function(size, showAll) {
            size = size || this.size;
            var naiveNext = this.start + size;
            
            var nextStart = (this.count != null) ? Math.min(this.count, naiveNext) : naiveNext;
            nextStart = Math.max(this.lastRow, nextStart);
            var basePath = showAll ? this.pagePath : this.getPageUrl(size);
            var ret = this.localiseUrl(basePath) + "&start=" + nextStart;
            return ret;
        };

        // Construct the url to page backwards to
        this.getPrevUrl = function(size) {
            size = size || this.size;

            var nextStart = Math.max(0, (this.start - size));
            var basePath = this.getPageUrl(size);
            var ret = this.localiseUrl(basePath) + "&start=" + nextStart;
            return ret;
        }

        // construct a url using the path fragment given and the
        // baseUrl we know, because we requested it in the first place
        this.localiseUrl = function(url) {
            return localiseUrl(url, this.options);
        };

        this.getLocaliser =function(options) {
            return function(url) {
                return localiseUrl(url, options);
            };
        };

        // insert rows of data into the table
        // either appending them, or replacing the 
        // current ones
        this.fillInTable = function(resultSet, append) {
            // Remove any data this table already contains
            if (! append) {
                this.table.children('tbody').detach();
                this.table.append(this.colHeaderRow);
            }
            // Remove the throbber
            this.table.children('img').detach();

            var resultCount = resultSet.results.length;
            for (var i = 0; i < resultCount; i++) {
                var rowNumber = i + 1 + parseInt(resultSet.start);
                var dataRow = document.createElement("tr");
                dataRow.setAttribute("class", "imbedded-table-row imbedded-data-row " 
                        + getRowClass(rowNumber - 1));
                var numCell = document.createElement("td");
                numCell.setAttribute("class", "imbedded-cell " + getColumnClass(0));
                numCell.innerHTML = rowNumber;
                dataRow.appendChild(numCell);

                var colCount = resultSet.results[i].length;

                for (var j = 0; j < colCount; j++) {
                    var cell = resultSet.results[i][j];
                    var tableCell = document.createElement("td");
                    tableCell.setAttribute("class", "imbedded-cell " + getColumnClass(j));
                    if (cell.value != null) {
                        var elem = this.options.createCellContent(cell, this.getLocaliser(this.options));
                        tableCell.appendChild(elem);
                    } else {
                        tableCell.innerHTML = this.options.emptyCellText;
                        jQuery(tableCell).addClass("imbedded-null");
                    }
                    dataRow.appendChild(tableCell);
                }
                this.table.append(dataRow);
            }
            if (! append) {
                this.start = resultSet.start;
            }
            this.lastRow = resultCount + resultSet.start;
            this.isFilledIn = true;
            if (this.options.afterTableUpdate) {
                this.options.afterTableUpdate(this, resultSet);
            }
            IMBedding.afterTableUpdate(this.table, resultSet);
        };

        this._constructor(data, passedOpts);

    };

    var buildTable = function(data, target, passedOpts) {
        var table = new Table(data, passedOpts);
        tables[table.uid] = table;

        jQuery(target).empty().append(table.container);
        if (table.options.afterBuildTable) {
            table.options.afterBuildTable(table);
        }
        IMBedding.afterBuildTable(table);
    };
        

    var getCallback = function(target, options) {
        if (target instanceof Function) {
            return target;
        } else {
            return function(data) {
                if ("wasSuccessful" in data) {
                    if (data.wasSuccessful) {
                        buildTable(data, target, options);
                    } else {
                        errorHandler = options.errorHandler || defaultOptions.errorHandler;
                        errorHandler(data.error, data.statusCode);
                    }
                } else {
                    errorHandler = options.errorHandler || defaultOptions.errorHandler;
                    errorHandler("Incorrect data format returned from server", 500);
                }
            }
        }
    };
    var loadUrl = function(url, target) {
            url += "&format=jsonptable";
            return getResults(url, {}, target);
    };

    var getErrorHandler = function(options) {
        if (options && "errorHandler" in options) {
            return options["errorHandler"];
        } else {
            return defaultOptions.errorHandler;
        }
    };

    var getResults = function(url, data, target, options) {
        var callback = getCallback(target, options);
        var errorHandler = getErrorHandler(options);
        if (! data.format) {
            data.format = "jsonptable";
        }
        if (! data.size && data.format == "jsonptable") {
            data.size = defaultTableSize;
        }
        if (!target instanceof Function) {
            var throbber = document.createElement("img");
            throbber.src = defaultOptions.throbberSrc;
            jQuery(target).empty().append(throbber);
        }

        jQuery.jsonp({
            url: url, 
            data: data, 
            success: callback, 
            callbackParameter: "callback",
            error: errorHandler,
            traditional: true
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
            var query = jQuery.extend({}, defaultQuery, source);
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
            xmlString += ">";
            if ("joins" in query) {
                for (var i = 0; i < query.joins.length; i++) {
                    var join = query.joins[i];
                    var joinString = '<join ';
                    
                    for (attr in join) {
                        joinString += (attr + '="' + join[attr] + '" ');
                    }
                    joinString += '/>';
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

    var _loadJSInclude = function(scriptPath, callback) {
        var scriptNode = document.createElement('SCRIPT');
        scriptNode.type = 'text/javascript';
        scriptNode.src = scriptPath;

        var headNode = document.getElementsByTagName('HEAD');
        if (headNode[0] != null) {
            headNode[0].appendChild(scriptNode);
        }

        if (callback != null) {
            scriptNode.onreadystagechange = callback;            
            scriptNode.onload = callback;
        }
    }

    var _check_for_jsonp = function(callback) {
        if (typeof(jQuery.jsonp) == 'undefined') {
            _loadJSInclude('http://jquery-jsonp.googlecode.com/files/jquery.jsonp-2.1.4.min.js', callback);
        } else {
            callback();
        }
    }
    var _check_for_jquery = function(callback) {
        if (typeof(jQuery) == 'undefined') {
            _loadJSInclude('https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js', function() {_check_for_jsonp(callback)});
        } else {
            _check_for_jsonp(callback);
        }
    }

    var loadDependencies = function(callback) {
        _check_for_jquery(callback);
    }

    return {
        getTables: function() {return tables},
        getTable: function(id) {return tables[id]},
        setErrorHandler: function(errorHandler) {handleError = errorHandler},
        afterTableUpdate: function(table, data) {},
        afterBuildTable: function(table) {},
        makeQueryXML: getXML,
        setDefaultQuery: function(obj) {
            defaultQuery = obj;
            return defaultQuery;
        },
        setDefaultOptions: function(obj) {
            loadDependencies(function() {
                jQuery.extend(defaultOptions, obj);
            });
        },
        getDefaultOptions: function() {
            return defaultOptions;
        },
        setBaseUrl: function(url) {
            baseUrl = url;
            return baseUrl;
        },
        getBaseUrl: function() {
            return baseUrl;
        },
        loadTemplate: function(data, target, options) {
            loadDependencies(function() {
                if ("baseUrl" in data) {
                    baseUrl = data.baseUrl;
                }
                var url = localiseUrl(templateResultsPath, options); 
                getResults(url, data, target, options);
            });
        },
        loadQuery: function(query, data, target, options) {
            loadDependencies(function() {
                data.query = getXML(query);
                var url = localiseUrl(queryResultsPath, options);
                getResults(url, data, target, options);
            });
        },
        loadPossibleValues: function(path, subclasses, options) {
            loadDependencies(function() {
                var url = localiseUrl(possibleValuesPath, options);
                var params = {path: path};
                if (subclasses) {
                    params.typeConstraints = JSON.stringify(subclasses);
                }
                options = options || {};
                if (options.format) {
                    params.format = options.format;
                }
                jQuery.jsonp({
                    url: url, 
                    data: params, 
                    success: options.callback, 
                    callbackParameter: "callback"
                });
            });
        },
        loadModel: function(options) {
            loadDependencies(function() {
                var url = localiseUrl(modelPath, options);
                options = options || {};
                var callback = options.callback || defaultModelCallback;
                jQuery.jsonp({
                    url: url, 
                    success: callback, 
                    callbackParameter: "callback"
                });
            });
        },
        loadTemplates: function(options) {
            loadDependencies(function() {
                var url = localiseUrl(availableTemplatesPath, options);
                options = options || {};
                var callback = options.callback || defaultTemplateCallback;
                jQuery.jsonp({
                    url: url, 
                    success: callback, 
                    callbackParameter: "callback"
                });
            });
        }
    };
})();

