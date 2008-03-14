function getProcessGraphWidget(widgetId, bagName, selectedExtraAttribute) {
  AjaxServices.getProcessGraphWidget(widgetId,bagName,selectedExtraAttribute,handleGraphWidget);
}

function handleGraphWidget(widget) {
  var widgetdataname = document.getElementById('widgetdata' + widget.id);
  var widgetdatawait = document.getElementById('widgetdatawait' + widget.id); 
  Element.update($(widgetdataname),widget.html);
  Element.hide($(widgetdatawait));
  Element.show($(widgetdataname));
}
