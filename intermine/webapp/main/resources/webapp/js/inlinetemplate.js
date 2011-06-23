<!--// <![CDATA[

queue = [];

function queueInlineTemplateQuery(placement, templateName, id, trail) {
    queue.push([placement, templateName, id, trail]);
}

jQuery(document).ready(function() {
  loadInlineTemplate(0);
});

/**
 * Load report page template (jQuery)
 *
 * @param i
 * @return
 */
function loadInlineTemplate(i) {
  if (i >= queue.length) {
    return;
  }

  var placement = queue[i][0];
  var templateName = queue[i][1];
  var id = queue[i][2];
  var trail = queue[i][3];
  var uid = placement.replace(/ /, '_') + '_' + templateName;

  // element to modify, replacing ":" as these are jQuery selectors albeit
  // valid in div id value
  var e = '#table_' + uid.replace(/:/g, '_') + '_int';

  jQuery(e).show();
  jQuery(e).innerHTML = placement + templateName + id;

  jQuery.ajax({
    url: modifyDetailsURL,
    dataType: 'html',
    data: 'method=ajaxTemplateCount&template='+templateName+'&id='+id+'&type=global&placement='+placement+'&detailsType='+detailsType+'&trail='+trail,
    success: function(result) {
      jQuery(e).hide();
      jQuery(e).html(result);
      jQuery(e).fadeIn();

      // remove any fail messages
      if (jQuery(e).parent().find('p.fail').length !== 0) {
        jQuery(e).parent().find('p.fail').remove();
      }

      loadInlineTemplate(i+1);
    },
    error: function(jXHR, textStatus) {
      // on fail append a retry to the parent if not present
      if (jQuery(e).parent().find('p.fail').length == 0) {
        jQuery(e).parent().append('<p class="fail theme-7-background">Failed to load the data. <a class="theme-1-color" href="#" onclick="return\
        toggleCollectionVisibilityJQuery(\'' + placement + '\',\'' + field + '\',\'' + object_id + '\',\'' + trail + '\');">Try again</a></p>');
      }
    },
    complete: function(jXHR, textStatus) {
        // get rid of the loading message
        jQuery(e).parent().find('p.loading').remove();
    }
  });

}

/**
 * Load report page table (jQuery)
 *
 * @param placement
 * @param field
 * @param object_id
 * @param trail
 * @return
 */
function toggleCollectionVisibilityJQuery(placement, field, object_id, trail) {

  // element to modify, replacing ":" as these are jQuery selectors albeit
  // valid in div id value
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
                table.parent().parent().append('<p class="toggle"><a href="#" class="theme-1-color toggler" onclick="return showMoreRows(\'' + e + '\', 1);">Show more rows</a></p>');
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
            jQuery(e).parent().append('<p class="fail">Failed to load the data. <a class="theme-1-color" href="#" onclick="return\
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

// ]]>-->