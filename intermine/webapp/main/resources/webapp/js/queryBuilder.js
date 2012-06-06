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
        //LIKE or NOT LIKE or CONTAINS or LOOKUP
        var multiValueAttribute = jQuery('#multiValueAttribute');
        if (multiValueAttribute.exists()) {
            multiValueAttribute.val('');
        }

        var attribute5 = jQuery('#attribute5');
        if (attribute5.exists()) {
            var constraintOpIndex = parseInt(attribute5.val());
            console.log(constraintOpIndex);
            switch (constraintOpIndex) {
                case 8:
                case 9:
                case 12:
                case 20:
                    var attribute8 = jQuery('#attribute8');
                    if (attribute8.exists()) attribute8.css('display', 'inline').val('');
                    
                    var attribute7 = jQuery('#attribute7');
                    if (attribute7.exists()) attribute7.hide();

                    var multiValue = jQuery('#multiValue');
                    if (multiValue.exists()) multiValue.hide();
                    
                    break;
                case 23:
                case 24:
                    var attribute7 = jQuery('#attribute7');
                    if (attribute7.exists()) attribute7.hide();

                    var attribute8 = jQuery('#attribute8');
                    if (attribute8.exists()) attribute8.hide();

                    var multiValue = jQuery('#multiValue');
                    if (multiValue.exists()) {
                        multiValue.css('display', 'inline');
                        multiValue.attr('selectedIndex', -1);
                    }

                    break;
                default:
                    var attribute7 = jQuery('#attribute7');
                    if (attribute7.exists()) {
                        attribute7.css('display', 'inline');

                        var attribute8 = jQuery('#attribute8');
                        if (attribute8.exists()) {
                            attribute8.hide();
                            attribute8.val(attribute7.find('option').first().text());
                        }
                    } else {
                        var attribute8 = jQuery('#attribute8');
                        if (attribute8.exists()) attribute8.css('display', 'inline');
                    }

                    var multiValue = jQuery('#multiValue');
                    if (multiValue.exists()) multiValue.hide();
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

