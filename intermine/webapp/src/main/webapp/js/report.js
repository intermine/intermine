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
  var rows = table.find('tbody tr');
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
      var target = (jQuery.browser.msie) ? table.parent() : table.parent().parent();
      jQuery('<div/>', {
          'class': 'toggle',
          'html': jQuery('<a/>', {
              'class': 'more',
              'text': 'Show more rows',
              'title': 'Show more rows',
              'style': 'float:right;',
              'click': function(event) {
                showMoreRows(e, 1, 10);
                event.preventDefault();
              }
          })
      }).appendTo(target);
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
      'href': '#',
      'class': 'less',
      'style': 'float:right;margin-left:20px;',
      'text': 'Collapse',
      'title': 'Collapse',
      'html': jQuery('<span/>', {
        'text': 'Collapse'
      }),
      'click': function(event) {
        collapseTable(e, maxCount);
        event.preventDefault();
      }
    }).appendTo(table.parent().parent().find('div.toggle'));
    // also show all show
    table.parent().parent().find('div.show-in-table').css('display', '');
  }

  // if the count is > 0 (< 30 entries) or 4th round (30+ entries) at this
  // point, show a link to table instead
  if (count > 0 || round == ((numberOfTableRowsToShow/10)-1)) {
    table.parent().parent().find('div.toggle a.more').hide();
  } else {
    round = parseInt(round) + 1;
    // update toggle count
    table.parent().parent().find('div.toggle a.more').remove();
    jQuery('<a/>', {
      'class': 'more',
      'text': 'Show more rows',
      'title': 'Show more rows',
      'style': 'float:right;',
      'click': function(event) {
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
  var table = jQuery(e + ' table');

  // show but the first 10 rows
  table.find('tbody tr').each(function(index) {
    if (index < 10) {
      jQuery(this).show();
    } else {
      jQuery(this).hide();
    }
  });

  // scroll to the table
  jQuery(e).scrollTo('fast', 'swing', -60);

  // remove collapser
  table.parent().parent().find('div.toggle a.less').hide();

  // remove toggler and replace with a first round version
  // update toggle count
  table.parent().parent().find('div.toggle a.more').remove();

  jQuery('<a/>', {
    'class': 'more',
    'text': 'Show more rows',
    'title': 'Show more rows',
    'style': 'float:right;',
    'html': jQuery('<span/>', {
      'text': 'Show more rows'
    }),
    'click': function(event) {
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
    'href': '#',
    'class': 'toggler',
    'text': 'Show more rows',
    'title': 'Show more rows',
    'style': 'float:right;',
    'html': jQuery('<span/>', {
      'text': 'Show 10 rows'
    }),
    'click': function(event) {
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
      'href': '#',
      'class': 'collapser',
      'style': 'float:right;margin-left:20px;',
      'text': 'Collapse',
      'title': 'Collapse',
      'html': jQuery('<span/>', {
        'text': 'Collapse'
      }),
      'click': function(event) {
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
      'href': '#',
      'class': 'toggler',
      'style': 'float:right;margin-left:20px;',
      'text': 'Collapse',
      'title': 'Collapse',
      'html': jQuery('<span/>', {
        'text': 'Show 10 rows'
      }),
      'click': function(event) {
        showMoreRowsTemplate(e, round, maxCount);
        event.preventDefault();
      }
    }).appendTo(table.parent().parent().find('p.in_table'));
  }

  return false;
}