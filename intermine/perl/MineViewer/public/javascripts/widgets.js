var widgets, $MODEL;
var CHART_OPTS = {
    fontName: 'Sans-Serif',
    width: 700,
    legend: 'bottom'
};

function make_option(widget) {
    var $option = $('<option>');
    $option.attr("value", widget.name);
    $option.text(widget.title);
    return $option;
}

function displayWidget() {
    var $select = $('#widget-selector');
    $('.widget-display').hide();
    var widget_name = $select.val();
    var widget = widgets[widget_name];
    if (! widget) {
        return false;
    }
    $('#enrichment-options').hide();

    var valid_classes = _(widget.targets).chain()
        .map(   function(x)    {return getSubclassesOf(x)})
        .reduce(function(x, y) {return x.concat(y)})
        .value()
    if ($.inArray(getCurrentListType(), valid_classes) < 0) {
        $('#widget-apology').show();
        $('#widget-apology').find('.types').text(valid_classes.join(", "));
        return;
    }
    $('#widget-description p').html(widget.description).parent().show();
    if (widget.widgetType == "enrichment") {
        $('#enrichment-options').show();
        displayEnrichmentWidget(widget_name, widget);
    } else if (widget.widgetType == "chart") {
        displayChartWidget(widget_name, widget);
    } else {
        alert("Sorry - I can't display " + widget.widgetType + " widgets");
    }
}

var _subclass_map = {};

function getSubclassesOf(cls) {
    if (cls in _subclass_map) {
        return _subclass_map[cls];
    }
    var ret = [cls];
    _($MODEL.classes).each(function(c) {
        if (_(c.extends).include(cls)) {
            ret = ret.concat(getSubclassesOf(c.name));
        }
    });
    _subclass_map[cls] = ret;
    return ret;
}


function getChartFilter() {
    // TODO - make this not a constant...
    return "Drosophila melanogaster";
}

function displayChartWidget(widget_name, widget) {
    var request_data = {
        widget: widget_name,
        list: getCurrentList(),
        filter: getChartFilter(),
        token: $TOKEN
    };
    $.getJSON(service + "list/chart", request_data, function(res) {
        var viz = google.visualization;
        var data = viz.arrayToDataTable(res.results, false);
        var targetElem = document.getElementById('widget-chart');
        var Chart = null;
        var options = $.extend({}, CHART_OPTS, {title: res.title});
        if (res.chartType == "BarChart") {
            Chart = viz.ColumnChart;
            options.fontSize = 14;
        } else if (res.chartType == "StackedBarChart") {
            Chart = viz.BarChart;
            $.extend(options, {isStacked: true, height: 500});
        } else if (res.chartType == "ScatterPlot") {
            Chart = viz.ScatterChart;
            $.extend(options, {series: {0: {pointSize: 3, lineWidth: 0}, 1: {pointSize: 0, lineWidth: 1}}});
        } else if (res.chartType == "PieChart") {
            Chart = viz.PieChart;
        } else if (res.chartType == "XYLineChart") {
            Chart = viz.ScatterChart;
            $.extend(options, {series: {0: {pointSize: 0, lineWidth: 2}, 1: {pointSize: 0, lineWidth: 1}}});
        } else if (res.chartType == "Histogram") {
            return plotHistogram(targetElem, res, widget);
        }

        if (widget.labels) {
            $.extend(options, {hAxis: {title: widget.labels.x}, vAxis: {title: widget.labels.y}});
        }
        if (Chart) {
            new Chart(targetElem).draw(data, options);
        } else {
            alert("Don't know how to draw " + res.chartType + "s yet!");
        }
        $('#widget-chart').show();
    });
}

