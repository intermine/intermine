// jQuery Context Menu Plugin
//
// Version 1.01
//
// Cory S.N. LaViska
// A Beautiful Site (http://abeautifulsite.net/)
//
// More info: http://abeautifulsite.net/2008/09/jquery-context-menu-plugin/
//
// Terms of Use
//
// This plugin is dual-licensed under the GNU General Public License
//   and the MIT License and is copyright A Beautiful Site, LLC.
//
if(jQuery)( function() {
  jQuery.extend(jQuery.fn, {

    contextMenu: function(o, callback) {
      // Defaults
      if( o.menu == undefined ) return false;
      if( o.inSpeed == undefined ) o.inSpeed = 150;
      if( o.outSpeed == undefined ) o.outSpeed = 75;
      if (o.leftButton == undefined) o.leftButton = false;
      // 0 needs to be -1 for expected results (no fade)
      if( o.inSpeed == 0 ) o.inSpeed = -1;
      if( o.outSpeed == 0 ) o.outSpeed = -1;
      // Loop each context menu
      jQuery(this).each( function() {
        var el = jQuery(this);
        var offset = jQuery(el).offset();
        // Add contextMenu class
        jQuery('#' + o.menu).addClass('contextMenu');
        // Simulate a true right click
        jQuery(this).mousedown( function(e) {
          var evt = e;
          evt.stopPropagation();
          jQuery(this).mouseup( function(e) {
            e.stopPropagation();
            var srcElement = jQuery(this);
            jQuery(this).unbind('mouseup');
            if( evt.button == 2 || o.leftButton == true) {
              // Hide context menus that may be showing
              jQuery(".contextMenu").hide();
              // Get this context menu
              var menu = jQuery('#' + o.menu);

              if( jQuery(el).hasClass('disabled') ) return false;

              // Detect mouse position
              var d = {}, x, y;
              if( self.innerHeight ) {
                d.pageYOffset = self.pageYOffset;
                d.pageXOffset = self.pageXOffset;
                d.innerHeight = self.innerHeight;
                d.innerWidth = self.innerWidth;
              } else if( document.documentElement &&
                document.documentElement.clientHeight ) {
                d.pageYOffset = document.documentElement.scrollTop;
                d.pageXOffset = document.documentElement.scrollLeft;
                d.innerHeight = document.documentElement.clientHeight;
                d.innerWidth = document.documentElement.clientWidth;
              } else if( document.body ) {
                d.pageYOffset = document.body.scrollTop;
                d.pageXOffset = document.body.scrollLeft;
                d.innerHeight = document.body.clientHeight;
                d.innerWidth = document.body.clientWidth;
              }
              (e.pageX) ? x = e.pageX : x = e.clientX + d.scrollLeft;
              (e.pageY) ? y = e.pageY : y = e.clientY + d.scrollTop;

              // Show the menu
              jQuery(document).unbind('click');
              jQuery(menu).css({ top: y, left: x }).fadeIn(o.inSpeed);
              // Hover events
              jQuery(menu).find('A').mouseover( function() {
                jQuery(menu).find('LI.hover').removeClass('hover');
                jQuery(this).parent().addClass('hover');
              }).mouseout( function() {
                jQuery(menu).find('LI.hover').removeClass('hover');
              });

              // Keyboard
              jQuery(document).keypress( function(e) {
                switch( e.keyCode ) {
                  case 38: // up
                    if( jQuery(menu).find('LI.hover').size() == 0 ) {
                      jQuery(menu).find('LI:last').addClass('hover');
                    } else {
                      jQuery(menu).find('LI.hover').removeClass('hover').prevAll('LI:not(.disabled)').eq(0).addClass('hover');
                      if( jQuery(menu).find('LI.hover').size() == 0 ) jQuery(menu).find('LI:last').addClass('hover');
                    }
                  break;
                  case 40: // down
                    if( jQuery(menu).find('LI.hover').size() == 0 ) {
                      jQuery(menu).find('LI:first').addClass('hover');
                    } else {
                      jQuery(menu).find('LI.hover').removeClass('hover').nextAll('LI:not(.disabled)').eq(0).addClass('hover');
                      if( jQuery(menu).find('LI.hover').size() == 0 ) jQuery(menu).find('LI:first').addClass('hover');
                    }
                  break;
                  case 13: // enter
                    jQuery(menu).find('LI.hover A').trigger('click');
                  break;
                  case 27: // esc
                    jQuery(document).trigger('click');
                  break
                }
              });

              // When items are selected
              jQuery('#' + o.menu).find('A').unbind('click');
              jQuery('#' + o.menu).find('LI:not(.disabled) A').click( function() {
                jQuery(document).unbind('click').unbind('keypress');
                jQuery(".contextMenu").hide();
                // Callback
                if( callback ) callback( jQuery(this).attr('href').substr(1), jQuery(srcElement), {x: x - offset.left, y: y - offset.top, docX: x, docY: y} );
                return false;
              });

              // Hide bindings
              setTimeout( function() { // Delay for Mozilla
                jQuery(document).click( function() {
                  jQuery(document).unbind('click').unbind('keypress');
                  jQuery(menu).fadeOut(o.outSpeed);
                  return false;
                });
              }, 0);
            }
          });
        });

        // Disable text selection
        if( jQuery.browser.mozilla ) {
          jQuery('#' + o.menu).each( function() { jQuery(this).css({ 'MozUserSelect' : 'none' }); });
        } else if( jQuery.browser.msie ) {
          jQuery('#' + o.menu).each( function() { jQuery(this).bind('selectstart.disableTextSelect', function() { return false; }); });
        } else {
          jQuery('#' + o.menu).each(function() { jQuery(this).bind('mousedown.disableTextSelect', function() { return false; }); });
        }
        // Disable browser context menu (requires both selectors to work in IE/Safari + FF/Chrome)
        jQuery(el).add(jQuery('UL.contextMenu')).bind('contextmenu', function() { return false; });

      });
      return jQuery(this);
    },

    // Disable context menu items on the fly
    disableContextMenuItems: function(o) {
      if( o == undefined ) {
        // Disable all
        jQuery(this).find('LI').addClass('disabled');
        return( jQuery(this) );
      }
      jQuery(this).each( function() {
        if( o != undefined ) {
          var d = o.split(',');
          for( var i = 0; i < d.length; i++ ) {
            jQuery(this).find('A[href="' + d[i] + '"]').parent().addClass('disabled');

          }
        }
      });
      return( jQuery(this) );
    },

    // Enable context menu items on the fly
    enableContextMenuItems: function(o) {
      if( o == undefined ) {
        // Enable all
        jQuery(this).find('LI.disabled').removeClass('disabled');
        return( jQuery(this) );
      }
      jQuery(this).each( function() {
        if( o != undefined ) {
          var d = o.split(',');
          for( var i = 0; i < d.length; i++ ) {
            jQuery(this).find('A[href="' + d[i] + '"]').parent().removeClass('disabled');

          }
        }
      });
      return( jQuery(this) );
    },

    // Disable context menu(s)
    disableContextMenu: function() {
      jQuery(this).each( function() {
        jQuery(this).addClass('disabled');
      });
      return( jQuery(this) );
    },

    // Enable context menu(s)
    enableContextMenu: function() {
      jQuery(this).each( function() {
        jQuery(this).removeClass('disabled');
      });
      return( jQuery(this) );
    },

    // Destroy context menu(s)
    destroyContextMenu: function() {
      // Destroy specified context menus
      jQuery(this).each( function() {
        // Disable action
        jQuery(this).unbind('mousedown').unbind('mouseup');
      });
      return( jQuery(this) );
    }

  });
})(jQuery);