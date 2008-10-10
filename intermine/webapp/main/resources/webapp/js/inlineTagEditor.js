var editingTag;
var inputType;

function getAddedTag(uid, type) {
	if (getInputType(uid, type) == 'select') {
		return getSelectValue('tagSelect-' + uid);
	} else {
		return $('tagValue-' + uid).value;
	}
}

function startEditingTag(uid) {
	if (editingTag) {
		stopEditingTag();
	}
	editingTag = uid;
	showEl('switchLink-' + uid);
	hideEl('addLink-' + editingTag);
	setInputType(uid, 'select');
	showEl('tagsEdit-' + uid);
}

function setInputType(uid, type) {
	inputType = type;
	if (type == 'select') {
		var title = 'New tag';
		showEl('tagSelect-' + uid);
		hideEl('tagValue-' + uid);
	} else {
		var title = 'Select tag';
		showEl('tagValue-' + uid);
		hideEl('tagSelect-' + uid);
		$('tagValue-' + uid).focus();
	}
	$('switchLink-' + uid).innerHTML = title;
}

function switchTagInput(uid) {
	if (getInputType(uid) == 'select') {
		setInputType(uid, 'new');
	} else {
		setInputType(uid, 'select');
	}
}

function getInputType(uid) {
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

function addTag(uid, type) {
	var tag = getAddedTag(uid, type);
	var callBack = function(success) {
		if (success) {
			refreshTags(uid, type);
		} else {
			window.alert('Adding tag failed.');
		}
	} 
	AjaxServices.addTag(tag, uid, type, callBack);
}

function deleteTag(tag, uid, type) {
	var callBack = function(success) {
		if (success) {
			refreshTags(uid, type);
		} else {
			window.alert('Deleting tag failed.');
		}
	}
	AjaxServices.deleteTag(tag, uid, type, callBack);
}

function displayTags(uid, type, tags) {
	var parent = $('currentTags-' + uid);
	parent.innerHTML = '';
	for (var i = 0; i < tags.length; i++) {
		var tag = tags[i];
		addTagSpan(uid, type, tag);
	}
}

function addTagSpan(uid, type, tag) {
	var parent = $('currentTags-' + uid);
	var span = document.createElement('span');
	span.setAttribute('class', 'tag');
	// for IE
	span.setAttribute('className', 'tag');
	span.innerHTML = tag + '<a class="deleteTagLink" onclick="javascript:deleteTag(\'' + tag + '\', \'' + uid + '\', \'' + type + '\')">[x]</a>&nbsp;';
	parent.appendChild(span);
}

function refreshTags(uid, type) {
	var callBack = function(tags) {
		displayTags(uid, type, tags);
	}
	AjaxServices.getObjectTags(type, uid, callBack);	
}
