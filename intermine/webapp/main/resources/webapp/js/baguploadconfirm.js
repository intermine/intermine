var duplicateArray = new Array();
var tdColorArray = new Array();

function addId2Bag(id,row,parentId){
    document.getElementById('add_'+id).className = '';
    document.getElementById('rem_'+id).className = "fakelink";
    var elements = document.getElementsByTagName('td');
    for (var i = 0; i < elements.length; i++) {
        var idtd = elements.item(i).id;
        if(idtd == 'row_' + row) {
            tdColorArray['row_'+row] = elements.item(i).style.backgroundColor;
            elements.item(i).style.backgroundColor = "#CCCCCC";
        }
    }
    var bagList = document.getElementById('matchIDs').value;
    if(bagList.indexOf(id) == -1){
        if (bagList.length != 0 ) {
            bagList += " ";
        }
        document.getElementById('matchIDs').value = bagList + id;
    }
    document.getElementById('matchCount').innerHTML++;
    var idArray = duplicateArray[parentId];
    if(idArray == null) {
        duplicateArray[parentId] = new Array(id);
        document.getElementById('duplicateCount').innerHTML--;
        document.getElementById("td_"+parentId).style.backgroundColor = '#CCCCCC';
    } else {
        idArray[idArray.length] = id;
        duplicateArray[parentId] = idArray;
    }
}

function removeIdFromBag(id,row, parentId){
    document.getElementById('rem_'+id).className = '';
    document.getElementById('add_'+id).className = "fakelink";
    var elements = document.getElementsByTagName('td');
    for (var i = 0; i < elements.length; i++) {
        var idtd = elements.item(i).id;
        if(idtd == 'row_' + row) {
            elements.item(i).style.backgroundColor = tdColorArray['row_'+row];
        }
    }
    var bagList = document.getElementById('matchIDs').value;
    if(bagList.indexOf(id) != -1){
        bagList = bagList.split(id).join('').split('  ').join(' ');
        document.getElementById('matchIDs').value = bagList.replace(/^s+/, '').replace(/s+$/, '');//Trim
    }
    var bagList = document.getElementById('matchIDs').value;
    document.getElementById('matchCount').innerHTML--;
    var idArray = duplicateArray[parentId];
    if(idArray.length == 1){
        document.getElementById('duplicateCount').innerHTML++;
        document.getElementById("td_"+parentId).style.backgroundColor = '#FFFFFF';
        duplicateArray[parentId] = null;
    } else {
        var idArrayCopy = new Array();
        for (var i=0;i<idArray.length;i++){
            if(id!=idArray[i]){
                idArrayCopy[idArrayCopy.length]=idArray[i];
            }
        }
        duplicateArray[parentId] = idArrayCopy;
    }
}
