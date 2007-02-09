// ***********************************************************
// * Disables form fields based on user toggle
// ***********************************************************

function swapInputs(open) {

    // different constraints available to the user
    var constraints = new Array("attribute","subclass","loopQuery","bag","empty");
    // field names, different fields will be visible for different constraints
    var maxVariableCount = 7; // there are seven attribute variables

    for (var i = 0; i < constraints.length; i++) {

        // if this constraint exists, it may not
        if(document.getElementById(constraints[i] + 'Submit')) {

            // enable if this is what the user just selected
            if (constraints[i] == open) {
                document.getElementById(constraints[i] + 'Submit').disabled = false;
                document.getElementById(constraints[i] + 'Toggle').src = 'images/disclosed.gif';

                // loop through other fields that may exist
                for (var j = 0; j < maxVariableCount; j++) {
                    if(document.getElementById(constraints[i] + j)) {
                        document.getElementById(constraints[i] + j).disabled = false;
                    }
                }

                // disable everything else
            } else {
                document.getElementById(constraints[i] + 'Submit').disabled = true;
                document.getElementById(constraints[i] + 'Toggle').src = 'images/undisclosed.gif';

                // loop through other fields that may exist
                for (var k = 0; k < maxVariableCount; k++) {
                    if(document.getElementById(constraints[i] + k)) {
                        document.getElementById(constraints[i] + k).disabled = true;
                    }
                }
            }
        }
    }
}
