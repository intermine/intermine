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

function toggleCollectionVisibility(placement, field, object_id) {
  if ($('coll_'+placement+'_'+field+'_inner').innerHTML=='') {
    // need to fetch
    new Ajax.Updater('coll_'+placement+'_'+field+'_inner', modifyDetailsURL, {
      parameters:'method=ajaxVerbosify&placement='+placement+'&field='+field+'&id='+object_id,
      asynchronous:true
    });
  } else {
    new Ajax.Request(modifyDetailsURL, {
      parameters:'method=ajaxVerbosify&placement='+placement+'&field='+field+'&id='+object_id,
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
