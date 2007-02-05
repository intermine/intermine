      // ***********************************************************
      // * Disables form fields based on user toggle
      // ***********************************************************
      
   function swapInputs(open) {
   
		var constraints = new Array("attribute","subclass","loop","bags","empty"); 
		for (var i = 0; i < constraints.length; i++)
		{     
			//alert(" *** " + constraints[i]);	
			if(document.getElementById(constraints[i] + 'Submit')) {
				if (constraints[i] == open) {
					document.getElementById(constraints[i] + 'Submit').disabled = false;
    		  		document.getElementById(constraints[i] + 'Toggle').src = 'images/disclosed.gif';
				} else {
		      		document.getElementById(constraints[i] + 'Submit').disabled = true;
    		  		document.getElementById(constraints[i] + 'Toggle').src = 'images/undisclosed.gif';
				}
			}
		}        
	}
		
