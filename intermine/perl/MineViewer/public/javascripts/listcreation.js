function notifyResults(results) {
    if (results.problem) {
        jQuery.jGrowl("ERROR: " + results.problem);
    } else {
        jQuery.jGrowl("COMPLETED: " + results.info);
    }
}

function updateExportBox(url, currentList) {
    url += 'lists.export';
    $('#export').load(url + ' #export-menu', {list: currentList});
} 

function removeitem(listName, objId) {
    var url = $BASE_URL + 'remove_list_item';
    var data = { list: listName, ids: objId };
    jQuery.post(url, data, handleListResults, "json");
}

function handleListResults(results) {
    notifyResults(results);
    // Update list details
    var thisUrl = window.location.protocol + '//' + window.location.host + ':' + 
            (window.location.port || '80') + window.location.pathname + window.location.hash;
    if (jQuery('#list-info-boxes').length) {
        console.log("Updating list info");
        jQuery('#contained-in-box').load(thisUrl + " #contained-in");
        jQuery('#list-addition-box').load(thisUrl + " #list-addition-form",
            null, function() {
                if (jQuery('#contained-in li').length > 0) {
                    jQuery('#list-collapser').show();
                } else {
                    jQuery('#list-collapser').hide();
                }
            });
    } 
}

function updateListItemDisplayArea(url, selectedList) {
    jQuery.get(url, {list: selectedList}, function(res) {
        jQuery('#list-item-box')
            .html(res);
        jQuery('#list-items')
            .makeacolumnlists({
            cols: colWidth, colWidth: 0, 
            equalHeight: true, startN: 1
        })
    }, "html");
}

function updateListDisplay(url, selectedList) {
    updateListSelector(url + '.options', selectedList);
    updateListItemDisplayArea(url + '.items', selectedList);
}

function updateListSelector(url, selectedList) {
    $.get(url, null, function(results) {
        $('#lists').html(results);
        $('#lists').multiselect('uncheckall');
        if (selectedList) {
            $("#lists").multiselect("widget").find(":radio").each(function(){
                if (this.value == selectedList) {
                    this.click();
                }
            });
        }
        $('#lists').multiselect('refresh');
    }, 'html');
}
