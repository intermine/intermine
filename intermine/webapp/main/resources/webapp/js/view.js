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

// change from ascending to descending sort, or vice versa
function reverseSortDirection() {
    var img = document.getElementById('sortImg').src;
    var newDirection;
    if (img.match('desc.gif')) {
      newDirection = 'asc';
    } else {
      newDirection = 'desc';
    }
     new Ajax.Request('<html:rewrite action="/sortOrderChange"/>', {
       parameters:'method=changeDirection&direction='+newDirection,
       asynchronous:true
     });
     document.getElementById('sortImg').src = 'images/' + newDirection + '.gif';
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
        AjaxServices.clearSortOrder(function() {
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

   // enable all imgs, disable the one the user just selected
function updateSortImgs(index) {
       for (i=0;true;i++) {
           if (!document.getElementById("btn_" + i)) return;
           var b = document.getElementById("btn_" + i);
           if(i==index) {
               b.src = "images/sort_down.gif";
               b.disabled = true;
           } else {
               b.src = "images/sort.png";
               b.disabled = false;
           }
       }
}