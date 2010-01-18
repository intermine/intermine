var fixedOps = new Array();

/***********************************************************
* Called when user chooses a constraint operator. If the
* user picks an operator contained in fixedOptionsOps then
* the input box is hidden and the user can only choose
**********************************************************/
function updateConstraintForm(index, attrOpElement, attrOptsElement, attrValElement) {
	if (attrOptsElement == null)
		return;
	
	for (var i = 0 ; i < fixedOps.length ; i++) {
		if (attrOpElement.value == fixedOps[i]) {
			document.getElementById("operandEditSpan" + index).style.display = "none";
			attrValElement.value = attrOptsElement.value; // constrain value
			return;
		}
	}
	document.getElementById("operandEditSpan" + index).style.display = "";
}

/***********************************************************
* Use bag checkbox has been clicked.
**********************************************************/
function clickUseBag(index) {
	var useBag = document.templateForm["useBagConstraint("+index+")"].checked;

	document.templateForm["attributeOps("+index+")"].disabled=useBag;
	if (document.templateForm["attributeOptions("+index+")"]) {
		document.templateForm["attributeOptions("+index+")"].disabled=useBag;
	}
	document.templateForm["attributeValues("+index+")"].disabled=useBag;
	document.templateForm["bag("+index+")"].disabled=!useBag;
	document.templateForm["bagOp("+index+")"].disabled=!useBag;	
}

function initClickUseBag(index) {
	if(selectedBagName){
		document.templateForm["bag("+index+")"].value=selectedBagName;
		document.templateForm["useBagConstraint("+index+")"].checked = true;
	}
	clickUseBag(index);
}

/***********************************************************
* Init attribute value with selected item and hide input box if
* required
**********************************************************/
/*function initConstraintForm(index, attrOpElement, attrOptsElement, attrValElement)
{
if (attrOptsElement == null)
return;

attrValElement.value = attrOptsElement.value;
updateConstraintForm(index, attrOpElement, attrOptsElement, attrValElement);
}*/

/***************Handle display of help windows******************************/
//var helpMsgArray = new Array();
//var ourDate;
//var isIE = '${iePre7}';
//function displayHelpMsg(e, id) {
//if(isIE != 'true') {
//// IE is retarded and doesn't pass the event object
//if (e == null)
//e = window.event;
//
//// IE uses srcElement, others use target
//var target = e.target != null ? e.target : e.srcElement;
//// grab the clicked element's position
//_clientX = e.clientX;
//_clientY = e.clientY;
//
//// grab the clicked element's position
//_offsetX = ExtractNumber(target.style.left);
//_offsetY = ExtractNumber(target.style.top);
//
//$(id).style.left = (_clientX + _offsetX) + "px";
//$(id).style.top = (_clientY + _offsetY) + "px";
//jQuery('#' + id).fadeIn(300);
//helpMsgArray[helpMsgArray.length] = id;
//ourDate = new Date().getTime();
//} else {
//$(id).style.position = "relative";
//$(id).style.display = "block";
//}
//}
//document.body.onclick = function clearHelpMsg() {
//newDate = new Date().getTime();
//if(newDate > (ourDate + 100)){
//    										 for(var i=0;i<helpMsgArray.length;i++) {
//jQuery('#' + helpMsgArray[i]).hide(300);
//$('DivShim').style.display = "none";
//}
//}
//}
//function ExtractNumber(value)
//{
//var n = parseInt(value);
//
//return n == null || isNaN(n) ? 0 : n;
//}

function forwardToLinks() {
	// needed validation that bag is not used, validation is performed in the Struts action as well
	if (isBagUsed()) {
		new Insertion.Bottom('error_msg','Link could not be created. This template contains a list constraint, which is currently not supported.  Remove the list constraint and try again.<br/>');
		haserrors=1;
		jQuery('#error_msg').fadeIn();
		return;
	}
	document.getElementById('actionType').value = 'links';
	document.templateForm.submit();
}

function isBagUsed() {
	// checks if bag is used, the presumption is that there aren't more than 10 bag constraints
	for (var i = 0; i < 10; i++) {
		if (document.templateForm["useBagConstraint("+i+")"]) {
			if (document.templateForm["useBagConstraint("+i+")"].checked) {
				return true;
			}
		}
	}
	return false;
}

function updateAttributeValues(index) {
	var attributeValues = document.templateForm['attributeValues('+index+')'];
	var selectedString = '';
	var attributeOptions = document.templateForm['attributeOptions('+index+')'];
	if (attributeOptions) {
		var i;
		var count = 0;
		for (i = 0; i < attributeOptions.options.length; i++) {
			if (attributeOptions.options[i].selected) {
//				var selectedValue = '\'' + attributeOptions.options[i].value + '\'';
				var selectedValue = attributeOptions.options[i].value;
				if (selectedString != '') {
					selectedString += ',';	
				}
				selectedString += selectedValue;
			}	
		}	
		attributeValues.value = selectedString;		
	}
}	


// FIXME this is broken - setSelectElement is in imutils
function filterByTag(tag) {
	if (tag != "") {
		if (origSelectValues == null) {
			saveOriginalSelect();
		}
		var callBack = function(filteredList) {
			setSelectElement('bagSelect', '', filteredList);
		}
		AjaxServices.filterByTag('bag', tag, callBack);
	} else {
		restoreOriginalSelect();
	}
}
var origSelectValues = null;

function saveOriginalSelect() {
	origSelectValues = getSelectValues('bagSelect');
}

function restoreOriginalSelect() {
	if (origSelectValues != null) {
		   setSelectElement('bagSelect', '', origSelectValues);
	}
}
