function handleResults(results) {
    if (results.problem) {
        var notice = document.createElement('p');
        notice.className = "ui-state-error ui-corner-all";
        notice.id = "list-error";
        var icon = document.createElement("span");
        icon.className = "ui-icon ui-icon-alert";
        icon.style["float"] = "left";
        icon.style["margin-right"] = ".3em";
        notice.appendChild(icon);
        var strong = document.createElement("strong");
        strong.innerHTML = "Um, excuse me:";
        notice.appendChild(strong);
        notice.appendChild(document.createTextNode(" " + results.problem));
        jQuery('#content').prepend(notice);
        setTimeout("jQuery('#list-error').fadeOut('slow', function() {jQuery('#list-error').remove()})", 2500);
    } else {
        var notice = document.createElement('p');
        notice.className = "ui-state-highlight ui-corner-all";
        notice.id = "list-results";
        var icon = document.createElement("span");
        icon.className = "ui-icon ui-icon-info";
        icon.style["float"] = "left";
        icon.style["margin-right"] = ".3em";
        notice.appendChild(icon);
        var strong = document.createElement("strong");
        strong.innerHTML = "Completed:";
        notice.appendChild(strong);
        notice.appendChild(document.createTextNode(" " + results.info));
        jQuery('#content').prepend(notice);
        setTimeout("jQuery('#list-results').fadeOut('slow', function() {jQuery('#list-results').remove()})", 2500);
    }
    // Update list details
    if (jQuery('#list-addition-box').length) {
        console.log("Updating list info");
        var thisUrl = window.location.protocol + '//' + window.location.host + ':' + 
                (window.location.port || '80') + window.location.pathname;
        jQuery('#list-addition-box').load(thisUrl + " #list-addition-form");
    }
}
