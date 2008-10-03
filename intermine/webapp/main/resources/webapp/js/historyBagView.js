function filterByTag(tag) {
	filterTableName = 'bagTable';
	if (tag != '') {
		var callBack = filterTable;
		AjaxServices.filterByTag("${type}", tag, callBack);
	} else {
		showAllTable(filterTableName);
	}
}

function showAllTable(tableName) {
	var els = getTableRows(tableName);
	for (var i = 0; i < els.length; i++) {
		displayEl(els[i]);
	}
} 

function getTableRows(tableName) {
	return document.getElementById(tableName).getElementsByTagName('tr');
}

function filterTable(filteredList) {
	if  (filteredList == null) {
		showAllTable(filterTableName);
	} else {
		var els = getTableRows(filterTableName);
		// first row is header row
		for (var i = 1; i < els.length; i++) {
			var el = els[i];
			var objectName = el.id;
			if (isInArray(filteredList, objectName)) {
				displayEl(el);
			} else {
				hideEl(el);
			}
		}	
	}
}

function isInArray(array, item) {
	if (array == null) {
		return false;
	}
	for (var i = 0; i < array.length; i++) {
		if (array[i] == item) {
			return true;
		}
	}
	return false;
}