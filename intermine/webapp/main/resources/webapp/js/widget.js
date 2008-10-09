function getProcessGraphWidgetConfig(widgetId, bagName) {
  var widgetdataname = document.getElementById('widgetdata' + widgetId);
  var widgetdatawait = document.getElementById('widgetdatawait' + widgetId);

  Element.hide($(widgetdataname));
  Element.show($(widgetdatawait));
  var extraAttr;
  if($("widgetselect" + widgetId)!=null) {
    extraAttr = $("widgetselect" + widgetId).value;
  }

  if($("pValue" + widgetId)!=null) {
    extraAttr = $("pValue" + widgetId).value;
  }
  AjaxServices.getProcessGraphWidget(widgetId,bagName,extraAttr,handleGraphWidget);
}

function handleGraphWidget(widget) {

  calcNotAnalysed(widget);

  Element.hide($('widgetdatanoresults' + widget.configId));
  if(widget.hasResults) {
    var widgetdataname = document.getElementById('widgetdata' + widget.configId);
    var widgetdatawait = document.getElementById('widgetdatawait' + widget.configId);

    Element.update($(widgetdataname),widget.html);
    Element.hide($(widgetdatawait));
    Element.show($(widgetdataname));
  } else {
    Element.hide($('widgetdatawait' + widget.configId));
    Element.hide($('widgetdata' + widget.configId));
    Element.show($('widgetdatanoresults' + widget.configId));
    //toggleWidget('widgetcontainer' + widget.configId, 'togglelink' + widget.configId);
  }
}

function getProcessTableWidgetConfig(widgetId, bagName) {
  AjaxServices.getProcessTableWidget(widgetId,bagName,handleTableWidget);
}

function handleTableWidget(widget) {

  calcNotAnalysed(widget);

  Element.hide($('widgetdatanoresults' + widget.configId));

  if(widget.hasResults) {
    removeChildren($("tablewidget"+widget.configId+"head"));
    removeChildren($("tablewidget"+widget.configId+"body"));
    var widgetdataname = document.getElementById('widgetdata' + widget.configId);
    var widgetdatawait = document.getElementById('widgetdatawait' + widget.configId);

    var row = document.createElement("tr");

    // checkbox
    var checkboxCell = document.createElement("th");
    var formName = "widgetaction" + widget.configId;
    var checky = "<input type=\"checkbox\" name=\"select_all\" id=\"selected_all" + widget.configId + "\"";
          checky += " onclick=\"toggleAllChecks('" + formName + "','" + widget.configId + "')\">";
    checkboxCell.innerHTML = checky;
    row.appendChild(checkboxCell);

      // column names
    for(var i = 0; i < widget.columns.length ; i++){
      var cell = document.createElement("th");
      cell.innerHTML = widget.columns[i];
      row.appendChild(cell);
    }

    $("tablewidget"+widget.configId+"head").appendChild(row);

  var bagContent;
  if(widget.title == "Protein Interactions") {
     bagContent = widget.elementInList;
   }

  for(var i = 0; i < widget.flattenedResults.length ; i++){
    row = document.createElement("tr");
      var columns = widget.flattenedResults[i];
      for(var j = 0; j < columns.length ; j++){
      var cell = document.createElement("td");
         if(columns[j]!=null) {
             if(columns[j][1] != null) {
               var link = document.createElement("a");
               link.setAttribute("href",columns[j][1]);
               link.innerHTML = columns[j][0];
               cell.appendChild(link);
             } else {
              cell.innerHTML = columns[j][0];
             }
         }
         row.appendChild(cell);
    }

      $("tablewidget"+widget.configId+"body").appendChild(row);
    }
    Element.hide($(widgetdatawait));
      Element.show($(widgetdataname));
  } else {
    Element.hide($('widgetdatawait' + widget.configId));
    Element.hide($('widgetdata' + widget.configId));
    Element.show($('widgetdatanoresults' + widget.configId));
    //toggleWidget('widgetcontainer' + widget.configId, 'togglelink' + widget.configId);
  }
}

