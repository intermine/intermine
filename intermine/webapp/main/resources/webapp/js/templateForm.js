/*******************************************************************************
 * Use bag checkbox has been clicked.
 ******************************************************************************/
function clickUseBag(index) {
  var useBag = document.getElementById("useBagConstraint("+index+")").checked;
  if (document.getElementById("attributeOps("+index+")") && document.getElementById("attributeOps("+index+")") != undefined)
      document.getElementById("attributeOps("+index+")").disabled=useBag;
  if (document.getElementById("attributeOptions("+index+")")) {
    document.getElementById("attributeOptions("+index+")").disabled=useBag;
  }

  if (document.getElementById("attributeValues("+index+")") && document.getElementById("attributeValues("+index+")") != undefined) {
    var attributeValuesElements = document.getElementsByName("attributeValues("+index+")");
     //if attributeValues is a radio button
     if(attributeValuesElements[0].type == 'radio') {
        attributeValuesElements[0].disabled=useBag;
        attributeValuesElements[1].disabled=useBag;
     } else {
         document.getElementById("attributeValues("+index+")").disabled=useBag;
     }
  }
  if (document.getElementById("multiValues("+index+")")) {
    document.getElementById("multiValues("+index+")").disabled=useBag;
  }
  if (document.getElementById("extraValues("+index+")")) {
    document.getElementById("extraValues("+index+")").disabled=useBag;
  }

  document.getElementById("bag("+index+")").disabled=!useBag;
  document.getElementById("bagOp("+index+")").disabled=!useBag;
 
  if( document.getElementById("attributeId_"+index) ){
    document.getElementById("attributeId_"+index).disabled=useBag;
  }
  if(useBag){
    document.getElementsByClassName('constraint_'+index)[1].style.color = "#aaa";
    document.getElementsByClassName('constraint_'+index)[2].style.color = "#000";
  }
  else {
    document.getElementsByClassName('constraint_'+index)[1].style.color = "#000";
    document.getElementsByClassName('constraint_'+index)[2].style.color = "#aaa";
  }
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
  jQuery('#templateForm').submit();
}

function exportTemplate() {
    document.getElementById('actionType').value = 'exportTemplate';
    jQuery('#templateForm').submit();
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
    if (document.getElementById("useBagConstraint("+i+")")) {
      if (document.getElementById("useBagConstraint("+i+")").checked) {
        return true;
      }
    }
  }
  return false;
}

