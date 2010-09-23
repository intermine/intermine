function initConstraint(selectedConstraint) {
      if(selectedConstraint != null) {
          if(selectedConstraint.value == "bag")
              document.getElementById("checkBoxBag").checked = true;
          swapInputs(selectedConstraint.value);
      }
  }

  function onChangeAttributeOp() {
        //attribute5 operator
        //attribute8 attribute value
        //attribute7 options
        //LIKE or NOT LIKE or LOOKUP
        if(document.getElementById("multiValueAttribute"))
            document.getElementById("multiValueAttribute").value = "";
        if (document.getElementById("attribute5")) {
            var constraintOpIndex = document.getElementById("attribute5").value;
            if (constraintOpIndex == '6' || constraintOpIndex == '7' || constraintOpIndex == '18') {
                document.getElementById("attribute8").style.display = 'inline';
                document.getElementById("attribute8").value = '';
                if (document.getElementById("attribute7"))
                    document.getElementById("attribute7").style.display = 'none';
                if (document.getElementById("multiValue"))
                    document.getElementById("multiValue").style.display = 'none';
              } // IN or NOT IN
            else if (constraintOpIndex == '21' || constraintOpIndex == '22') {
                if (document.getElementById("multiValue"))
                    document.getElementById("multiValue").style.display = 'inline';
                document.getElementById("multiValue").selectedIndex='-1';
                if(document.getElementById("attribute8"))
                    document.getElementById("attribute8").style.display = 'none';
                if (document.getElementById("attribute7"))
                      document.getElementById("attribute7").style.display = 'none';
              } else {
                if (document.getElementById("attribute7")) {
                    document.getElementById("attribute7").style.display = 'inline';
                    if (document.getElementById("attribute8").style != undefined)
                        document.getElementById("attribute8").style.display = 'none';
                    if (document.getElementById("attribute7") != undefined)
                        document.getElementById("attribute8").value = document.getElementById("attribute7").options[0].value;
                } else {
                    if (document.getElementById("attribute8").style != undefined)
                        document.getElementById("attribute8").style.display = 'inline';
                }
                 if (document.getElementById("multiValue"))
                   document.getElementById("multiValue").style.display = 'none';
              }
        }
    }

  function updateMValueAttribute() {
        var multiValueAttribute = document.getElementById("multiValueAttribute");
        var selectedString = '';
        var multiValuesOptions = document.getElementById("multiValue");
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
            document.getElementById("multiValueAttribute").value = selectedString;
        }
    }
// ***********************************************************
// * Disables form fields based on user toggle
// ***********************************************************

function swapInputs(open) {
    // different constraints available to the user
    var constraints = new Array("attribute","subclass","loopQuery","bag","empty");
    // field names, different fields will be visible for different constraints
    for (var i = 0; i < constraints.length; i++) {
       // enable if this is what the user just selected
        if(open != "bag" || document.getElementById("checkBoxBag").checked) {
            if (constraints[i] == open) {
                if (document.getElementById(constraints[i] + 'Submit') != null) {
                    document.getElementById(constraints[i] + 'Submit').disabled = false;
                }
                // loop through other fields that may exist
                for (var k = 1; k < 10; k++) {
                    if(document.getElementById(constraints[i] + k) != null) {
                        document.getElementById(constraints[i] + k).disabled = false;
                    }
                }
            // disable everything else
            } else {
                if (document.getElementById(constraints[i] + 'Submit') != null) {
                    document.getElementById(constraints[i] + 'Submit').disabled = true;
                }
                for (var k = 1; k < 10; k++) {
                    if(document.getElementById(constraints[i] + k) != null) {
                        document.getElementById(constraints[i] + k).disabled = true;
                    }
                }
            }
        }
        else { //open == bag and checkBoxBag not checked
            document.getElementById('attributeSubmit').name = "attribute";
            return swapInputs('attribute');
        }
    }
    if(open == "bag") {
        //we use the same button for attribute/lookup and bag
        if(document.getElementById('attributeSubmit') != null) {
         document.getElementById('attributeSubmit').name = "bag";
         document.getElementById('attributeSubmit').disabled = false;
        }
    } else {
        if(document.getElementById("checkBoxBag") != null) {
            document.getElementById("checkBoxBag").checked = false;
            document.getElementById("checkBoxBag").disabled = false;
        }
    }

    // If we've got a loop query, disable the join type selection
    if(open == 'loopQuery') {
        jQuery('#inner').attr("checked", true);
        //jQuery('#useJoin').attr("disabled", true);
        jQuery('#outer').attr("disabled", true);
        jQuery('#inner').attr("disabled", true);
        jQuery('#joinStyleSubmit').attr("disabled", true);
    } else {
        jQuery('#outer').attr("disabled", false);
        jQuery('#inner').attr("disabled", false);
        jQuery('#joinStyleSubmit').attr("disabled", false);
    }
    if (document.getElementById("extraValue1")) {
        if(open == 'attribute') {
        document.getElementById("extraValue1").disabled = false;
        } else {
        document.getElementById("extraValue1").disabled = true;
        }
    }
}

function toggleConstraintDetails(isEditable) {
    if (isEditable) {
        document.getElementById("editableConstraintOptions").setAttribute("class", "");
        document.getElementById('templateLabel').disabled = false;
        radios = document.getElementsByName('switchable');
        for (i = 0; i < radios.length; i++) {
            radios[i].disabled = false;
        }
    } else {
        document.getElementById("editableConstraintOptions").setAttribute("class", "constraintEditableOptionsDisabled");
        document.getElementById('templateLabel').disabled = true;;
        radios = document.getElementsByName('switchable');
        for (i = 0; i < radios.length; i++) {
            radios[i].disabled = true;
        }
    }
}

