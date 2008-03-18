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
  var widgetdataname = document.getElementById('widgetdata' + widget.id);
  var widgetdatawait = document.getElementById('widgetdatawait' + widget.id); 
  Element.update($(widgetdataname),widget.html);
  Element.hide($(widgetdatawait));
  Element.hide($('widgetdatanoresults' + widget.id));
  Element.show($(widgetdataname));
}


function getProcessTableWidget(widgetId, bagName) {
  AjaxServices.getProcessTableWidget(widgetId,bagName,handleTableWidget);
}

function handleTableWidget(widget) {
  Element.hide($('widgetdatanoresults' + widget.id));
  if(widget.hasResults) {
	  removeChildren($("tablewidget"+widget.id+"head"));
	  removeChildren($("tablewidget"+widget.id+"body"));
	  var widgetdataname = document.getElementById('widgetdata' + widget.id);
	  var widgetdatawait = document.getElementById('widgetdatawait' + widget.id);
	  var row = null;
	  row = document.createElement("tr");
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
  	// TODO add a no results message
    Element.hide($('widgetdatawait' + widget.id));
    Element.hide($('widgetdata' + widget.id));
    Element.show($('widgetdatanoresults' + widget.id));
  	toggleWidget('widgetcontainer' + widget.id, 'togglelink' + widget.id);
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
  AjaxServices.getProcessEnrichmentWidget(widgetId,bagName,errorCorrection,max,handleTableWidget);
}

function removeChildren(node) {
	while(node.childNodes.length > 0) {
		node.removeChild(node.childNodes[0]);
	}
}