function getProcessGridWidgetConfig(widgetId, bagName) {
  var widgetdataname = document.getElementById('widgetdata' + widgetId);
  var widgetdatawait = document.getElementById('widgetdatawait' + widgetId);

  Element.hide($(widgetdataname));
  Element.show($(widgetdatawait));

  var externalLink;
  if($("externalLink" + widgetId)!=null) {
    externalLink = $("externalLink" + widgetId).value;
  }

  var externalLinkLabel;
  if($("externalLinkLabel" + widgetId)!=null) {
    externalLinkLabel = $("externalLinkLabel" + widgetId).value;
  }

  var highlight;
  if($("highlight" + widgetId)!=null) {
    highlight = $("highlight" + widgetId).value;
  }

  var numberOpt;
  if($("numberOpt" + widgetId)!=null) {
    numberOpt = $("numberOpt" + widgetId).value;
  }

  var pValue;
  if($("pValue" + widgetId)!=null) {
    pValue = $("pValue" + widgetId).value;
  }
  AjaxServices.getProcessGridWidget(widgetId,bagName,highlight,pValue,numberOpt,externalLink,externalLinkLabel,handleGridWidget);
}

function handleGridWidget(widget) {

  calcNotAnalysed(widget);

    Element.hide($('widgetdatanoresults' + widget.configId));

  if(widget.hasResults) {
    removeChildren($("tablewidget"+widget.configId+"head"));
    removeChildren($("tablewidget"+widget.configId+"body"));
    var widgetdataname = document.getElementById('widgetdata' + widget.configId);
    var widgetdatawait = document.getElementById('widgetdatawait' + widget.configId);

    var row = document.createElement("tr");
    var cell = document.createElement("th");
    cell.width = "40";
    cell.innerHTML = "/";
      row.appendChild(cell);
      // column names
    for(var i = 0; i < widget.columns.length ; i++){
      cell = document.createElement("th");
      cell.width = "40";
      if(widget.columns[i].length <= 5) {
        cell.innerHTML = '<font size=\"1px\" >' + widget.columns[i] + '</font>';
      } else {
        cell.innerHTML = '<font size=\"1px\" >' + widget.columns[i].substring(0,5) + "..." + '</font>';
        cell.title = widget.columns[i];
      }
      row.appendChild(cell);
    }

     $("tablewidget"+widget.configId+"head").appendChild(row);
     //for display
     var counter = 1;
     var display = 0;
     //for special table
     var specialtable = widget.columns.length + 1;
     var specialTableCounter = 1;
    // column names
    for(var i = 0; i < widget.columns.length ; i++){

      var results = widget.flattenedResults[i];
      var resultsDown = 0;
      if (widget.elementInList.length > 0) {
        resultsDown = widget.elementInList[i];
      }

      row = document.createElement("tr");
      var cell = document.createElement("th");
      if(widget.columns[i].length <= 5) {
        cell.innerHTML = '<font size=\"1px\" >' +  widget.columns[i] + '</font>';
      } else {
        cell.innerHTML ='<font size=\"1px\" >' +  widget.columns[i].substring(0,5) + "..." + '</font>';
        cell.title = widget.columns[i];
      }
      row.appendChild(cell);

      for(var k = 0; k < widget.columns.length ; k++){

       cell = document.createElement("td");
       if (resultsDown.length > 0) {
        if(specialTableCounter%specialtable == 1) {
             var newTable = document.createElement("table");
             var newrow = document.createElement("tr");
             var newCell = document.createElement("td");
             newrow.appendChild(newCell)
             newCell = document.createElement("td");
             var link = document.createElement("a");
              link.setAttribute("href", results[k][1]);
              link.innerHTML ='<font size=\"1px\" >' +  results[k][0] + '</font>';
              link.title = (results[k][3]);
               newCell.appendChild(link);
               var rgb = "#" + results[k][2];
               newCell.style.background = rgb;
             newrow.appendChild(newCell)
             newTable.appendChild(newrow);
             newrow = document.createElement("tr");
             newCell = document.createElement("td");
             var link = document.createElement("a");
               link.setAttribute("href", resultsDown[k][1]);
              link.innerHTML ='<font size=\"1px\" >' +  resultsDown[k][0] + '</font>';
              link.title = (resultsDown[k][3]);
               newCell.appendChild(link);
               var rgb = "#" + resultsDown[k][2];
               newCell.style.background = rgb;
             newrow.appendChild(newCell);
             newCell = document.createElement("td");
             newrow.appendChild(newCell);
             newTable.appendChild(newrow);
             newTable.width = "100%";
             cell.appendChild(newTable);
         } else if(counter > display) {
                 var link = document.createElement("a");
                 link.setAttribute("href", results[k][1]);
                link.innerHTML ='<font size=\"1px\" >' +  results[k][0] + '</font>';
                link.title = (results[k][3]);
                 cell.appendChild(link);
                 var rgb = "#" + results[k][2];
                 cell.style.background = rgb;
             } else {
               var link = document.createElement("a");
                 link.setAttribute("href", resultsDown[k][1]);
                link.innerHTML ='<font size=\"1px\" >' +  resultsDown[k][0] + '</font>';
                link.title = (resultsDown[k][3]);
                 cell.appendChild(link);
                 var rgb = "#" + resultsDown[k][2];
                 cell.style.background = rgb;
             }
       } else {
         if(counter > display) {
                 var link = document.createElement("a");
                 link.setAttribute("href", results[k][1]);
                link.innerHTML ='<font size=\"1px\" >' +   results[k][0] + '</font>';
                link.title = (results[k][3]);
                 cell.appendChild(link);
                 var rgb = "#" + results[k][2];
                 cell.style.background = rgb;
             }
           }
           counter++;
           specialTableCounter++;
         row.appendChild(cell);

      }
      counter = 1;
      display++;
      $("tablewidget"+widget.configId+"head").appendChild(row);
    }


    Element.hide($(widgetdatawait));
      Element.show($(widgetdataname));

  } else {
    Element.hide($('widgetdatawait' + widget.configId));
    Element.hide($('widgetdata' + widget.configId));
    Element.show($('widgetdatanoresults' + widget.configId));
  }
}

