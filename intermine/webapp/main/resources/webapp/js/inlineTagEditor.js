var editingTag;
var inputType;

function getAddedTag(tagged, type) {
	if (getInputType(tagged, type) == 'select') {
		return getSelectValue('tagSelect-' + tagged);
	} else {
		return $('tagValue-' + tagged).value;
	}
}

function startEditingTag(tagged) {
	if (editingTag) {
		stopEditingTag();
	}
	editingTag = tagged;
	showEl('switchLink-' + tagged);
	hideEl('addLink-' + editingTag);
	setInputType(tagged, 'select');
	showEl('tagsEdit-' + tagged);
}

function setInputType(tagged, type) {
	inputType = type;
	if (type == 'select') {
		var title = 'New tag';
		showEl('tagSelect-' + tagged);
		hideEl('tagValue-' + tagged);
	} else {
		var title = 'Select tag';
		showEl('tagValue-' + tagged);
		hideEl('tagSelect-' + tagged);
		$('tagValue-' + tagged).focus();
	}
	$('switchLink-' + tagged).innerHTML = title;
}

function switchTagInput(tagged) {
	if (getInputType(tagged) == 'select') {
		setInputType(tagged, 'new');
	} else {
		setInputType(tagged, 'select');
	}
}

function getInputType(tagged) {
	return inputType;
}

function stopEditingTag() {
	if (editingTag) {
		hideEl('tagsEdit-' + editingTag);
		showEl('addLink-' + editingTag);
		hideEl('switchLink-' + editingTag);
	}
	editingTag = '';
}

function addTag(tagged, type) {
	var tag = getAddedTag(tagged, type);
	var callBack = function(returnStr) {
		if (returnStr == 'ok') {
			refreshTags(tagged, type);
		} else {
			window.alert(returnStr);
		}
	}
	if (tag != '') {
		AjaxServices.addTag(tag, tagged, type, callBack);
	}
}

function deleteTag(tag, tagged, type) {
	var callBack = function(returnStr) {
		if (returnStr == 'ok') {
			refreshTags(tagged, type);
		} else {
			window.alert(returnStr);
		}
	}
	AjaxServices.deleteTag(tag, tagged, type, callBack);
}

function displayTags(tagged, type, tags) {
	var parent = $('currentTags-' + tagged);
	parent.innerHTML = '';
	for (var i = 0; i < tags.length; i++) {
		var tag = tags[i];
		addTagSpan(tagged, type, tag);
	}
}

function addTagSpan(tagged, type, tag) {
	var parent = $('currentTags-' + tagged);
	var span = document.createElement('span');
	span.setAttribute('class', 'tag');
	// for IE
	span.setAttribute('className', 'tag');
	span.innerHTML = tag + '<a class="deleteTagLink" onclick="javascript:deleteTag(\'' + tag + '\', \'' + tagged + '\', \'' + type + '\')">[x]</a>&nbsp;';
	parent.appendChild(span);
}

// Refreshes displayed tags for specified tagged object and refreshes selects in InlineTagEditors 
function refreshTags(tagged, type) {
	var callBack = function(tags) {
		displayTags(tagged, type, tags);
	}
	AjaxServices.getObjectTags(type, tagged, callBack);
	refreshTagSelects(type);
}

function refreshTagSelects(type) {
	var callBack = function(tags) {
		var selects = document.getElementsByTagName('select');
		for (var i = 0; i < selects.length; i++) {
			var select = selects[i];
			if (select.id.indexOf('tagSelect-') == 0) {
				setSelectElement(select.id, '-- filter by tag --', tags);
			}
		}			
	}
	AjaxServices.getTags(type, callBack);
}

