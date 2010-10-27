/*******************************************************************************
 * Use bag checkbox has been clicked.
 ******************************************************************************/
function clickUseBag(index) {
  var useBag = document.templateForm["useBagConstraint("+index+")"].checked;
  if (document.templateForm["attributeOps("+index+")"] && document.templateForm["attributeOps("+index+")"] != undefined)
      document.templateForm["attributeOps("+index+")"].disabled=useBag;
  if (document.templateForm["attributeOptions("+index+")"]) {
    document.templateForm["attributeOptions("+index+")"].disabled=useBag;
  }
  // if attributeValues is a radio button
  if (document.templateForm["attributeValues("+index+")"][0]) {
    document.templateForm["attributeValues("+index+")"][0].disabled=useBag;
    document.templateForm["attributeValues("+index+")"][1].disabled=useBag;
  }
  if (document.templateForm["multiValues("+index+")"]) {
    document.templateForm["multiValues("+index+")"].disabled=useBag;
  }
  if (document.templateForm["extraValues("+index+")"]) {
    document.templateForm["extraValues("+index+")"].disabled=useBag;
  }
  document.templateForm["attributeValues("+index+")"].disabled=useBag;
  document.templateForm["bag("+index+")"].disabled=!useBag;
  document.templateForm["bagOp("+index+")"].disabled=!useBag;
}

function forwardToLinks() {
  // needed validation that bag is not used, validation is performed in the
    // Struts action as well
  if (isBagUsed()) {
    new Insertion.Bottom('error_msg','Link could not be created. This template contains a list constraint, which is currently not supported.  Remove the list constraint and try again.<br/>');
    haserrors=1;
    jQuery('#error_msg').fadeIn();
    return;
  }
  document.getElementById('actionType').value = 'links';
  document.templateForm.submit();
}

function exportTemplate() {
    document.getElementById('actionType').value = 'exportTemplate';
    document.templateForm.submit();
}

function codeGenTemplate(method) {
    jQuery('#actionType').val(method);
    jQuery('#templateForm').attr('target', '_blank');
    jQuery('#templateForm').submit();
}

