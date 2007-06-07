var duplicateArray = new Array();
var tdColorArray = new Array();


function addId2Bag(id,row,parentId,issueType){
    if(document.getElementById('add_'+issueType+'_'+id).className=='fakelink'){
        document.getElementById('add_'+issueType+'_'+id).className = '';
        document.getElementById('rem_'+issueType+'_'+id).className = "fakelink";
        var elements = document.getElementsByTagName('td');
        for (var i = 0; i < elements.length; i++) {
            var idtd = elements.item(i).id;
            if(idtd == 'row_'+issueType+'_' + row) {
                tdColorArray['row_'+issueType+'_'+row] = elements.item(i).style.backgroundColor;
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
            document.getElementById(issueType+'Count').innerHTML--;
            document.getElementById('td_'+issueType+'_'+parentId).style.backgroundColor = '#CCCCCC';
            document.getElementById('initialIdCount').innerHTML++;
        } else {
            idArray[idArray.length] = id;
            duplicateArray[parentId] = idArray;
        }
        toggleForm(1);
    }    
}

function removeIdFromBag(id,row, parentId, issueType){
    if(document.getElementById('rem_'+issueType+'_'+id).className=='fakelink'){
        document.getElementById('rem_'+issueType+'_'+id).className = '';
        document.getElementById('add_'+issueType+'_'+id).className = "fakelink";
        var elements = document.getElementsByTagName('td');
        for (var i = 0; i < elements.length; i++) {
            var idtd = elements.item(i).id;
            if(idtd == 'row_'+issueType+'_' + row) {
                elements.item(i).style.backgroundColor = tdColorArray['row_'+issueType+'_'+row];
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
            document.getElementById(issueType+'Count').innerHTML++;
            document.getElementById('td_'+issueType+'_'+parentId).style.backgroundColor = '#FFFFFF';
            document.getElementById('initialIdCount').innerHTML--;
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
        toggleForm(document.getElementById('matchCount').innerHTML);        
    }
}

function addAll(flatArray){
	// split string into rows
	// a,b,c,d|e,f,g,h
	var a = flatArray.split("|");
	
	for (i=0;i<a.length-1;i++) {
		// split rows into vars
		var b = a[i].split(",");
		addId2Bag(b[0],b[1],b[2],b[3]);
	}
}

function removeAll(flatArray){
	// split string into rows
	// a,b,c,d|e,f,g,h
	var a = flatArray.split("|");
	
	for (i=0;i<a.length-1;i++) {
		// split rows into vars
		var b = a[i].split(",");
		removeIdFromBag(b[0],b[1],b[2],b[3]);
	}
}