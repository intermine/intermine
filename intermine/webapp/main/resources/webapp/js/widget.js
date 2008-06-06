function getProcessGraphWidget(widgetId, bagName) {
  var widgetdataname = document.getElementById('widgetdata' + widgetId);
  var widgetdatawait = document.getElementById('widgetdatawait' + widgetId);

  Element.hide($(widgetdataname));
  Element.show($(widgetdatawait));
  var extraAttr;
  if($("widgetselect" + widgetId)!=null) {
  	extraAttr = $("widgetselect" + widgetId).value;
  }
  AjaxServices.getProcessGraphWidget(widgetId,bagName,extraAttr,handleGraphWidget);
}

function handleGraphWidget(widget) {

  calcNotAnalysed(widget);

  Element.hide($('widgetdatanoresults' + widget.id));
  if(widget.hasResults) {
    var widgetdataname = document.getElementById('widgetdata' + widget.id);
    var widgetdatawait = document.getElementById('widgetdatawait' + widget.id);

    Element.update($(widgetdataname),widget.html);
    Element.hide($(widgetdatawait));
    Element.show($(widgetdataname));
  } else {
  	Element.hide($('widgetdatawait' + widget.id));
    Element.hide($('widgetdata' + widget.id));
    Element.show($('widgetdatanoresults' + widget.id));
    //toggleWidget('widgetcontainer' + widget.id, 'togglelink' + widget.id);
  }
}

function getProcessTableWidget(widgetId, bagName) {
  AjaxServices.getProcessTableWidget(widgetId,bagName,handleTableWidget);
}

function handleTableWidget(widget) {

  calcNotAnalysed(widget);

  Element.hide($('widgetdatanoresults' + widget.id));

  if(widget.hasResults) {
	  removeChildren($("tablewidget"+widget.id+"head"));
	  removeChildren($("tablewidget"+widget.id+"body"));
	  var widgetdataname = document.getElementById('widgetdata' + widget.id);
	  var widgetdatawait = document.getElementById('widgetdatawait' + widget.id);

	  var row = document.createElement("tr");

	  // checkbox
	  var checkboxCell = document.createElement("th");
	  var formName = "widgetaction" + widget.id;
	  var checky = "<input type=\"checkbox\" name=\"select_all\" id=\"selected_all" + widget.id + "\"";
          checky += " onclick=\"toggleAllChecks('" + formName + "', " + widget.id + ")\">";
	  checkboxCell.innerHTML = checky;
	  row.appendChild(checkboxCell);

      // column names
	  for(var i = 0; i < widget.columns.length ; i++){
	    var cell = document.createElement("th");
	    cell.innerHTML = widget.columns[i];
	    row.appendChild(cell);
	  }

	  $("tablewidget"+widget.id+"head").appendChild(row);

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

	    $("tablewidget"+widget.id+"body").appendChild(row);
	  }
	  Element.hide($(widgetdatawait));
      Element.show($(widgetdataname));
  } else {
    Element.hide($('widgetdatawait' + widget.id));
    Element.hide($('widgetdata' + widget.id));
    Element.show($('widgetdatanoresults' + widget.id));
    //toggleWidget('widgetcontainer' + widget.id, 'togglelink' + widget.id);
  }
}

function getProcessEnrichmentWidget(widgetId, bagName) {
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
    $('widgetnotanalysed' + widget.id).update(widget.notAnalysed);
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