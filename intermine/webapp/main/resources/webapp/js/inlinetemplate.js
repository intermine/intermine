<!--//<![CDATA[

queue = [];

function queueInlineTemplateQuery(placement, templateName, id, trail) {
    queue.push([placement, templateName, id, trail]);
}

/* Called onload */
function loadInlineTemplates() {
  loadInlineTemplate(0);
}

function loadInlineTemplate(i) {
  if (i >= queue.length) {
    return;
  }

  var placement = queue[i][0];
  var templateName = queue[i][1];
  var id = queue[i][2];
  var trail = queue[i][3];
  var uid = placement.replace(/ /, '_') + '_' + templateName;

  Element.show('table_'+uid+'_int');
  $('table_'+uid+'_int').innerHTML = placement + templateName + id;
  new Ajax.Updater('table_'+uid+'_int', modifyDetailsURL, {
    parameters:'method=ajaxTemplateCount&template='+templateName+'&id='+id+'&type=global&placement='+placement+'&detailsType='+detailsType+'&trail='+trail, asynchronous:true,
    onComplete: function() {
      var count = $('count_'+uid).innerHTML;
      if (count == '0')
        $('img_'+uid).src='images/blank.gif';
      else
        $('img_'+uid).src='images/plus.gif';
      // load the next one
      loadInlineTemplate(i+1);
    },
    evalScripts: true
  });
}

/**
 * Load report page table (jQuery)
 * @param placement
 * @param field
 * @param object_id
 * @param trail
 * @return
 */
function toggleCollectionVisibilityJQuery(placement, field, object_id, trail) {

  // element to modify, replacing ":" as these are jQuery selectors albeit valid in div id value
  var e = '#coll_'+placement.replace(/:/g, '_')+field+'_inner';

  // is the target table empty?
  if (jQuery(e).is(":empty")) {
    // append a loading message and thus set the element as not empty
    jQuery(e).append("<p class='loading'>&nbsp;</p>");
    // need to fetch
    jQuery.ajax({
        url: modifyDetailsURL,
        dataType: 'html',
        data: 'method=ajaxVerbosify&placement=' + placement + '&field=' + field + '&id=' + object_id + '&trail=' + trail,
        success: function(result) {
          // place result in div with a fade
          jQuery(e).append(result);

          // do we table?
          if (jQuery(e).find('table.refSummary').length !== 0) {
            var table = jQuery(e).find('table.refSummary');
            // hide the table
            table.toggle();

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
                table.parent().append('<p class="toggle"><a href="#" onclick="return showMoreRows(\'' + e + '\', 1);">Show more rows</a></p>');
            }

            // fade it in
            table.fadeIn();
          }

          // remove any fail messages
          if (jQuery(e).parent().find('p.fail').length !== 0) {
            jQuery(e).parent().find('p.fail').remove();
          }
        },
        error: function(jXHR, textStatus) {
          // on fail append a retry to the parent if not present
          if (jQuery(e).parent().find('p.fail').length == 0) {
            jQuery(e).parent().append('<p class="fail">Failed to load the data. <a href="#" onclick="return\
            toggleCollectionVisibilityJQuery(\'' + placement + '\',\'' + field + '\',\'' + object_id + '\',\'' + trail + '\');">Try again</a></p>');
          }
        },
        complete: function(jXHR, textStatus) {
          // get rid of the loading message
          jQuery(e).parent().find('p.loading').remove();
        }
    });
  }

  return false;
}

/**
 * Toggle upto 10 rows in a table above that are currently hidden
 * @param e
 * @return
 */
function showMoreRows(e, round) {
  // fetch the associated table
  var table = jQuery(e + ' table');

  // fetch all rows that are not shown yet
  var rows = table.find('tbody tr:hidden')
  // count of max number of rows to show
  var count = 10;
  // traverse rows
  rows.each(function(index) {
    if (count > 0) {
      // show row
      jQuery(this).css('display', '');
      count--;
    }
  });

  // if the count is > 0 (< 30 entries) or 3rd round (30+ entries) at this point, show a link to table instead
  if (count > 0 || round == 2) {
    table.parent().find('p.toggle').css('display', 'none');
    table.parent().parent().find('p.in_table').css('display', '');
  } else {
    round = parseInt(round) + 1;
    // update toggle count
    table.parent().find('p.toggle').html('<a href="#" onclick="return showMoreRows(\'' + e + '\', ' + round + ');">Show more rows</a>');
  }

  return false;
}

/* load report page table (Prototype) */
function toggleCollectionVisibility(placement, field, object_id, trail) {
  if ($('coll_'+placement+'_'+field+'_inner').innerHTML=='') {
    // need to fetch
    new Ajax.Updater('coll_'+placement+'_'+field+'_inner', modifyDetailsURL, {
      parameters:'method=ajaxVerbosify&placement='+placement+'&field='+field+'&id='+object_id+'&trail='+trail,
      asynchronous:true
    });
  } else {
    new Ajax.Request(modifyDetailsURL, {
      parameters:'method=ajaxVerbosify&placement='+placement+'&field='+field+'&id='+object_id+'&trail='+trail,
      asynchronous:true
    });
  }
  toggleSlide(placement, field);
  return false;
}

function toggleSlide(placement, field) {
  var img = $('img_'+placement+'_'+field).src;
  $('img_'+placement+'_'+field).src = (img.indexOf('images/minus.gif') >= 0 ? 'images/plus.gif' : 'images/minus.gif');
  Element.toggle('coll_'+placement+'_'+field);//, 'blind');//, {duration: 0.2});
}

function toggleTemplateList(placement, template) {
  var img = $('img_'+placement+'_'+template).src;
  $('img_'+placement+'_'+template).src = (img.indexOf('images/minus.gif') >= 0 ? 'images/plus.gif' : 'images/minus.gif');
  Element.toggle('table_'+placement+'_'+template);
  return false;
}

Event.observe(window, 'load', loadInlineTemplates, false);

//]]>-->