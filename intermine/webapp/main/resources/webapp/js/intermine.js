// the prefix
var im;
if (!im) {
    im = {};
    if (window.console && window.console.firebug) {
    	console.log("ƒ InterMine JavaScript Library loaded");
    	im.firebug = true;
    }
} else if (typeof org != 'object') throw new Error("ƒ InterMine JavaScript Library cannot be loaded, 'var im' is already in use");
if (typeof jQuery == 'undefined') throw new Error("ƒ missing jQuery!");

// (on all IE only), will apply .odd/.even classes to "known" tables (see Wiki for those)
im.alternatingColors = function() {
	if (jQuery.browser.msie) {
		jQuery.each(['collection-table'], function(index, imTable) {
			jQuery('div.' + imTable + ' table tbody tr:nth-child(2n)').addClass('odd');
			jQuery('div.' + imTable + '.column-border table tbody tr td:nth-child(n+2)').addClass('left-column-border');
			jQuery('div.' + imTable + '.column-border-by-2 table tbody tr td:nth-child(2n+3)').addClass('left-column-border');
		});
	}
};

// return a path to the current element
im.elementPath = function(e) {
	e = jQuery(e);
	if (e.length != 1) throw new Error("ƒ element does not exist");

	var node = e;
	var path = Array();
	while (node.length) {
		var realNode = node[0], name = realNode.localName;
	    if (!name) break;
	    name = name.toLowerCase();
	    var parent = node.parent();
	    var siblings = parent.children(name);
	    if (siblings.length > 1) {
	    	attrId = jQuery(node).attr('id');
	        attrClass = jQuery(node).attr('class');
	        if (attrId) name += '#' + attrId;
	        if (attrClass) name += '.' + attrClass.replace(/ /g, "."); // using dot notation
	    }
	    path.push(name);
	    node = parent;
	}
	return path.join(' < ');
};

// log message in Firebug if enabled
im.log = function(message) {
	if (im.firebug) {
		console.log('ƒ ' + message);
	}
};

// jQuery extensions
jQuery.fn.extend({
	elementPath: function() {
		return im.elementPath(this);
	},
	log: function(message) {
		im.log(message);
	}
});

// on load methods
jQuery(document).ready(function() {
	im.alternatingColors();
});