// jQuery reqs
if (typeof jQuery == 'undefined') {
	throw new Error("ƒ missing jQuery!");
} else {
	if (jQuery().jquery < '1.4.1') {
		throw new Error("ƒ jQuery >= 1.4.1 required");
	}
}
// the prefix
var im;
if (!im) {
    im = {};
    if (window.console && window.console.firebug) { // 'attach' Firebug
    	console.log("ƒ InterMine JavaScript Library loaded");
    	im.firebug = true;
    	jQuery.error = console.error;
    }
} else if (typeof org != 'object') throw new Error("ƒ InterMine JavaScript Library cannot be loaded, 'var im' is already in use");

//check if element exists
im.exists = function(e) {
	return (jQuery(e).length > 0);
};

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
	switch (e.length) {
		case 0:
			jQuery.error("ƒ element does not exist");
			im.trace(1);
			break;
		case 1:
			break;
		default:
			return e.each().elementPath(); // call us for each matched element
	}

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
	return im.log(path.join(' < '));
};

// log message in Firebug if enabled
im.log = function(message) {
	if (im.firebug) {
		console.log('ƒ ' + message);
	}
};

// return a stack trace (Firefox only)
im.trace = function(level) {
	if (jQuery.browser.mozilla) {
		try {
			kaboodakabooda++; // booda?
		} catch (e) {
			level = (level === undefined) ? 0 : level;
		    var stack = ['trace:'];
		    jQuery.each(e.stack.split("\n"), function(index, value) {
		    	if (index > level) {
		    		stack.push(value.substring(value.indexOf('@')));
		    	}
		    });
		    return im.log(stack.join('\n'));
		}
	}
};

// jQuery extensions
jQuery.fn.extend({
	exists: function() {
		return im.exists(this);
	},
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