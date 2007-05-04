
function selectColumnCheckbox(columnsToDisable, columnsToHighlight, columnIndex) {
    selectColumnCheckboxes(columnsToDisable, columnsToHighlight, columnIndex);
    if (isClear()) {
        enableAll();
    }
}


function enableAll() {
    with(document.saveBagForm) {
        for(var i=0;i < elements.length;i++) {
            thiselm = elements[i];
            if(thiselm.id.indexOf('selectedObjects_') != -1) {
                thiselm.disabled = false;
            }
        }
    }
}


function isClear() {
    with(document.saveBagForm) {
        for(var i=0;i < elements.length;i++) {
            thiselm = elements[i];
            if(thiselm.id.indexOf('selectedObjects_') != -1 && thiselm.checked) {
                return false;
            }
        }
    }
    return true;
}


function selectColumnCheckboxes(columnsToDisable, columnsToHighlight, columnIndex) {
    var columnCheckBox = 'selectedObjects_' + columnIndex;
    with(document.saveBagForm) {
        for(var i=0;i < elements.length;i++) {
            thiselm = elements[i];
            var testString = 'selectedObjects_' + columnIndex + '_';
            if(thiselm.id.indexOf(testString) != -1) {
                thiselm.checked = document.getElementById(columnCheckBox).checked;
                var bits = thiselm.value.split(',');
                var checkedRow = bits[1];
                itemChecked(columnsToDisable, columnsToHighlight, checkedRow,
                            columnIndex, thiselm, true);
            }
        }
    }
}

/**
 * Ran when a user selects a keyfield in the results table.  internal is true
 * when called from other methods in this file (ie. not from an onclick in table.jsp)
 **/
function itemChecked(columnsToDisable, columnsToHighlight, checkedRow, checkedColumn, checkbox, internal) {
    if (bagType == null) {
        var columnsToDisableArray = columnsToDisable[checkedColumn];
        if (columnsToDisableArray != null) {
            for (var columnIndex = 0; columnIndex < columnsToDisableArray.length; columnIndex++) {
                disableColumn(columnsToDisableArray[columnIndex]);
            }
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
    for (var elementsIndex = 0; elementsIndex < elements.length; elementsIndex++) {
        var id = elements.item(elementsIndex).id;
        if (id.indexOf('cell') != -1
            && id.split(',')[2] == checkedRow) {
            for (var columnArrayIndex = 0; 
                 columnArrayIndex < columnsToHighlightArray.length; 
                 columnArrayIndex++) {
                if (columnsToHighlightArray[columnArrayIndex] == id.split(',')[1]) {
                    if (checkbox.checked){
                        elements.item(elementsIndex).className = 'highlightCell';
                    } else {
                        elements.item(elementsIndex).className = '';
                    }
                }
            }
        }
    }
}

/**
 * disables all checkboxes in the given column
 **/
function disableColumn(index){
    with(document.saveBagForm) {
        for(var i=0;i < elements.length;i++) {
            thiselm = elements[i];
            if(thiselm.id.indexOf('selectedObjects_') != -1 
               && thiselm.value.split(',')[0] == index) {
                thiselm.disabled = 'true';
            }
        }
    }
}

/**
 * de-selects a whole column of a given number
 **/
function unselectColumnCheckbox(column) {
    document.getElementById('selectedObjects_' + column).checked = false;
}
