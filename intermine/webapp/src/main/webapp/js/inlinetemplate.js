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
  var e = '#' + uid.replace(/:/g, '_') + ' div.collection-table';
  if (jQuery(e).length == 0) throw new Error('I got no target div for "' + templateName + '"');

  jQuery(e).show();
  jQuery(e).innerHTML = placement + templateName + id;

  // return the table in 'result'
  jQuery.ajax({
    url: modifyDetailsURL,
    dataType: 'html',
    data: 'method=ajaxTemplateCount&template='+templateName+'&id='+id+'&type=global&placement='+placement+'&detailsType='+detailsType+'&trail='+trail,
    success: function(result) {
      jQuery(e).html(result);
      // dequeue
      loadInlineTemplate(i+1);
    },
    error: function(jXHR, textStatus) {
      throw new Error('failed to load template "' + templateName + '", ' + textStatus);
    },
    complete: function(jXHR, textStatus) {
        // get rid of the loading spinner
    	jQuery(e).parent().find('div.loading-spinner').remove();
    }
  });

}

// ]]>-->