function getProcessEnrichmentWidgetConfig(widgetId, bagName) {
  var widgetdataname = document.getElementById('widgetdata' + widgetId);
  var widgetdatawait = document.getElementById('widgetdatawait' + widgetId);

  Element.hide($(widgetdataname));
  Element.show($(widgetdatawait));
  var errorCorrection;
  if($("errorCorrection" + widgetId)!=null) {
    errorCorrection = $("errorCorrection" + widgetId).value;
  }
  var max;
  if($("max" + widgetId)!=null) {
    max = $("max" + widgetId).value;
  }
  var extraAttr;
  if($("widgetselect" + widgetId)!=null) {
    extraAttr = $("widgetselect" + widgetId).value;
  }
  var externalLink;
  if($("externalLink" + widgetId)!=null) {
    externalLink = $("externalLink" + widgetId).value;
  }

  var externalLinkLabel;
  if($("externalLinkLabel" + widgetId)!=null) {
    externalLinkLabel = $("externalLinkLabel" + widgetId).value;
  }
  AjaxServices.getProcessEnrichmentWidget(widgetId,bagName,errorCorrection,max,extraAttr,externalLink,externalLinkLabel,handleTableWidget);
}

function removeChildren(node) {
  while(node.childNodes.length > 0) {
    node.removeChild(node.childNodes[0]);
  }
}

function checkSelected(formName) {
  var elts = Form.getElements(formName);
  for(var i=0;i<elts.length;i++) {
    if( $(elts[i]).name=='selected' && $(elts[i]).checked ) {
     return true;
    }
  }
  alert("Please select some items")
  return false;
}

function submitWidgetForm(widgetId,type,extra) {
  if(checkSelected('widgetaction'+widgetId)){
    $('action'+widgetId).value=type;
    $('export' + widgetId).value=extra;
    $('widgetaction' + widgetId).submit();
  }
}

function displayNotAnalysed(widgetId,type,extra) {
    $('action'+widgetId).value='notAnalysed';
    $('widgetaction' + widgetId).submit();
}

function calcNotAnalysed(widget) {
    $('widgetnotanalysed' + widget.configId).update(widget.notAnalysed);
}

function toggleAllChecks(formName, widgetId) {
  var checked = document.getElementById('selected_all' + widgetId).checked;
  var elts = Form.getElements(formName);
  for(var i=0;i<elts.length;i++) {
    if( $(elts[i]).name=='selected') {
        $(elts[i]).checked=checked;
    }
  }
}