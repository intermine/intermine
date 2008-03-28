var lastOpen;

function toggleToolBarMenu(e) {
    toggleToolBarMenu(e,null);
}

function toggleToolBarMenu(e,pos) {
	e = e || window.event;
	var tgt = e.target || e.srcElement;
	var id = tgt.id;
	if(id == null || id == '') {
	    return;
	}
	if (lastOpen) {
	    hideMenu(lastOpen,pos);
	}
	var button = document.getElementById(id);
	var item = document.getElementById(id.replace(/button/,'item'));
	if (item.style.visibility == 'visible') {
		hideMenu(item.id,pos);
		return;
	}
	if(pos!='widget') {
    	button.className=button.id;
	}
	var posArray = findPosition(button);
	item.style.left = posArray[0] +"px";
	item.style.top = posArray[1] + 25 +"px";
	item.style.visibility = 'visible';
	lastOpen = item.id;
}
function hideMenu(id) {
    hideMenu(id,null);
}
function hideMenu(id,pos) {
	document.getElementById(id).style.visibility = 'hidden';
	if(pos!='widget') {
	    document.getElementById(id.replace(/item/,'button')).className = null;
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