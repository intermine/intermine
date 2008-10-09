function reloadTagSelect(selectId, type) {
    displayTagSelect(selectId, type);
}

function callOnChangeFunction(selectId, onChangeFunction) {
    var select = document.getElementById(selectId);
    var value = select[select.selectedIndex].value;
    eval(onChangeFunction + '(value)');
}

function displayTagSelect(selectId, type) {
    var callBack = function(tags) {
		setSelectElement(selectId, '-- filter by tag --', tags);        
    }
    AjaxServices.getTags(type, callBack);
}

