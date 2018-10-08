/**
 * Called when a whole column is selected/deselected
 */
function selectAll(columnIndex, columnClass, tableid) {
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
    } else {
        AjaxServices.selectAll(-1, tableid);
        $('selectedIdFields').update('');
    }
    disableOtherColumns(columnIndex);
    setToolbarAvailability(!$('selectedObjects_' + columnIndex).checked);
    if (isClear()) {
        enableAll();
    }
    toggleToolbarButtons();
}

/**
 * Will toggle the status of toolbar buttons for creating/adding to the list based on if we have selected something
 */
function toggleToolbarButtons() {
  if (!isClear()) {
    jQuery("li#tool_bar_li_createlist").removeClass('inactive');
    jQuery("li#tool_bar_li_addtolist").removeClass('inactive');
  } else {
    jQuery("li#tool_bar_li_createlist").addClass('inactive');
    jQuery("li#tool_bar_li_addtolist").addClass('inactive');
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
    var selectedIdFields = $('selectedIdFields');
    return (selectedIdFields.innerHTML.strip() == '');
}


/**
 * Disable columns with a different class
 */
function disableOtherColumns(index) {
    $$('input.selectable').each(function(input){
            if (input.id != 'selectedObjects_'  + index) {
                if (! input.hasClassName('index_' + index)) {
                    input.disabled = true;
                }
            }
        });
}

/**
 * Run when a user selects a keyfield in the results table.  internal is true
 * when called from other methods in this file (ie. not from an onclick in table.jsp)
 **/
function itemChecked(checkedRow, checkedColumn, tableid, checkbox, internal) {
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

    // Update list and save selected state
    if(checkbox.checked) {
        AjaxServices.selectId(objectId,tableid,checkedColumn, {
              callback: function(selectedIds) {
                    $('selectedIdFields').update(selectedIds.join(', '));
                },
              async:false
            }
         );
    } else {
        AjaxServices.deSelectId(objectId,tableid, {
              callback: function(selectedIds) {
                $('selectedIdFields').update(selectedIds.join(', '));
              },
             async:false
         });
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
        bagType = null;
    } else {
        disableOtherColumns(checkedColumn);
    }

    var nothingSelected = $('selectedIdFields').innerHTML.strip() == '';

    setToolbarAvailability(nothingSelected);
    if (nothingSelected) {
        unselectColumnCheckbox(checkedColumn);
    }
    toggleToolbarButtons();
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
  if ($('newBagName')) {
    $('newBagName').disabled = status;
  }
  if ($('saveNewBag')) {
      $('saveNewBag').disabled = status;
  }
    if($('addToBag')){
            with($('addToBag')) {
                $('addToBag').disabled = status;
            }
    }
    if($('removeFromBag')){
            with($('removeFromBag')) {
                $('removeFromBag').disabled = status;
            }
    }
}

function onSaveBagEnter(formName) {
  var frm = document.forms[formName];
  frm.operationButton.value = 'saveNewBag';
}
