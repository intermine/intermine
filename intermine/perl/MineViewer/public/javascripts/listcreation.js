function handleResults(results) {
    if (results.problem) {
        jQuery.jGrowl(results.problem);
    } else {
        jQuery.jGrowl(results.info);
    }
    // Update list details
    if (jQuery('#list-info-boxes').length) {
        console.log("Updating list info");
        var thisUrl = window.location.protocol + '//' + window.location.host + ':' + 
                (window.location.port || '80') + window.location.pathname;
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