function updateAttributeValues(index) {
  var attributeValues = document.getElementById('attributeValues('+index+')');
  var selectedString = '';
  var attributeOptions = document.getElementById('attributeOptions('+index+')');
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
  var multiValueAttribute = document.getElementById('multiValueAttribute('+index+')');
  var selectedString = '';
  var multiValuesOptions = document.getElementById('multiValues('+index+')');
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
            if (document.getElementById("useBagConstraint(" + index + ")")) {
                clickUseBag(index);
                document.getElementById("useBagConstraint(" + index + ")").disabled = false;
            } else {
                disableFields(index, false);
            }
        } else {
            document.getElementById("optionalEnabled_" + index).style.display = "none"
            document.getElementById("optionalDisabled_" + index).style.display = "inline"
            disableFields(index, true);
        }
    } else {
      if (document.getElementById("useBagConstraint(" + index + ")")) {
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
        if (document.getElementById("useBagConstraint(" + index + ")")) {
            clickUseBag(index);
            document.getElementById("useBagConstraint(" + index + ")").disabled = false;
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
    if (document.getElementById("attributeOps(" + index + ")")) {
        document.getElementById("attributeOps(" + index + ")").disabled = disable;
    }
    if (document.getElementById("attributeValues(" + index + ")")) {
        document.getElementById("attributeValues(" + index + ")").disabled = disable;
    }
    if (document.getElementById("multiValues(" + index + ")")) {
        document.getElementById("multiValues(" + index + ")").disabled = disable;
    }
    if (document.getElementById("attributeOptions(" + index + ")")) {
        document.getElementById("attributeOptions(" + index + ")").disabled = disable;
    }
    // if attributeValues is a radio button
    if (document.getElementById("attributeValues(" + index + ")")) {
        if (document.getElementById("attributeValues(" + index + ")")[0]) {
            document.getElementById("attributeValues(" + index + ")")[0].disabled = disable;
            document.getElementById("attributeValues(" + index + ")")[1].disabled = disable;
        }
    }
    if (document.getElementById("extraValues(" + index + ")")) {
        document.getElementById("extraValues(" + index + ")").disabled = disable;
    }
    if (document.getElementById("useBagConstraint(" + index + ")")) {
        document.getElementById("useBagConstraint(" + index + ")").disabled = disable;
    }
    if (document.getElementById("nullConstraint(" + index + ")")) {
        document.getElementById("nullConstraint(" + index + ")").disabled = disable;
    }
    if (document.getElementById("bagOp(" + index + ")") != undefined)
        document.getElementById("bagOp(" + index + ")").disabled = disable;
    if (document.getElementById("bag(" + index + ")") != undefined)
        document.getElementById("bag(" + index + ")").disabled = disable;

    if (disable == true) {
        jQuery(".constraint_" + index).addClass("constraintHeadingDisabled");
    } else {
        jQuery(".constraint_" + index).removeClass("constraintHeadingDisabled");
    }
}

function onChangeAttributeOps(index, init) {

      //LIKE or NOT LIKE - CONTAINS  - LOOKUP
      if(document.getElementById("attributeOps(" + index + ")") != undefined && document.getElementById("attributeOps(" + index + ")")){
      var constraintOpIndex = document.getElementById("attributeOps(" + index + ")").value;
        if (constraintOpIndex == '8' || constraintOpIndex == '9' ||
            constraintOpIndex == '12' || constraintOpIndex == '20') {
            if (document.getElementById("attributeValues(" + index + ")"))
              document.getElementById("attributeValues(" + index + ")").style.display = 'inline';
            if (document.getElementById("attributeOptions(" + index + ")"))
                  document.getElementById("attributeOptions(" + index + ")").style.display = 'none';
            if (document.getElementById("multiValues(" + index + ")"))
                  document.getElementById("multiValues(" + index + ")").style.display = 'none';
            if (document.getElementById("multiValueAttribute(" + index + ")"))
                  document.getElementById("multiValueAttribute(" + index + ")").value = '';
          } // ONE OF or NONE OF
        else if (constraintOpIndex == '23' || constraintOpIndex == '24') {
          if (document.getElementById("multiValues(" + index + ")"))
              document.getElementById("multiValues(" + index + ")").style.display = 'inline';
          if (document.getElementById("attributeValues(" + index + ")"))
              document.getElementById("attributeValues(" + index + ")").style.display = 'none';
              if (document.getElementById("attributeOptions(" + index + ")"))
                  document.getElementById("attributeOptions(" + index + ")").style.display = 'none';
          } else {
            if (document.getElementById("attributeOptions(" + index + ")")) {
                document.getElementById("attributeOptions(" + index + ")").style.display = 'inline';
              if (document.getElementById("attributeValues(" + index + ")")) {
                if (document.getElementById("attributeValues(" + index + ")").style != undefined)
                      document.getElementById("attributeValues(" + index + ")").style.display = 'none';
                if (!init && document.getElementById("attributeOptions(" + index + ")") != undefined)
                    document.getElementById("attributeValues(" + index + ")").value = document.getElementById("attributeOptions("
                                                                                                      + index + ")").options[0].value;
              }
            } else {
              if (document.getElementById("attributeValues(" + index + ")")) {
                if (document.getElementById("attributeValues(" + index + ")").style != undefined)
                      document.getElementById("attributeValues(" + index + ")").style.display = 'inline';
                }
            }
              if (document.getElementById("multiValues(" + index + ")"))
                  document.getElementById("multiValues(" + index + ")").style.display = 'none';
              if (document.getElementById("multiValueAttribute(" + index + ")"))
                  document.getElementById("multiValueAttribute(" + index + ")").value = '';
          }
      }
}

function addAND(index) {
}

function addOR(index) {
}
