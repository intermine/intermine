var CHART_OPTS = {
    fontName: 'Sans-Serif',
    fontSize: 09,
    width: 400,
    height:450,
    legend: 'bottom',
    colors:['#2F72FF', '#9FC0FF'],
    chartArea:{top:30}
};

function displayGraphWidgetConfig(widgetId, domainLabel, rangeLabel, link, bagName) {
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
    jQuery.getJSON(service + "list/chart", request_data, function(res) {
    if (res.results.length != 0) {
      var viz = google.visualization;
      var data = google.visualization.arrayToDataTable(res.results, false);
      var targetElem = document.getElementById("widgetdata" + widgetId);
      var Chart = null;
      var options = jQuery.extend({}, CHART_OPTS, {title: res.title});
      if (res.chartType == "ColumnChart") {
          Chart = viz.ColumnChart;
          jQuery.extend(options, {reverseCategories: true});
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
          jQuery.extend(options, {hAxis: {title: rangeLabel, titleTextStyle: {fontName: 'Sans-Serif'}}});
      }
      if (rangeLabel) {
          jQuery.extend(options, {vAxis: {title: domainLabel, titleTextStyle: {fontName: 'Sans-Serif'}}});
      }
      var viz;
      if (Chart) {
          viz = new Chart(targetElem);
          viz.draw(data, options);
          if (link != "") {
            google.visualization.events.addListener(viz, 'select', function() {
              var selection = viz.getSelection();
              for (var i = 0; i < selection.length; i++) {
                var item = selection[i];
                if (item.row != null && item.column != null) {
                    var category = res.results[item.row + 1][0];
                    var series = res.results[0][item.column];
                    window.location.assign(webappUrl + "queryForGraphAction.do?bagName=" + bagName
                      + "&category=" + category + "&series=" + series + "&urlGen=" + link);
                } else if (item.row != null) {
                  category = res.results[item.row + 1][0];
                  window.location.assign(webappUrl + "queryForGraphAction.do?bagName=" + bagName
                          + "&category=" + category + "&series=&urlGen=" + link);
                }
              }
            });
          }
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
  AjaxServices.getSingleUseKey(wsCall);
}

function getExtraValue(widgetId) {
  var extraAttr;
  var extraAttrForm = document.getElementById("widgetselect" + widgetId);
  if(extraAttrForm != null) {
    extraAttr = extraAttrForm.value;
  }
  return extraAttr;
 }

function getProcessHTMLWidgetConfig(widgetId, bagName) {
  AjaxServices.getProcessHTMLWidget(widgetId, bagName, handleHTMLWidget);
}

function handleHTMLWidget(widget) {
  jQuery('#widgetdata' + widget.configId).hide();
  jQuery('#widgetdatanoresults' + widget.configId).hide();
  jQuery('#widgetdatawait' + widget.configId).show();
}

function getProcessTableWidgetConfig(widgetId, bagName) {
  AjaxServices.getProcessTableWidget(widgetId,bagName,handleTableWidget);
}

function createTableHeader(widgetId, columns) {
   var $row = jQuery('<tr>');

    // checkbox
    var formName = "widgetaction" + widgetId;
    var $checkBoxAll = jQuery('<input />').attr({'type':'checkbox', 'id':'selected_all' + widgetId,
                                                 'name': 'select_all'});
    $checkBoxAll.click(function(){toggleAllChecks(formName, widgetId)});
    $row.append(jQuery('<th>').append($checkBoxAll));
    // column names
    for(var i = 0; i < columns.length ; i++){
      $row.append(jQuery('<th>').text(columns[i]));
    }
    jQuery("#tablewidget"+widgetId+"head").append($row);
}

function handleTableWidget(widget) {
  jQuery('#widgetdata' + widget.configId).hide();
  jQuery('#widgetdatanoresults' + widget.configId).hide();
  jQuery('#widgetdatawait' + widget.configId).show();
  if(widget.hasResults) {
    jQuery("#tablewidget"+widget.configId+"head").empty();
    jQuery("#tablewidget"+widget.configId+"body").empty();

    createTableHeader(widget.configId, widget.columns);

    for(var i = 0; i < widget.flattenedResults.length ; i++) {
      $row = jQuery("<tr>");
      var columns = widget.flattenedResults[i];
      for(var j = 0; j < columns.length ; j++) {
         var $cell = jQuery("<td>");
         if(columns[j]!=null) {
             if(columns[j][1] != null) {
               $a = jQuery('<a>').text(columns[j][0]);
               $a.attr("href",columns[j][1]);
               $cell.append($a);
             } else {
               $cell.text(columns[j][0]);
             }
         }
         $row.append($cell);
      }
      jQuery("#tablewidget"+widget.configId+"body").append($row);
    }

     jQuery('#widgetdatawait' + widget.configId).hide();
     jQuery('#widgetdata' + widget.configId).show();
  } else {
    jQuery('#widgetdatawait' + widget.configId).hide();
    jQuery('#widgetdatanoresults' + widget.configId).show();
  }
  calcNotAnalysed(widget.configId, widget.notAnalysed);
}

function displayEnrichmentWidgetConfig(widgetId, label, bagName) {
    jQuery('#widgetdata' + widgetId).hide();
    jQuery('#widgetdatanoresults' + widgetId).hide();
    jQuery('#widgetdatawait' + widgetId).show();
    var errorCorrection;
    if($("errorCorrection" + widgetId)!=null) {
      errorCorrection = $("errorCorrection" + widgetId).value;
    }
    var max;
    if($("max" + widgetId)!=null) {
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
      jQuery.getJSON(service + "list/enrichment", request_data, function(res) {
        jQuery("#tablewidget"+widgetId+"head").empty();
        jQuery("#tablewidget"+widgetId+"body").empty();
        var results = res.results;
        if (results.length != 0) {
          var columns = [label, "p-Value", "Matches"];
          createTableHeader(widgetId, columns)

          //table
          var $table = jQuery("#tablewidget"+widgetId+"body").empty();
          var i = 0;
          var externalLink;
          if($("externalLink" + widgetId)!=null) {
           externalLink = $("externalLink" + widgetId).value;
          } 

          var externalLinkLabel;
          if($("externalLinkLabel" + widgetId)!=null) {
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

  AjaxServices.getSingleUseKey(wsCall);
}

function make_enrichment_row(result, externalLink, externalLinkLabel) {
    var $row = jQuery('<tr>');
    var $checkBox = jQuery('<input />').attr({'type':'checkbox', 'id':'selected_' +result.item,
                                             'value': result.item, 'name': 'selected'});
    $row.append(jQuery('<td>').append($checkBox));

    if (result.description) {
        $td = jQuery('<td>').text(result.description + ' ');
        if(externalLink) {
          var label;
          if (externalLinkLabel != undefined) {
            label = externalLinkLabel;
          }
          label = label + result.item;
          $a = jQuery('<a>').addClass('extlink').text('[' + label + ']');
          $a.attr({'target': '_new', 'href': externalLink + result.item});
          $td.append($a);
        }
        $row.append($td);
    } else {
        $row.append(jQuery('<td>').html("<em>no description</em>"));
    }
    $row.append(jQuery('<td>').text(result["p-value"]));
    var $count = jQuery('<span>').addClass("match-count").text(result.matches.length);
    var $matches = jQuery('<div>');
    $matches.css({"display": "none"});
    var $list = jQuery('<ul>');
    var i = 0;
    for (i in result.matches) {
        $list.append(jQuery('<li>').text(result.matches[i]));
    }
    $matches.append($list);
    $count.append($matches);
    $count.click(function() {$matches.slideToggle()});
    $row.append(jQuery('<td>').append($count));
    return $row;
}

function checkSelected(formName) {
  var form = document.getElementById(formName);
  var elts = form.getElementsByTagName("input");
  for(var i=0;i<elts.length;i++) {
    if( elts[i].name=='selected' && elts[i].checked) {
      return true;
    }
  }
  return false;
}

function submitWidgetForm(widgetId,type,extra) {
    if (document.getElementById('selected_all' + widgetId) != null) {
        var formName = 'widgetaction'+widgetId;
        if(formName && !checkSelected(formName)) {
            document.getElementById('selected_all' + widgetId).checked = true;
            toggleAllChecks(formName, widgetId);
        }
        $('action'+widgetId).value=type;
        $('export' + widgetId).value=extra;
        $('widgetaction' + widgetId).submit();
    }
}

function calcNotAnalysed(widgetId, notAnalysed) {
    var isMSIE = /*@cc_on!@*/false;
    if (!isMSIE) {
        document.getElementById('widgetnotanalysed' + widgetId).innerHTML=notAnalysed;
    }
}

function toggleAllChecks(formName, widgetId) {
  var checked = document.getElementById('selected_all' + widgetId).checked;
  var form = document.getElementById(formName);
  var elts = form.getElementsByTagName("input");
  for(var i=0;i<elts.length;i++) {
    if( elts[i].name=='selected') {
        elts[i].checked=checked;
    }
  }
}