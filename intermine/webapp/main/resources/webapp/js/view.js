var previousOrder = '';

jQuery(document).ready(function(){
	jQuery('#viewDivs').sortable({dropOnEmpty:true,update:function() {
    	    reorderOnServer();
        }
	});
    recordCurrentOrder();
});
  

function recordCurrentOrder() {
    previousOrder = jQuery('#viewDivs').sortable('serialize');
}

/**
* Send the previous order and the new order to the server.
*/
function reorderOnServer() {
	var newOrder = jQuery('#viewDivs').sortable('serialize');
    AjaxServices.reorder(newOrder, previousOrder);
    recordCurrentOrder();
}


// called from viewElement.jsp
function updateSortOrder(pathString) {
	 if(jQuery('#btn_' + pathString.replace(/[\.:]/g,'_')).attr('src').match('none')) {
        AjaxServices.addToSortOrder(pathString, 'asc', function() {
        	AjaxServices.getSortOrderMap(function(sortMap) {
        		reDrawSorters(sortMap);
        	})
        });
	 } else if(jQuery('#btn_' + pathString.replace(/[\.:]/g,'_')).attr('src').match('asc')) {
        AjaxServices.addToSortOrder(pathString, 'desc', function() {
        	AjaxServices.getSortOrderMap(function(sortMap) {
        		reDrawSorters(sortMap);
        	})
        });
	 } else if(jQuery('#btn_' + pathString.replace(/[\.:]/g,'_')).attr('src').match('desc')) {
	    AjaxServices.addToSortOrder(pathString, 'asc', function() {
	    	AjaxServices.getSortOrderMap(function(sortMap) {
	    		reDrawSorters(sortMap);
	    	})
	    });	
	 } else {
	 	return;
	 }
}

function reDrawSorters(sortMap) {
	for(name in sortMap) {
 	   jQuery('#btn_' + name.replace(/[\.:]/g,'_')).attr('src','images/sort_'+sortMap[name]+'.png');
	}
}
