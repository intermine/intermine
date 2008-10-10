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
