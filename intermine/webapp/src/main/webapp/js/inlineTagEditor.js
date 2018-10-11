(function($) {

	var editingTag = false;
	var inputType;
	var inputPrefix;

	this.getAddedTag = function(editorId) {
	  return $('#' + inputPrefix + '-' + editorId).val();
	};

	this.startEditingTag = function(editorId) {
	  if (editingTag) {
	    stopEditingTag();
	  }
	  editingTag = editorId;
	  showEl('switchLink-' + editorId);
	  hideEl('addLink-' + editingTag);
	  setInputType(editorId, 'select');
	  showEl('tagsEdit-' + editorId);
	};

	this.setInputType = function(editorId, type) {
	  var title;
	  if (type == 'select') {
		inputPrefix = "tagSelect";
	    title = 'New tag';
	    showEl('tagSelect-' + editorId);
	    hideEl('tagValue-' + editorId);
	  } else {
		inputPrefix = "tagValue";
	    title = 'Select tag';
	    showEl('tagValue-' + editorId);
	    hideEl('tagSelect-' + editorId);
	    $('#tagValue-' + editorId).focus();
	  }
	  $('#switchLink-' + editorId).innerHTML = title;
	};

	this.switchTagInput = function(editorId) {
	  if (inputPrefix == 'tagSelect') {
	    setInputType(editorId, 'new');
	  } else {
	    setInputType(editorId, 'select');
	  }
	};

	this.stopEditingTag = function() {
	  if (editingTag) {
	    hideEl('tagsEdit-' + editingTag);
	    showEl('addLink-' + editingTag);
	    hideEl('switchLink-' + editingTag);
	  }
	  editingTag = false;
	};
	
	var CANNOT_TAG_THIS = 'Adding tag failed. It is not possible to tag this item.';
	var cannotTagNotification = new FailureNotification({message: CANNOT_TAG_THIS});

	this.addTag = function(editorId, type) {
	  var taggedObject = parseTagged(editorId);;
	  var tag = getAddedTag(editorId, type);
	  
	  if ( !taggedObject ) {
		  return cannotTagNotification.render();
	  }
	  
	  var callBack = function(returnStr) {
	    if (returnStr == 'ok') {
	      refreshTags(editorId, type);
	    } else {
	      new FailureNotification({message: returnStr}).render();
	    }
	  }
	  
	  if (tag != '') {
	    AjaxServices.addTag(tag, taggedObject, type, callBack);
	  }
	};

	this.deleteTag = function(tag, editorId, type) {
	  var callBack = function(returnStr) {
	    if (returnStr == 'ok') {
	      refreshTags(editorId, type);
	    } else {
	      new FailureNotification({message: returnStr}).render();
	    }
	  }
	  taggedObject = parseTagged(editorId);
	  if (taggedObject == '') {
	    window.alert('Deleting tag failed. It is not possible delete tag for this item.');
	  }
	  AjaxServices.deleteTag(tag, taggedObject, type, callBack);
	};

	this.displayTags = function(editorId, type, tags) {
	  var i, tag, tagsl = tags.length, parent = $('#currentTags-' + editorId).empty();
	  
	  for (i = 0; i < tagsl; i++) {
	    tag = tags[i];
	    addTagSpan(editorId, type, tag);
	  }
	};

	this.addTagSpan = function(editorId, type, tag) {
	  var parent = $('#currentTags-' + editorId);
	  var span = $('<span class="tag">');
	  var a = $('<a class="deleteTagLink">[x]</a>');
	  a.click(function() {
		  deleteTag(tag, editorId, type);
	  });
	  span.append(tag).append(a);
	  parent.append(span);
	};

	// Refreshes displayed tags for specified tagged object and refreshes selects in InlineTagEditors
	this.refreshTags = function(editorId, type) {
	  refreshObjectTags(parseTagged(editorId), type);
	  refreshTagSelects(type);
	};

	this.refreshObjectTags = function(tagged, type) {
	  var callBack = function(tags) {
		var currentTagHolders = $('.current-tags');
		currentTagHolders.map(function(i) {
			var editorId, id = this.id;
			
			if (id && id.indexOf('currentTags-') == 0) {
				editorId = id.replace('currentTags-', '');
				if (parseTagged(editorId) == tagged) {
					displayTags(editorId, type, tags);
				}
			}
			
		});
	  }
	  AjaxServices.getObjectTags(type, tagged, callBack);
	};

	this.refreshTagSelects = function(type) {
	  var callBack = function(tags) {
	    var selects = document.getElementsByTagName('select');
	    for (var i = 0; i < selects.length; i++) {
	      var select = selects[i];
	      if (select.id.indexOf('tagSelect-') == 0) {
	        setSelectElement(select.id, getSelectTitle(select), tags);
	      }
	    }
	  }
	  AjaxServices.getTags(type, callBack);
	};

	// retrieves tagged object id from editorId where it is hidden
	this.parseTagged = function(editorId) {
	  return $('#taggable-' + editorId).val();
	};
}).call(window, jQuery);
