/**
 * jQuery function extension performing a 'scroll to target'
 */
jQuery.fn.extend({
  /**
   * @speed A string or number determining how long the animation will run
   * @easing A string indicating which easing function to use for the transition (linear or swing).
   * @val Extra offset in px
   * @onComplete A function to call once the animation is complete
   */
    scrollTo : function(speed, easing, val, onComplete) {
        return this.each(function() {
            var targetOffset = jQuery(this).offset().top + val;

            jQuery('html,body').animate({
                scrollTop: targetOffset
            }, speed, easing, onComplete);
        });
    }
});

/**
 * jQuery function extension checking if an element is (fully/partially) visible
 */
jQuery.fn.extend({
  /**
   * @visibility Full or partial visibility?
   */
  isInView : function(visibility) {
    var pageTop = jQuery(window).scrollTop();
    var pageBottom = pageTop + jQuery(window).height();

    var elementTop = jQuery(this).offset().top;
    var elementBottom = elementTop + jQuery(this).height();

    if (visibility == 'partial') {
      return ((elementBottom >= pageTop) && (elementTop <= pageBottom));
    } else {
      return ((elementBottom < pageBottom) && (elementTop > pageTop));
    }
  }
});

/**
 * The purpose of this function is to display max 10 rows in a "verbose" table (=
 * cached...)
 *
 * @param e
 * @return
 */
function trimTable(e) {
  // find our table
  var table = jQuery(e).find('table');

  // do we have more than 10 rows? XXX: hardcoded value
  var rows = table.find('tbody tr')
  if (rows.length > 10) {
      var count = 10;
      rows.each(function(index) {
        if (count > 0) {
          count--;
        } else {
          // hide rows 10+
          jQuery(this).css('display', 'none');
        }
      });
      // add a toggler for more rows
      jQuery('<div/>', {
          className: 'toggle',
          html: jQuery('<a/>', {
              className: 'more',
              text: 'Show more rows',
              title: 'Show more rows',
              click: function(event) {
                showMoreRows(e, 1, 10);
                event.preventDefault();
              }
          })
      }).appendTo(table.parent().parent());
  }

}

/**
 * Toggle upto ($count) rows in a table above that are currently hidden
 *
 * @param e element containing the table
 * @param round how many times have we asked for more rows
 * @param maxCount max number of rows to show
 * @return
 */
function showMoreRows(e, round, maxCount) {
  // fetch the associated table
  var table = jQuery(e + ' table');

  var count = maxCount;

  // fetch all rows that are not shown yet
  var rows = table.find('tbody tr:hidden')
  // traverse rows
  rows.each(function(index) {
    if (count > 0) {
      // show row
      jQuery(this).css('display', '');
      count = parseInt(count) - 1;
    }
  });

  // first round, show collapser
  if (round == 1) {
    jQuery('<a/>', {
      href: '#',
      className: 'less',
      style: 'float:right;',
      text: 'Collapse',
      title: 'Collapse',
      html: jQuery('<span/>', {
        text: 'Collapse'
      }),
      click: function(event) {
        collapseTable(e, maxCount);
        event.preventDefault();
      }
    }).appendTo(table.parent().parent().find('div.toggle'));
  }

  // if the count is > 0 (< 30 entries) or 4th round (30+ entries) at this
  // point, show a link to table instead
  if (count > 0 || round == ((numberOfTableRowsToShow/10)-1)) {
    table.parent().parent().find('div.toggle a.more').hide();
    table.parent().parent().find('div.show-in-table').css('display', '');
  } else {
    round = parseInt(round) + 1;
    // update toggle count
    table.parent().parent().find('div.toggle a.more').remove();
    jQuery('<a/>', {
      className: 'more',
      text: 'Show more rows',
      title: 'Show more rows',
      click: function(event) {
        showMoreRows(e, round, maxCount);
        event.preventDefault();
      }
    }).appendTo(table.parent().parent().find('div.toggle'));
  }

  return false;
}

/**
 * Collapse/close collection or reference
 *
 * @param e element containing the table
 * @return
 */
