// the prefix
var im;
if (!im) {
    im = {};
} else if (typeof org != 'object') {
	throw new Error("Intermine JavaScript Library cannot be loaded, 'var im' is already in use");
}
// check for jQuery
if (typeof jQuery == 'undefined') {
	throw new Error("missing jQuery!");
}

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

// on load methods
jQuery(document).ready(function() {
	im.alternatingColors();
});