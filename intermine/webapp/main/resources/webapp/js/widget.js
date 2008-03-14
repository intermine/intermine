function getProcessGraphWidget(widgetId, bagName, selectedExtraAttribute) {
  var widgetdataname = document.getElementById('widgetdata' + widgetId);
  var widgetdatawait = document.getElementById('widgetdatawait' + widgetId); 
  Element.hide($(widgetdataname));
  Element.show($(widgetdatawait));
  AjaxServices.getProcessGraphWidget(widgetId,bagName,selectedExtraAttribute,handleGraphWidget);
}

function handleGraphWidget(widget) {
  var widgetdataname = document.getElementById('widgetdata' + widget.id);
  var widgetdatawait = document.getElementById('widgetdatawait' + widget.id); 
  Element.update($(widgetdataname),widget.html);
  Element.hide($(widgetdatawait));
  Element.show($(widgetdataname));
}


function getProcessTableWidget(widgetId, bagName) {
  AjaxServices.getProcessTableWidget(widgetId,bagName,handleTableWidget);
}

function handleTableWidget(widget) {
  var widgetdataname = document.getElementById('widgetdata' + widget.id);
  var widgetdatawait = document.getElementById('widgetdatawait' + widget.id);
  var row = null;
  row = document.createElement("tr");
  for(var i = 0; i < widget.columns.length ; i++){
    var cell = document.createElement("td");
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
  
  //Element.update($(widgetdataname)wdget.html);
  Element.hide($(widgetdatawait));
  Element.show($(widgetdataname));
}
