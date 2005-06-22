// S5 slides.js -- released under CC by-sa 2.0 license
//
// Please see http://www.meyerweb.com/eric/tools/s5/credits.html for information 
// about all the wonderful and talented contributors to this code!

var snum = 0;
var smax = 1;
var undef;
var slcss = 1;
var isIE = navigator.appName == 'Microsoft Internet Explorer' ? 1 : 0;
var isOp = navigator.userAgent.indexOf('Opera') > -1 ? 1 : 0;
var isGe = navigator.userAgent.indexOf('Gecko') > -1 && navigator.userAgent.indexOf('Safari') < 1 ? 1 : 0;
var slideCSS = document.getElementById('slideProj').href;

function isClass(object, className) {
	return (object.className.search('(^|\\s)' + className + '(\\s|$)') != -1);
}

function GetElementsWithClassName(elementName,className) {
	var allElements = document.getElementsByTagName(elementName);
	var elemColl = new Array();
	for (i = 0; i< allElements.length; i++) {
		if (isClass(allElements[i], className)) {
			elemColl[elemColl.length] = allElements[i];
		}
	}
	return elemColl;
}

function isParentOrSelf(element, id) {
	if (element == null || element.nodeName=='BODY') return false;
	else if (element.id == id) return true;
	else return isParentOrSelf(element.parentNode, id);
}

function nodeValue(node) {
	var result = "";
	if (node.nodeType == 1) {
		var children = node.childNodes;
		for ( i = 0; i < children.length; ++i ) {
			result += nodeValue(children[i]);
		}		
	}
	else if (node.nodeType == 3) {
		result = node.nodeValue;
	}
	return(result);
}

function slideLabel() {
	var slideColl = GetElementsWithClassName('div','slide');
	var list = document.getElementById('jumplist');
	smax = slideColl.length;
	for (n = 0; n < smax; n++) {
		var obj = slideColl[n];

		var did = 'slide' + n.toString();
		obj.setAttribute('id',did);
		if(isOp) continue;

		var otext = '';
 		var menu = obj.firstChild;
		if (!menu) continue; // to cope with empty slides
		while (menu && menu.nodeType == 3) {
			menu = menu.nextSibling;
		}
	 	if (!menu) continue; // to cope with slides with only text nodes

		var menunodes = menu.childNodes;
		for (o = 0; o < menunodes.length; o++) {
			otext += nodeValue(menunodes[o]);
		}
		list.options[list.length] = new Option(n+' : ' +otext,n);
	}
}

function currentSlide() {
	var cs;
	if (document.getElementById) {
		cs = document.getElementById('currentSlide');
	} else {
		cs = document.currentSlide;
	}
	cs.innerHTML = '<span id="csHere">' + snum + '<\/span> ' + 
		'<span id="csSep">\/<\/span> ' + 
		'<span id="csTotal">' + (smax-1) + '<\/span>';
	if (snum == 0) {
		cs.style.visibility = 'hidden';
	} else {
		cs.style.visibility = 'visible';
	}
}

function go(inc) {
	if (document.getElementById("slideProj").disabled) return;
	var cid = 'slide' + snum;
	if (inc != 'j') {
		snum += inc;
		lmax = smax - 1;
		if (snum > lmax) snum = 0;
		if (snum < 0) snum = lmax;
	} else {
		snum = parseInt(document.getElementById('jumplist').value);
	}
	var nid = 'slide' + snum;
	var ne = document.getElementById(nid);
	if (!ne) {
		ne = document.getElementById('slide0');
		snum = 0;
	}
	document.getElementById(cid).style.visibility = 'hidden';
	ne.style.visibility = 'visible';
	document.getElementById('jumplist').selectedIndex = snum;
	currentSlide();
}