function collapseTable(e, maxCount) {
  im.log(e); // is calling inner!!! #coll_im_aspect_MiscellaneousdataSets_inner
  var table = jQuery(e + ' table');

  var count = maxCount;

  // show but the first 10 rows
  table.find('tbody tr').each(function(index) {
    if (count > 0) {
      jQuery(this).show();
    } else {
      jQuery(this).hide();
    }

    count--;
  });

  // scroll to the table
  jQuery(e).scrollTo('fast', 'swing', -60);

  // remove collapser
  table.parent().parent().find('div.toggle a.less').hide();

  // remove toggler and replace with a first round version
  // update toggle count
  table.parent().parent().find('div.toggle a.more').remove();

  jQuery('<a/>', {
    className: 'more',
    text: 'Show more rows',
    title: 'Show more rows',
    html: jQuery('<span/>', {
      text: 'Show more rows'
    }),
    click: function(event) {
      showMoreRows(e, 1, maxCount);
      event.preventDefault();
    }
  }).appendTo(table.parent().parent().find('div.toggle'));

  // hide the show all in table one
  table.parent().parent().find('div.show-in-table').hide();

  return false;
}

/**
 * Collapse/close template
 *
 * @param e element containing the table
 * @return
 */
function collapseTemplate(e, maxCount) {
  var table = jQuery(e + ' table');

  // hide description and table rows
  table.hide();
  jQuery(e).parent().find('p.description').hide();

  // scroll to the table
  if (typeof jQuery(e).scrollTo == 'function') { // seems to fail on bagDetails otherwise...
    jQuery(e).scrollTo('fast', 'swing', -60);
  }

  // remove collapser & toggler
  table.parent().parent().find('p.in_table a.collapser').remove();
  table.parent().parent().find('p.in_table a.toggler').remove();

  // hide "show all" if not
  table.parent().parent().find('p.in_table a.showAll').hide();

  // append a toggler back
  jQuery('<a/>', {
    href: '#',
    className: 'toggler',
    text: 'Show more rows',
    title: 'Show more rows',
    html: jQuery('<span/>', {
      text: 'Show 10 rows'
    }),
    click: function(event) {
      showMoreRowsTemplate(e, 1, maxCount);
      event.preventDefault();
    }
  }).appendTo(table.parent().parent().find('p.in_table'));

  return false;
}

/**
 * Toggle upto ($count) rows in a table inside a template
 *
 * @param e element containing the table
 * @param round how many times have we asked for more rows
 * @param maxCount max number of rows to show
 * @return
 */
function showMoreRowsTemplate(e, round, maxCount) {
  var table = jQuery(e + ' table');

  if (round == 1) {
    table.show();
    var rows = table.find('tbody tr.bodyRow');
    rows.each(function(index) {
      jQuery(this).hide();
    });
  }

  // fetch all rows that are not shown yet
  var rows = table.find('tbody tr.bodyRow:hidden');
  var count = maxCount;
  // traverse rows
  rows.each(function(index) {
   if (count > 0) {
      // show row
      jQuery(this).show();
      count = parseInt(count) - 1;
    }
  });

  // first round, show collapser
  if (round == 1) {
    jQuery('<a/>', {
      href: '#',
      className: 'collapser',
      style: 'float:right;',
      text: 'Collapse',
      title: 'Collapse',
      html: jQuery('<span/>', {
        text: 'Collapse'
      }),
      click: function(event) {
        collapseTemplate(e, maxCount);
        event.preventDefault();
      }
    }).appendTo(table.parent().parent().find('p.in_table'));
  }

  // if the count is > 0 (< 30 entries) or 4th round (30+ entries) at this
  // point, show a link to table instead
  if (count > 0 || round == (numberOfTableRowsToShow / 10)) {
    table.parent().parent().find('p.in_table a').css('display', '');
    table.parent().parent().find('p.in_table a.toggler').css('display', 'none');
  } else {
    round = parseInt(round) + 1;
    // remove toggle count
    table.parent().parent().find('p.in_table a.toggler').remove();
    // update toggle count
    jQuery('<a/>', {
      href: '#',
      className: 'toggler',
      text: 'Collapse',
      title: 'Collapse',
      html: jQuery('<span/>', {
        text: 'Show 10 rows'
      }),
      click: function(event) {
        showMoreRowsTemplate(e, round, maxCount);
        event.preventDefault();
      }
    }).appendTo(table.parent().parent().find('p.in_table'));
  }

  return false;
}