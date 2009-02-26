var lastOpen;

function toggleToolBarMenu(button) {
    toggleToolBarMenu(button,null);
}

function toggleToolBarMenu(button,pos) {
	if(button == null) {
	    return;
	}
	if (lastOpen) {
	    hideMenu(lastOpen,pos);
	}
	var item_name = "#"+jQuery(button).attr('id').replace(/li/,'item');
	if (jQuery(item_name).is(":visible")) {
		hideMenu(item_name,pos);
		return;
	}
	if(pos!='widget') {
        jQuery(button).addClass('tb_button_active');
	}
	var posArray = findPosition(button);
    jQuery(item_name).css('left', posArray[0] +"px");
    jQuery(item_name).css('top', posArray[1] + 25 +"px");
	jQuery(item_name).show();
	lastOpen = jQuery(item_name).attr('id');
}
function hideMenu(id) {
    hideMenu(id,null);
}
function hideMenu(id,pos) {
	jQuery("#"+id).hide();
	if(pos!='widget') {
        jQuery("#"+id.replace(/item/,'li')).removeClass('tb_button_active');
    }
}
function findPosition(obj) {
	var curleft = curtop = 0;
	if (obj.offsetParent) {
		curleft = obj.offsetLeft;
		curtop = obj.offsetTop;
		while (obj = obj.offsetParent) {
			curleft += obj.offsetLeft;
			curtop += obj.offsetTop;
		}
	}
	return [curleft,curtop];
}