function isBagUsed() {
  // checks if bag is used, the presumption is that there aren't more than 10
    // bag constraints
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
  if (attributeOptions != undefined) {
    var i;
    var count = 0;
    for (i = 0; i < attributeOptions.options.length; i++) {
      if (attributeOptions.options[i].selected) {
// var selectedValue = '\'' + attributeOptions.options[i].value + '\'';
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

function updateMultiValueAttribute(index) {
  var multiValueAttribute = document.templateForm['multiValueAttribute('+index+')'];
  var selectedString = '';
  var multiValuesOptions = document.templateForm['multiValues('+index+')'];
  if (multiValuesOptions) {
    var i;
    var count = 0;
    for (i = 0; i < multiValuesOptions.options.length; i++) {
      if (multiValuesOptions.options[i].selected) {
        var selectedMultiValue = multiValuesOptions.options[i].value;
        if (selectedString != '') {
          selectedString += ',';
        }
        selectedString += selectedMultiValue;
      }
    }
    multiValueAttribute.value = selectedString;
  }
}

function recordCurrentConstraintsOrder() {
    previousConstraintsOrder = jQuery('#constraintList').sortable('serialize');
}

/**
 * Send the previous order and the new order to the server.
 */
function reorderConstraintsOnServer() {
    var newOrder = jQuery('#constraintList').sortable('serialize');
    AjaxServices.reorderConstraints(newOrder, previousConstraintsOrder);
    recordCurrentConstraintsOrder();
}

function initConstraints(index) {
  onChangeAttributeOps(index, true);
    if (document.getElementById("switchOff(" + index + ")")) {
        if (document.getElementById("switchOff(" + index + ")").value == "ON") {
            document.getElementById("optionalEnabled_" + index).style.display = "inline"
            document.getElementById("optionalDisabled_" + index).style.display = "none"
            if (document.templateForm["useBagConstraint(" + index + ")"]) {
                clickUseBag(index);
                document.templateForm["useBagConstraint(" + index + ")"].disabled = false;
            } else {
                disableFields(index, false);
            }
        } else {
            document.getElementById("optionalEnabled_" + index).style.display = "none"
            document.getElementById("optionalDisabled_" + index).style.display = "inline"
            disableFields(index, true);
        }
    } else {
      if (document.templateForm["useBagConstraint(" + index + ")"]) {
        clickUseBag(index);
      }
    }
}


function enableConstraint(index) {
    if (document.getElementById("switchOff(" + index + ")")) {
        document.getElementById("switchOff(" + index + ")").value = "ON";
        document.getElementById("optionalDisabled_" + index).style.display = "none"
        document.getElementById("optionalEnabled_" + index).style.display = "inline"
        disableFields(index, false);
        if (document.templateForm["useBagConstraint(" + index + ")"]) {
            clickUseBag(index);
            document.templateForm["useBagConstraint(" + index + ")"].disabled = false;
        }
    }
}

function disableConstraint(index) {
    if (document.getElementById("switchOff(" + index + ")")) {
        document.getElementById("switchOff(" + index + ")").value = "OFF";
        document.getElementById("optionalEnabled_" + index).style.display = "none"
        document.getElementById("optionalDisabled_" + index).style.display = "inline"
        disableFields(index, true);
    }
}

function disableFields(index, disable) {
    if (document.templateForm["attributeOps(" + index + ")"]) {
        document.templateForm["attributeOps(" + index + ")"].disabled = disable;
    }
    if (document.templateForm["attributeValues(" + index + ")"]) {
        document.templateForm["attributeValues(" + index + ")"].disabled = disable;
    }
    if (document.templateForm["multiValues(" + index + ")"]) {
        document.templateForm["multiValues(" + index + ")"].disabled = disable;
    }
    if (document.templateForm["attributeOptions(" + index + ")"]) {
        document.templateForm["attributeOptions(" + index + ")"].disabled = disable;
    }
    // if attributeValues is a radio button
    if (document.templateForm["attributeValues(" + index + ")"][0]) {
        document.templateForm["attributeValues(" + index + ")"][0].disabled = disable;
        document.templateForm["attributeValues(" + index + ")"][1].disabled = disable;
    }
    if (document.templateForm["extraValues(" + index + ")"]) {
        document.templateForm["extraValues(" + index + ")"].disabled = disable;
    }
    if (document.templateForm["useBagConstraint(" + index + ")"]) {
        document.templateForm["useBagConstraint(" + index + ")"].disabled = disable;
    }
    if (document.templateForm["bagOp(" + index + ")"] != undefined)
        document.templateForm["bagOp(" + index + ")"].disabled = disable;
    if (document.templateForm["bag(" + index + ")"] != undefined)
        document.templateForm["bag(" + index + ")"].disabled = disable;

    if (disable == true) {
        jQuery(".constraint_" + index).addClass("constraintHeadingDisabled");
    } else {
        jQuery(".constraint_" + index).removeClass("constraintHeadingDisabled");
    }
}

function onChangeAttributeOps(index, init) {

      //LIKE or NOT LIKE
      if(document.templateForm["attributeOps(" + index + ")"] != undefined && document.templateForm["attributeOps(" + index + ")"]){
      var constraintOpIndex = document.templateForm["attributeOps(" + index + ")"].value;
        if (constraintOpIndex == '6' || constraintOpIndex == '7' || constraintOpIndex == '18') {
            if (document.templateForm["attributeValues(" + index + ")"])
              document.templateForm["attributeValues(" + index + ")"].style.display = 'inline';
            if (document.templateForm["attributeOptions(" + index + ")"])
                  document.templateForm["attributeOptions(" + index + ")"].style.display = 'none';
            if (document.templateForm["multiValues(" + index + ")"])
                  document.templateForm["multiValues(" + index + ")"].style.display = 'none';
            if (document.templateForm["multiValueAttribute(" + index + ")"])
                  document.templateForm["multiValueAttribute(" + index + ")"].value = '';
          } // ONE OF or NONE OF
        else if (constraintOpIndex == '21' || constraintOpIndex == '22') {
          if (document.templateForm["multiValues(" + index + ")"])
              document.templateForm["multiValues(" + index + ")"].style.display = 'inline';
          if (document.templateForm["attributeValues(" + index + ")"])
              document.templateForm["attributeValues(" + index + ")"].style.display = 'none';
              if (document.templateForm["attributeOptions(" + index + ")"])
                  document.templateForm["attributeOptions(" + index + ")"].style.display = 'none';
          } else {
            if (document.templateForm["attributeOptions(" + index + ")"]) {
                document.templateForm["attributeOptions(" + index + ")"].style.display = 'inline';
              if (document.templateForm["attributeValues(" + index + ")"]) {
                if (document.templateForm["attributeValues(" + index + ")"].style != undefined)
                      document.templateForm["attributeValues(" + index + ")"].style.display = 'none';
                if (!init && document.templateForm["attributeOptions(" + index + ")"] != undefined)
                    document.templateForm["attributeValues(" + index + ")"].value = document.templateForm["attributeOptions("
                                                                                                      + index + ")"].options[0].value;
              }
            } else {
              if (document.templateForm["attributeValues(" + index + ")"]) {
                if (document.templateForm["attributeValues(" + index + ")"].style != undefined)
                      document.templateForm["attributeValues(" + index + ")"].style.display = 'inline';
                }
            }
              if (document.templateForm["multiValues(" + index + ")"])
                  document.templateForm["multiValues(" + index + ")"].style.display = 'none';
              if (document.templateForm["multiValueAttribute(" + index + ")"])
                  document.templateForm["multiValueAttribute(" + index + ")"].value = '';
          }
      }
}

function addAND(index) {
}

function addOR(index) {
}
