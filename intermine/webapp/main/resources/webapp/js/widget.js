window.Widgets;
if (!window.Widgets) {
	window.Widgets = function(service) {

		var CHART_OPTS, displayGraphWidgetConfig, getSeriesValue, getExtraValue, displayEnrichmentWidgetConfig, make_enrichment_row,
			calcNotAnalysed, loadGraphWidget, loadEnrichmentWidget;


		/* -------------- options -------------- */

        CHART_OPTS = {
            fontName: 'Sans-Serif',
            fontSize: 9,
            width: 400,
            height: 450,
            legend: 'bottom',
            colors: ['#2F72FF', '#9FC0FF'],
            chartArea: {
                top: 30
            }
        };


        /* -------------- Graph Widget -------------- */

        displayGraphWidgetConfig = function(widgetId, domainLabel, rangeLabel, seriesLabels, seriesValues, bagName) {
            jQuery('#widgetdata' + widgetId).hide();
            jQuery('#widgetdatanoresults' + widgetId).hide();
            jQuery('#widgetdatawait' + widgetId).show();
            var extraAttr = getExtraValue(widgetId);
            var wsCall = function wsCall(token) {
                    var request_data = {
                        widget: widgetId,
                        list: bagName,
                        filter: extraAttr,
                        token: token
                    };
                    jQuery.getJSON(service + "list/chart", request_data, function (res) {
                        if (res.results.length != 0) {
                            var viz = google.visualization;
                            var data = google.visualization.arrayToDataTable(res.results, false);
                            var targetElem = document.getElementById("widgetdata" + widgetId);
                            var Chart = null;
                            var options = jQuery.extend({}, CHART_OPTS, {
                                title: res.title
                            });
                            if (res.chartType == "ColumnChart") {
                                Chart = viz.ColumnChart;
                            } else if (res.chartType == "BarChart") {
                                Chart = viz.BarChart;
                                //jQuery.extend(options, {height: 450});
                            } else if (res.chartType == "ScatterPlot") {
                                Chart = viz.ScatterChart;
                            } else if (res.chartType == "PieChart") {
                                Chart = viz.PieChart;
                            } else if (res.chartType == "XYLineChart") {
                                Chart = viz.LineChart;
                            }

                            if (domainLabel) {
                                jQuery.extend(options, {
                                    hAxis: {
                                        title: rangeLabel,
                                        titleTextStyle: {
                                            fontName: 'Sans-Serif'
                                        }
                                    }
                                });
                            }
                            if (rangeLabel) {
                                jQuery.extend(options, {
                                    vAxis: {
                                        title: domainLabel,
                                        titleTextStyle: {
                                            fontName: 'Sans-Serif'
                                        }
                                    }
                                });
                            }
                            var viz;
                            if (Chart) {
                                viz = new Chart(targetElem);
                                viz.draw(data, options);
                                var pathQuery = res.pathQuery;
                                google.visualization.events.addListener(viz, 'select', function () {
                                    var selection = viz.getSelection();
                                    for (var i = 0; i < selection.length; i++) {
                                        var item = selection[i];
                                        if (item.row != null && item.column != null) {
                                            var category = res.results[item.row + 1][0];
                                            var series = res.results[0][item.column];
                                            var seriesValue = getSeriesValue(series, seriesLabels, seriesValues);
                                            var pathQueryWithConstraintValues = pathQuery.replace("%category", category);
                                            pathQueryWithConstraintValues = pathQueryWithConstraintValues.replace("%series", seriesValue);
                                            window.open(service + "query/results?query=" + pathQueryWithConstraintValues + "&format=html");
                                        } else if (item.row != null) {
                                            category = res.results[item.row + 1][0];
                                            pathQuery = pathQuery.replace("%category", category);
                                            window.open(service + "query/results?query=" + pathQuery + "&format=html");
                                        }
                                    }
                                });
                            } else {
                                alert("Don't know how to draw " + res.chartType + "s yet!");
                            }
                            jQuery('#widgetdata' + widgetId).show();
                        } else {
                            jQuery('#widgetdatanoresults' + widgetId).show();
                        }
                        jQuery('#widgetdatawait' + widgetId).hide();
                        calcNotAnalysed(widgetId, res.notAnalysed);
                    });
                }
                //AjaxServices.getSingleUseKey(wsCall);
                wsCall(""); // Work with public lists only so far
        }

        getSeriesValue = function(seriesLabel, seriesLabels, seriesValues) {
            var arraySeriesLabels = seriesLabels.split(",");
            var arraySeriesValues = seriesValues.split(",");
            for (var i = 0; i < arraySeriesLabels.length; i++) {
                if (seriesLabel == arraySeriesLabels[i]) {
                    return arraySeriesValues[i];
                }
            }
        }

        calcNotAnalysed = function(widgetId, notAnalysed) {
            var isMSIE = /*@cc_on!@*/
            false;
            if (!isMSIE) {
                document.getElementById('widgetnotanalysed' + widgetId).innerHTML = notAnalysed;
            }
        }


        /* -------------- Enrichment Widget -------------- */

        displayEnrichmentWidgetConfig = function(widgetId, label, bagName) {
            jQuery('#widgetdata' + widgetId).hide();
            jQuery('#widgetdatanoresults' + widgetId).hide();
            jQuery('#widgetdatawait' + widgetId).show();
            var errorCorrection;
            if ($("errorCorrection" + widgetId) != null) {
                errorCorrection = $("errorCorrection" + widgetId).value;
            }
            var max;
            if ($("max" + widgetId) != null) {
                max = $("max" + widgetId).value;
            }
            var extraAttr = getExtraValue(widgetId);

            var wsCall = function wsCall(tokenId) {
                    var request_data = {
                        widget: widgetId,
                        list: bagName,
                        correction: errorCorrection,
                        maxp: max,
                        filter: extraAttr,
                        token: tokenId
                    };
                    jQuery.getJSON(service + "list/enrichment", request_data, function (res) {
                        jQuery("#tablewidget" + widgetId + "head").empty();
                        jQuery("#tablewidget" + widgetId + "body").empty();
                        var results = res.results;
                        if (results.length != 0) {
                            var columns = [label, "p-Value", "Matches"];
                            createTableHeader(widgetId, columns)

                            //table
                            var $table = jQuery("#tablewidget" + widgetId + "body").empty();
                            var i = 0;
                            var externalLink;
                            if ($("externalLink" + widgetId) != null) {
                                externalLink = $("externalLink" + widgetId).value;
                            }

                            var externalLinkLabel;
                            if ($("externalLinkLabel" + widgetId) != null) {
                                externalLinkLabel = $("externalLinkLabel" + widgetId).value;
                            }
                            for (i in results) {
                                $table.append(make_enrichment_row(results[i], externalLink, externalLinkLabel));
                            }
                            jQuery('#widgetdata' + widgetId).show();
                        } else {
                            jQuery('#widgetdatanoresults' + widgetId).show();
                        }
                        jQuery('#widgetdatawait' + widgetId).hide();
                        calcNotAnalysed(widgetId, res.notAnalysed);
                    });
                }

                //AjaxServices.getSingleUseKey(wsCall);
                wsCall(""); // Work with public lists only so far
        }

        getExtraValue = function(widgetId) {
            var extraAttr;
            var extraAttrForm = document.getElementById("widgetselect" + widgetId);
            if (extraAttrForm != null) {
                extraAttr = extraAttrForm.value;
            }
            return extraAttr;
        }

        make_enrichment_row = function(result, externalLink, externalLinkLabel) {
            var $row = jQuery('<tr>');
            var $checkBox = jQuery('<input />').attr({
                'type': 'checkbox',
                'id': 'selected_' + result.item,
                'value': result.item,
                'name': 'selected'
            });
            $row.append(jQuery('<td>').append($checkBox));

            if (result.description) {
                $td = jQuery('<td>').text(result.description + ' ');
                if (externalLink) {
                    var label;
                    if (externalLinkLabel != undefined) {
                        label = externalLinkLabel;
                    }
                    label = label + result.item;
                    $a = jQuery('<a>').addClass('extlink').text('[' + label + ']');
                    $a.attr({
                        'target': '_new',
                        'href': externalLink + result.item
                    });
                    $td.append($a);
                }
                $row.append($td);
            } else {
                $row.append(jQuery('<td>').html("<em>no description</em>"));
            }
            $row.append(jQuery('<td>').text(result["p-value"]));
            var $count = jQuery('<span>').addClass("match-count").text(result.matches.length);
            var $matches = jQuery('<div>');
            $matches.css({
                "display": "none"
            });
            var $list = jQuery('<ul>');
            var i = 0;
            for (i in result.matches) {
                $list.append(jQuery('<li>').text(result.matches[i]));
            }
            $matches.append($list);
            $count.append($matches);
            $count.click(function () {
                $matches.slideToggle()
            });
            $row.append(jQuery('<td>').append($count));
            return $row;
        }


        /* -------------- public -------------- */

        /**
         * Examples:
         *
         * id = 		  'flyatlas'
         * domainLabel =  'Tissue'
         * rangeLabel =   'Up (+) or Down (-) gene count'
         * seriesLabels = 'Up,Down'
         * seriesValues = 'Up,Down'
         * bagName =      'myBagName'
         */
        loadGraphWidget = function(id, domainLabel, rangeLabel, seriesLabels, seriesValues, bagName) {
            google.setOnLoadCallback(function() {
            	displayGraphWidgetConfig(id, domainLabel, rangeLabel, seriesLabels, seriesValues, bagName);
            });
        }

        loadEnrichmentWidget = function(id, domainLabel, rangeLabel, seriesLabels, seriesValues, bagName) {
            google.setOnLoadCallback(function() {
            	displayEnrichmentWidgetConfig(id, label, bagName);
            });
        }

        google.load('visualization', '1.0', {'packages':['corechart']});

        return {
        	loadGraph: loadGraphWidget,
        	loadEnrichment: loadEnrichmentWidget
        };
    };
} else if (typeof window.Widgets != 'function') {
    throw new Error("Widgets library already exists and is not an object");
}