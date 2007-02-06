      // ***********************************************************
      // * Disables form fields based on user toggle
      // ***********************************************************
      
   function swapInputs(open) {
   
   		// different constraints available to the user
		var constraints = new Array("attribute","subclass","loopQuery","bag","empty"); 
		// field names, different fields will be visible for different constraints
		const maxVariableCount = 7; // there are seven attribute variables
		
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
		
		
	function enableField(constraint) {
	
		switch (constraint) {
		case "attribute":			
			document.getElementById('attributeValue').disabled = false;						

			if (document.getElementById('attributeOp')) {
				document.getElementById('attributeOp').disabled = false;						
			}
			if (document.getElementById('attributeOptions')) {
				document.getElementById('attributeOptions').disabled = false;						
			}
		break;
		case "subclass":
			document.getElementById('subclassValue').disabled = false;
		break;		
		case "loop":
			document.getElementById('loopQueryOp').disabled = false;
			document.getElementById('loopQueryValue').disabled = false;
		break;		
		case "bags":
			document.getElementById('bagOp').disabled = false;
			document.getElementById('bagValue').disabled = false;
		break;
		case "empty":
			document.getElementById('nullConstraint').disabled = false;		
		break;
		}
	}
	
	function disableField(constraint) {
	
		switch (constraint) {
		case "attribute":
			document.getElementById('attributeValue').disabled = true;						

			if (document.getElementById('attributeOp')) {
				document.getElementById('attributeOp').disabled = true;						
			}
			if (document.getElementById('attributeOptions')) {
				document.getElementById('attributeOptions').disabled = true;						
			}
		break;
		case "subclass":
			document.getElementById('subclassValue').disabled = true;
		break;		
		case "loop":
			document.getElementById('loopQueryOp').disabled = true;
			document.getElementById('loopQueryValue').disabled = true;
		break;		
		case "bags":
			document.getElementById('bagOp').disabled = true;
			document.getElementById('bagValue').disabled = true;
		break;
		case "empty":
			document.getElementById('nullConstraint').disabled = true;		
		break;
		}
	}
	
