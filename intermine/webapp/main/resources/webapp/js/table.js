/**
 * Called when a wholc column is selected/deselected
 */
function selectColumnCheckbox(columnsToDisable, columnsToHighlight, columnIndex) {
    selectColumnCheckboxes(columnsToDisable, columnsToHighlight, columnIndex);
    if (isClear()) {
        enableAll();
    }
}

/**
 * Enables all checkboxes
 */
function enableAll() {
	var array = $$('input.selectable');
    array.each(function(item) {
    	item.disabled = false
    });
}


/**
 * Checks that nothing is selected
 */
function isClear() {
	var isclear = true;
    var array = $$('.selectable');
    array.each(function(item) {
        if(item.checked == true) {
        	isclear = false;
        }
    });
    return isclear;
}


/**
 * Called by selectColumnCheckbox
 */
function selectColumnCheckboxes(columnsToDisable, columnsToHighlight, columnIndex, tableid) {
	$$('.selectable').each(function(item) {
	   if(item.id.split("_")[1] == columnIndex) {
           item.checked = $('selectedObjects_' + columnIndex).checked;
           var bits = item.value.split(',');
           var checkedRow = bits[1];
           itemChecked(columnsToDisable, columnsToHighlight, checkedRow,
                            columnIndex, tableid, item, true);
	   }
	});
    setToolbarAvailability(!$('selectedObjects_' + columnIndex).checked);
}

/**
 * Run when a user selects a keyfield in the results table.  internal is true
 * when called from other methods in this file (ie. not from an onclick in table.jsp)
 **/
function itemChecked(columnsToDisable, columnsToHighlight, checkedRow, checkedColumn, tableid, checkbox, internal) {
    if (bagType == null) {
        var columnsToDisableArray = columnsToDisable[checkedColumn];
        if (columnsToDisableArray != null) {
        	columnsToDisableArray.each(function(item) {
        	  disableColumn(item);
        	});
        }
        bagType = checkedColumn;
    }
    if (!internal) {
        unselectColumnCheckbox(checkedColumn);
    }
    if (isClear()) {
        enableAll();
        bagType = null;
    }
    var elements = document.getElementsByTagName('td');
    var columnsToHighlightArray = columnsToHighlight[checkedColumn]
    $$('td').each(function(item){
    	var splitter = item.id.split(',');
    	if(splitter[2] == checkedRow || (checkedRow == null && splitter[0].startsWith('cell'))) {
    		columnsToHighlightArray.each(function(item2) {
    		  if(item2 == splitter[1]) {
    		  	if(checkbox.checked) {
    		  		item.addClassName('highlightCell');
    		  		/*var classes = $w(item.className);
    		  		for(var i=0; i<classes.length; i++) {
    		  			var clazz = classes[i];
    		  			if(!isNaN(parseFloat(clazz))) {
    		  				AjaxServices.selectId(clazz,tableid);
    		  			}
    		  		}*/
    		  	} else {
    		  		item.removeClassName('highlightCell');
    		  	}
    		  }
    		})
    	}
    });
}

/**
 * disables all checkboxes in the given column
 **/
function disableColumn(index){
	$$('.selectable').each(function (item) {
		if (item.id.split('_')[1] == index) {
			item.disabled = 'true';
		}
	});
}

/**
 * de-selects a whole column of a given number
 **/
function unselectColumnCheckbox(column) {
	var enabled = true;
    $('selectedObjects_' + column).checked = false;
        $$('.selectable').each(function (item) {
        if ((item.id.split('_')[1] == column) && (item.checked)) {
           enabled = false;
           //setToolbarAvailability(false);
           return;
        }
    });
	setToolbarAvailability(enabled);
}

function setToolbarAvailability(status) {
    $('newBagName').disabled = status;
    $('saveNewBag').disabled = status;
    if($('addToBag')){
            with($('addToBag')) {
                $('addToBag').disabled = status;
            }
    }
}

function onSaveBagEnter(formName) {
	var frm = document.forms[formName];
	frm.operationButton.value = 'saveNewBag';
}