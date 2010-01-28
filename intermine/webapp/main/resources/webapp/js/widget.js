function getProcessGraphWidgetConfig(widgetId, bagName) {
  display('widgetdata' + widgetId, true);
  display('widgetdatawait' + widgetId, false);
  var extraAttr;
  var extraAttrForm = document.getElementById("widgetselect" + widgetId);
  if(extraAttrForm != null) {
	  extraAttr = extraAttrForm.value;
  }
  var pValue = document.getElementById("pValue" + widgetId);
  if(pValue != null) {
    extraAttr = pValue.value;
  }
  AjaxServices.getProcessGraphWidget(widgetId,bagName,extraAttr,handleGraphWidget);
}

function handleGraphWidget(widget) {
  display('widgetdatanoresults' + widget.configId, false);
  if(widget.hasResults) {
    var widgetdataname = document.getElementById('widgetdata' + widget.configId);
    widgetdataname.innerHTML = widget.html;
    display('widgetdatawait' + widget.configId, false);
    widgetdataname.style.display = 'block';
  } else {
    display('widgetdatawait' + widget.configId, false);
    display('widgetdata' + widget.configId, false);
    display('widgetdatanoresults' + widget.configId, true);
  }
  calcNotAnalysed(widget);
}

function getProcessHTMLWidgetConfig(widgetId, bagName) {
	AjaxServices.getProcessHTMLWidget(widgetId, bagName, handleHTMLWidget);
}

function handleHTMLWidget(widget) {
	var widgetdataname = document.getElementById('widgetdata' + widget.configId);
    var widgetdatawait = document.getElementById('widgetdatawait' + widget.configId);
    var widgetdatacontent = document.getElementById('widgetdatacontent' + widget.configId);
    Element.hide($(widgetdatawait));
    Element.hide($(widgetdataname));
    Element.show($(widgetdatacontent));
}

function getProcessTableWidgetConfig(widgetId, bagName) {
  AjaxServices.getProcessTableWidget(widgetId,bagName,handleTableWidget);
}

function handleTableWidget(widget) {
  display('widgetdatanoresults' + widget.configId, false);

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
    display('widgetdatawait' + widget.configId, false);
    display('widgetdata' + widget.configId, false);
    display('widgetdatanoresults' + widget.configId, true);
  }
  calcNotAnalysed(widget);
}

function getProcessEnrichmentWidgetConfig(widgetId, bagName) {
  display('widgetdata' + widgetId, false);
  display('widgetdatawait' + widgetId, true);
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
  if(type == 'displayAll' || checkSelected('widgetaction'+widgetId)) {
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
        var isMSIE = /*@cc_on!@*/false;
        if (!isMSIE) {
            document.getElementById('widgetnotanalysed' + widget.configId).innerHTML=widget.notAnalysed;
            //$('widgetnotanalysed' + widget.configId).update(widget.notAnalysed);
        }
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