function toggle() {
    var slideColl = GetElementsWithClassName('div','slide');
    var obj = document.getElementById('slideProj');
    if (!obj.disabled) {
        obj.disabled = true;
        for (n = 0; n < smax; n++) {
            var slide = slideColl[n];
            slide.style.visibility = 'visible';
        }
    } else {
        obj.disabled = false;
        for (n = 0; n < smax; n++) {
            var slide = slideColl[n];
            slide.style.visibility = 'hidden';
        }
        slideColl[snum].style.visibility = 'visible';
    }
}

function showHide(action) {
	var obj = document.getElementById('jumplist');
	switch (action) {
	case 's': obj.style.visibility = 'visible'; break;
	case 'h': obj.style.visibility = 'hidden'; break;
	case 'k':
		if (obj.style.visibility != 'visible') {
			obj.style.visibility = 'visible';
		} else {
			obj.style.visibility = 'hidden';
		}
	break;
	}
}

// 'keys' code adapted from MozPoint (http://mozpoint.mozdev.org/)
function keys(key) {
	if (!key) {
		key = event;
		key.which = key.keyCode;
	}
 	switch (key.which) {
		case 10: // return
		case 13: // enter
			if (window.event && isParentOrSelf(window.event.srcElement, "controls")) return;
			if (key.target && isParentOrSelf(key.target, "controls")) return;
		case 32: // spacebar
		case 34: // page down
		case 39: // rightkey
		case 40: // downkey
			go(1);
			break;
		case 33: // page up
		case 37: // leftkey
		case 38: // upkey
			go(-1);
			break;
		case 84: // t
			toggle();
			break;
		case 67: // c
			showHide('k');
			break;
	}
}

function clicker(e) {
	var target;
	if (window.event) {
		target = window.event.srcElement;
		e = window.event;
	} else target = e.target;
 	if (target.href != null || isParentOrSelf(target, 'controls')) return true;
	if (!e.which || e.which == 1) go(1);
}

function slideJump() {
	if (window.location.hash == null) return;
	var sregex = /^#slide(\d+)$/;
	var matches = sregex.exec(window.location.hash);
	var dest = null;
	if (matches != null) {
		dest = parseInt(matches[1]);
	} else {
		var target = window.location.hash.slice(1);
		var targetElement = null;
		var aelements = document.getElementsByTagName("a");
		for (i = 0; i < aelements.length; i++) {
			var aelement = aelements[i];
			if ( (aelement.name && aelement.name == target)
			 || (aelement.id && aelement.id == target) ) {
				targetElement = aelement;
				break;
			}
		}
		while(targetElement != null && targetElement.nodeName != "body") {
			if (targetElement.className == "slide") break;
			targetElement = targetElement.parentNode;
		}
		if (targetElement != null && targetElement.className == "slide") {
			dest = parseInt(targetElement.id.slice(1));
		}
	}
	if (dest != null)
		go(dest - snum);
 }
 
function createControls() {
	controlsDiv = document.getElementById("controls");
	if (!controlsDiv) return;
	controlsDiv.innerHTML = '<form action="#" id="controlForm">' +
	'<div>' +
	'<a accesskey="t" id="toggle" href="javascript:toggle();">&#216;<\/a>' +
	'<a accesskey="z" id="prev" href="javascript:go(-1);">&laquo;<\/a>' +
	'<a accesskey="x" id="next" href="javascript:go(1);">&raquo;<\/a>' +
	'<\/div>' +
	'<div onmouseover="showHide(\'s\');" onmouseout="showHide(\'h\');"><select id="jumplist" onchange="go(\'j\');"><\/select><\/div>' +
	'<\/form>';
}

function notOperaFix() {
	var obj = document.getElementById('slideProj');
	obj.setAttribute('media','screen');
	if (isGe) {
		obj.setAttribute('href','null');   // Gecko fix
		obj.setAttribute('href',slideCSS); // Gecko fix
	}
}

function startup() {
	if (!isOp) createControls();
	slideLabel();
	if (!isOp) {		
		notOperaFix();
		slideJump();
		document.onkeyup = keys;
		document.onclick = clicker;
	}
}

window.onload = startup;
