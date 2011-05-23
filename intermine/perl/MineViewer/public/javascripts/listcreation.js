function handleResults(results) {
    if (results.problem) {
        jQuery.jGrowl(results.problem);
    } else {
        jQuery.jGrowl(results.info);
    }
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
    
    if (jQuery('#list-container').length) {
        jQuery('#list-container').load(thisUrl + ' #lists', null,
            function() {
                var currentList = unescape(window.location.hash.substr(1));
                jQuery('option').each(function(index, elem) {
                    if (elem.value == currentList) {
                        jQuery(elem).attr('selected', true);
                    }
                });
                jQuery('#lists').change(handleListSelection);
        });
    }

    if (jQuery('#list-item-box').length) {
        jQuery('#list-item-box').load(thisUrl + ' #list-items', null, function() {
            jQuery('#list-items').makeacolumnlists({
                cols: colWidth, colWidth: 0, 
                equalHeight: true, startN: 1
            })
        });
    }
    if (jQuery('#list-title-container').length) {
        jQuery('#list-title-container').load(url + ' #list-title');
    }
}
