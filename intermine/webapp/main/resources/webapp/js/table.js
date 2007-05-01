
function selectColumnCheckbox(column, bagType) {
    setBagType(bagType);
    selectColumnCheckboxes(column, bagType);
    if (isClear()) {
        enableAll();
    }
}


function enableAll() {
    with(document.saveBagForm) {
        for(i=0;i < elements.length;i++) {
            thiselm = elements[i];
            if(thiselm.id.indexOf('selectedObjects_') != -1) {
                thiselm.disabled = false;
            }
        }
    }
}


function isClear() {
    with(document.saveBagForm) {
        for(i=0;i < elements.length;i++) {
            thiselm = elements[i];
            if(thiselm.id.indexOf('selectedObjects_') != -1 && thiselm.checked) {
                return false;
            }
        }
    }
    return true;
}


function selectColumnCheckboxes(column, bagType) {
    var columnCheckBox = 'selectedObjects_' + column;
    with(document.saveBagForm) {
        for(i=0;i < elements.length;i++) {
            thiselm = elements[i];
            var testString = 'selectedObjects_' + column + '_';
            if(thiselm.id.indexOf(testString) != -1)
                thiselm.checked = document.getElementById(columnCheckBox).checked;
        }
    }
    elements = document.getElementsByTagName('td');
    for (var i = 0; i < elements.length; i++) {
        var id = elements.item(i).id;
        if (id.indexOf('cell') != -1
            && id.indexOf(bagType) != -1){
            if (document.getElementById(columnCheckBox).checked){
                elements.item(i).className = 'highlightCell';
            } else {
                elements.item(i).className = '';
            }
        }
    }
}

/**
 * Ran when a user selects a keyfield in the results table
 **/
function itemChecked(row, column, bagType, checkbox) {
    setBagType(bagType)
        unselectColumnCheckbox('' + column);
    if (isClear()) {
        enableAll();
    }
    var elements = document.getElementsByTagName('td');
    for (var i = 0; i < elements.length; i++) {
        var id = elements.item(i).id;
        if (id.indexOf('cell') != -1
            && id.indexOf(bagType) != -1
            && id.split(',')[2] == row){
            if (checkbox.checked){
                elements.item(i).className = 'highlightCell';
            } else {
                elements.item(i).className = '';
            }
        }
    }
}

/**
 * disables all checkboxes which are not of the specified
 * type
 **/
function setBagType(bagType){
    with(document.saveBagForm) {
        for(i=0;i < elements.length;i++) {
            thiselm = elements[i];
            if(thiselm.id.indexOf('selectedObjects_') != -1 
               && thiselm.value.indexOf(bagType) == -1) {
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
