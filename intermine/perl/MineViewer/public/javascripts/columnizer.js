/** 
#  * Copyright (c) 2008 Pasyuk Sergey (www.codeasily.com) 
#  * Licensed under the MIT License: 
#  * http://www.opensource.org/licenses/mit-license.php 
#  *  
#  * Splits a <ul>/<ol>-list into equal-sized columns. 
#  *  
#  * Requirements:  
#  * <ul> 
#  * <li>"ul" or "ol" element must be styled with margin</li> 
#  * </ul> 
#  *  
#  * @see http://www.codeasily.com/jquery/multi-column-list-with-jquery 
#  */  
jQuery.fn.makeacolumnlists = function(settings){
	settings = jQuery.extend({
		cols: 2,				// set number of columns
		colWidth: 0,			// set width for each column or leave 0 for auto width
		equalHeight: false, 	// can be false, 'ul', 'ol', 'li'
		startN: 1				// first number on your ordered list
	}, settings);

	if(jQuery('> li', this)) {
		this.each(function(y) {
			var y=jQuery('.li_container').size(),
		    	height = 0, 
		        maxHeight = 0,
				t = jQuery(this),
				classN = t.attr('class'),
				listsize = jQuery('> li', this).size(),
				percol = Math.ceil(listsize/settings.cols),
				contW = t.width(),
				bl = ( isNaN(parseInt(t.css('borderLeftWidth'),10)) ? 0 : parseInt(t.css('borderLeftWidth'),10) ),
				br = ( isNaN(parseInt(t.css('borderRightWidth'),10)) ? 0 : parseInt(t.css('borderRightWidth'),10) ),
				pl = parseInt(t.css('paddingLeft'),10),
				pr = parseInt(t.css('paddingRight'),10),
				ml = parseInt(t.css('marginLeft'),10),
				mr = parseInt(t.css('marginRight'),10),
				col_Width = Math.floor((contW - (settings.cols-1)*(bl+br+pl+pr+ml+mr))/settings.cols);
			if (settings.colWidth) {
				col_Width = settings.colWidth; 
			}
			var colnum=1,
				percol2=percol;
			jQuery(this).addClass('li_cont1').wrap('<div id="li_container' + (++y) + '" class="li_container"></div>');
			for (var i=0; i<=listsize; i++) {
				if(i>=percol2) { percol2+=percol; colnum++; }
				var eq = jQuery('> li:eq('+i+')',this);
				eq.addClass('li_col'+ colnum);
				if(jQuery(this).is('ol')){eq.attr('value', ''+(i+settings.startN))+'';}
			}
			jQuery(this).css({cssFloat:'left', width:''+col_Width+'px'});
			for (colnum=2; colnum<=settings.cols; colnum++) {
				if(jQuery(this).is('ol')) {
					jQuery('li.li_col'+ colnum, this).appendTo('#li_container' + y).wrapAll('<ol class="li_cont'+colnum +' ' + classN + '" style="float:left; width: '+col_Width+'px;"></ol>');
				} else {
					jQuery('li.li_col'+ colnum, this).appendTo('#li_container' + y).wrapAll('<ul class="li_cont'+colnum +' ' + classN + '" style="float:left; width: '+col_Width+'px;"></ul>');
				}
			}
			if (settings.equalHeight=='li') {
				for (colnum=1; colnum<=settings.cols; colnum++) {
				    jQuery('#li_container'+ y +' li').each(function() {
				        var e = jQuery(this);
				        var border_top = ( isNaN(parseInt(e.css('borderTopWidth'),10)) ? 0 : parseInt(e.css('borderTopWidth'),10) );
				        var border_bottom = ( isNaN(parseInt(e.css('borderBottomWidth'),10)) ? 0 : parseInt(e.css('borderBottomWidth'),10) );
				        height = e.height() + parseInt(e.css('paddingTop'), 10) + parseInt(e.css('paddingBottom'), 10) + border_top + border_bottom;
				        maxHeight = (height > maxHeight) ? height : maxHeight;
				    });
				}
				for (colnum=1; colnum<=settings.cols; colnum++) {
					var eh = jQuery('#li_container'+ y +' li');
			        var border_top = ( isNaN(parseInt(eh.css('borderTopWidth'),10)) ? 0 : parseInt(eh.css('borderTopWidth'),10) );
			        var border_bottom = ( isNaN(parseInt(eh.css('borderBottomWidth'),10)) ? 0 : parseInt(eh.css('borderBottomWidth'),10) );
					mh = maxHeight - (parseInt(eh.css('paddingTop'), 10) + parseInt(eh.css('paddingBottom'), 10) + border_top + border_bottom );
			        eh.height(mh);
				}
			} else 
			if (settings.equalHeight=='ul' || settings.equalHeight=='ol') {
				for (colnum=1; colnum<=settings.cols; colnum++) {
				    jQuery('#li_container'+ y +' .li_cont'+colnum).each(function() {
				        var e = jQuery(this);
				        var border_top = ( isNaN(parseInt(e.css('borderTopWidth'),10)) ? 0 : parseInt(e.css('borderTopWidth'),10) );
				        var border_bottom = ( isNaN(parseInt(e.css('borderBottomWidth'),10)) ? 0 : parseInt(e.css('borderBottomWidth'),10) );
				        height = e.height() + parseInt(e.css('paddingTop'), 10) + parseInt(e.css('paddingBottom'), 10) + border_top + border_bottom;
				        maxHeight = (height > maxHeight) ? height : maxHeight;
				    });
				}
				for (colnum=1; colnum<=settings.cols; colnum++) {
					var eh = jQuery('#li_container'+ y +' .li_cont'+colnum);
			        var border_top = ( isNaN(parseInt(eh.css('borderTopWidth'),10)) ? 0 : parseInt(eh.css('borderTopWidth'),10) );
			        var border_bottom = ( isNaN(parseInt(eh.css('borderBottomWidth'),10)) ? 0 : parseInt(eh.css('borderBottomWidth'),10) );
					mh = maxHeight - (parseInt(eh.css('paddingTop'), 10) + parseInt(eh.css('paddingBottom'), 10) + border_top + border_bottom );
			        eh.height(mh);
				}
			}
		    jQuery('#li_container' + y).append('<div style="clear:both; overflow:hidden; height:0px;"></div>');
		});
	}
}

jQuery.fn.uncolumnlists = function(){
	jQuery('.li_cont1').each(function(i) {
		var onecolSize = jQuery('#li_container' + (++i) + ' .li_cont1 > li').size();
		if(jQuery('#li_container' + i + ' .li_cont1').is('ul')) {
			jQuery('#li_container' + i + ' > ul > li').appendTo('#li_container' + i + ' ul:first');
			for (var j=1; j<=onecolSize; j++) {
				jQuery('#li_container' + i + ' ul:first li').removeAttr('class').removeAttr('style');
			}
			jQuery('#li_container' + i + ' ul:first').removeAttr('style').removeClass('li_cont1').insertBefore('#li_container' + i);
		} else {
			jQuery('#li_container' + i + ' > ol > li').appendTo('#li_container' + i + ' ol:first');
			for (var j=1; j<=onecolSize; j++) {
				jQuery('#li_container' + i + ' ol:first li').removeAttr('class').removeAttr('style');
			}
			jQuery('#li_container' + i + ' ol:first').removeAttr('style').removeClass('li_cont1').insertBefore('#li_container' + i);
		}
		jQuery('#li_container' + i).remove();
	});
}
