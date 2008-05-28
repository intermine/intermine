/**
 * Called when a whole column is selected/deselected
 */
function selectAll(columnsToDisable, columnsToHighlight, columnIndex, tableid) {
    $$('.selectable').each(function(item) {
       if(item.id.split("_")[1] == columnIndex) {
           item.checked = $('selectedObjects_' + columnIndex).checked;
           $w(item.className).each(function(className){
              if(className.startsWith('id_')) {
                 objectId = className;
                // Hightlight all cells for this object
                $$("td."+objectId).each(function(cell){
                    if(item.checked) {
                        cell.addClassName('highlightCell');
                    } else {
                        cell.removeClassName('highlightCell');
                    }
                });
              }
           });
       }
    });
    if($('selectedObjects_' + columnIndex).checked) {
        AjaxServices.selectAll(columnIndex, tableid);
        $('selectedIdFields').update(' All selected on all pages');
        $('selectedIds').value = 'all_' + columnIndex;
    } else {
        AjaxServices.selectAll(-1, tableid);
        $('selectedIdFields').update('');
        $('selectedIds').value = '';
    }
    setToolbarAvailability(!$('selectedObjects_' + columnIndex).checked);    if (isClear()) {
        enableAll();
    }
}

/**
 * Enables all checkboxes
 */
function enableAll() {
    $$('input.selectable').each(function(item) {
    	item.disabled = false;
    });
}


/**
 * Checks that nothing is selected
 */
function isClear() {
	return ($('selectedIds').value.strip() == '');
}


/**
 * Disable columns with a different class
 * TODO there seems to be a bug when adding classes with []
 */
function disableOtherColumns(className) {
	$$('input.selectable').each(function(input){
		if(! input.hasClassName(className)) {
            input.disabled = true;
        }
	});
}

/**
 * Run when a user selects a keyfield in the results table.  internal is true
 * when called from other methods in this file (ie. not from an onclick in table.jsp)
 **/
function itemChecked(columnsToDisable, columnsToHighlight, checkedRow, checkedColumn, tableid, checkbox, internal) {
    /*if (bagType == null) {
        var columnsToDisableArray = columnsToDisable[checkedColumn];
        if (columnsToDisableArray != null) {
        	columnsToDisableArray.each(function(item) {
        	  disableColumn(item);
        	});
        }
        bagType = checkedColumn;
    }*/
    
    /*if (!internal) {
        unselectColumnCheckbox(checkedColumn);
    }*/
    
    var objectId;
    var objectClass;
    $w(checkbox.className).each(function(className){
      	if(className.startsWith('id_')) {
      		objectId = className.sub('id_','');
      	}
        if(className.startsWith('class_')) {
            objectClass = className;
        }
    })

    // Update list at the top and save selected state
    if(checkbox.checked) {
        AjaxServices.selectId(objectId,tableid,
            function(selectedIds) { 
            	$('selectedIdFields').update(selectedIds); 
        });
        if($('selectedIds').value.strip() != '') {
          var splitted = $('selectedIds').value.split(',');
          splitted.push(objectId);
        } else {
          var splitted=objectId;
        }
        $('selectedIds').value=splitted;
    } else {
        AjaxServices.deSelectId(objectId,tableid, function(selectedIds) { 
                $('selectedIdFields').update(selectedIds); 
        });
        var splitted = $('selectedIds').value.split(',');
        var count=0;
        splitted.each(function(item){
           if(item.strip() == objectId) {
           	 index = count;
           }
           count++;
        })
        var removed = splitted.splice(index,1);
        $('selectedIds').value=splitted;
    }
    
    // Hightlight all cells for this object
    $$("td.id_"+objectId).each(function(cell){
        if(checkbox.checked) {
            cell.addClassName('highlightCell');
        } else {
            cell.removeClassName('highlightCell');
        }
    });
    
    $$("input.id_"+objectId).each(function(box){
        box.checked = checkbox.checked;
    });
    
        // Disable/enable other classes columns
    if (isClear()) {
        enableAll();
        AjaxServices.setClassForId('', tableid);
        bagType = null;
    } else {
        disableOtherColumns(objectClass);
        AjaxServices.setClassForId(objectClass.sub('class_',''), tableid);
    }
    
    setToolbarAvailability($('selectedIds').value.strip() == '');
    
    /*var columnsToHighlightArray = columnsToHighlight[checkedColumn]
    $$('td').each(function(item){
    	var splitter = item.id.split(',');
    	if(splitter[2] == checkedRow || (checkedRow == null && splitter[0].startsWith('cell'))) {
    		columnsToHighlightArray.each(function(item2) {
    		  if(item2 == splitter[1]) {
    		  	if(checkbox.checked) {
    		  		item.addClassName('highlightCell');
    		  		var classes = $w(item.className);
    		  		for(var i=0; i<classes.length; i++) {
    		  			var clazz = classes[i];
    		  			if(!isNaN(parseFloat(clazz))) {
    		  				AjaxServices.selectId(clazz,tableid);
    		  			}
    		  		}
    		  	} else {
    		  		item.removeClassName('highlightCell');
    		  	}
    		  }
    		})
    	}
    });*/
}

/**
 * disables all checkboxes in the given column
 **/
/*function disableColumn(index){
	$$('.selectable').each(function (item) {
		if (item.id.split('_')[1] == index) {
			item.disabled = 'true';
		}
	});
}*/

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