function plotHistogram(elem, res, widget) {
    var i = 0;
    var mod = 5;
    var l = res.results.length;
    var step = (res.results[2][0] - res.results[1][0]) * mod;
    console.log(step);
    var series = [{data: [], lines: {show: true}}, {data: [], bars: {show: true, barWidth: step}}];
    var count = 0;
    for (i = 1; i < l; i++) {
        series[0].data.push([res.results[i][0], res.results[i][2]]);
        count += res.results[i][1];
        if (i % mod == 0) {
            series[1].data.push([res.results[i][0] - step, count]);
            count = 0;
        }
    }
    if (count != 0) {
        series[1].data.push([res.results[i][0] - step, count]);
    }

    var options = {colors: ["#3366CC", "#DC3912"]};
    console.log(series, options);
    $(elem).css({width: "600px", height: "300px"});
    $(elem).show();
    $.plot($(elem), series, options);
}

function displayEnrichmentWidget(widget_name, widget) {
    var request_data = {
        widget: widget_name,
        list: getCurrentList(),
        correction: $('#correction-selector').val(),
        maxp: $('#max-p-value').val(),
        token: $TOKEN
    };
    if (widget.filters.length > 0) {
        request_data.filter = $('#filter-selector').val();
    }
    var $table = $('table.widget-display').children("tbody").empty();
    $table.append("Working...");
    $('table.widget-display').slideDown();
    $.getJSON(service + "list/enrichment", request_data, function(res) {
        var results = res.results;
        var i = 0;
        $table.empty();
        for (i in results) {
            $table.append(make_enrichment_row(results[i]));
        }
        if (results.length == 0) {
            $table.append("No results");
        }
    });
}

function make_enrichment_row(result) {
    var $row = $('<tr>');
    $row.append($('<td>').text(result.item));
    if (result.description) {
        $row.append($('<td>').text(result.description));
    } else {
        $row.append($('<td>').html("<em>no description</em>"));
    }
    $row.append($('<td>').text(result["p-value"]));
    var $count = $('<span>').addClass("match-count").text(result.matches.length);
    var $matches = $('<div>');
    $matches.css({"display": "none"});
    var $list = $('<ul>');
    var i = 0;
    for (i in result.matches) {
        $list.append($('<li>').text(result.matches[i]));
    }
    $matches.append($list);
    $count.click(function() {$matches.slideToggle()});
    $row.append($('<td>').append($count).append($matches));
    return $row;
}

$(function() {
    if (typeof($MODEL) == "undefined") {
        $.getJSON(service + "model/json", function(res) {
            $MODEL = res.model;
        });
    }
    var $select = $('#widget-selector');
    $.getJSON(service + "widgets" , function(res) {
        widgets = {}
        var i = 0;
        var w;
        var options = {enrichment: [], chart: []};
        for (i in res.widgets) {
            w = res.widgets[i];
            if (w.widgetType in options) {
                options[w.widgetType].push(make_option(w));
            }
            widgets[w.name] = w;
        }
        var $enrichment_group = $("<optgroup label='Enrichment'>");
        for (i in options.enrichment) {
            $enrichment_group.append(options.enrichment[i]);
        }
        $select.append($enrichment_group);

        var $chart_group = $("<optgroup label='Charts'>");
        for (i in options.chart) {
            $chart_group.append(options.chart[i]);
        }
        $select.append($chart_group);
    });

    // Update the widget being displayed if either a new widget,
    // or a new list is selected.
    $select.change(function() {
        var widget_name = $select.val();
        var widget = widgets[widget_name];
        if (!widget) {
            return;
        }
        var $filters = $('#filter-selector').empty();
        var i = 0;
        if (widget.filters && widget.filters.length > 0) {
            for (i in widget.filters) {
                $filters.append($('<option>').text(widget.filters[i]));
            }
            $filters.show();
        } else {
            $filters.hide();
        }
        displayWidget();
    });

    $('#lists').bind("multiselectclick", function(event, ui) {
        $select.change();
    });

    $('#filter-selector').change(displayWidget);
    $('#correction-selector').change(displayWidget);
    $('#max-p-value').change(displayWidget);

    $('#p-value-slider').slider({
        value: 5,
        min: 1,
        max: 100,
        step: 1,
        slide: function(event, ui) {
            $('#max-p-value').val(ui.value / 100);
        },
        stop: function(event, ui) {
            $('#max-p-value').change();
        }
    });

});


