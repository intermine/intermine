function updateGoStatsForm() {
	// form may not exist if we don't have a gene bag
	if (eval(document.goStatForm)) {
		// ontology form field
		var goStatsObj = document.goStatForm.ontology;
		
		// loop through and select the option that's in the URL
		// if there is nothing in the URL that means this is the first time the user's seen 
		// this page, so the default will stay at the first option - biological process
		for (var i = 0; i < goStatsObj.length; ++i) {	
			if (goStatsObj[i].value == getParam("ontology")) { 
				goStatsObj.selectedIndex = i;
				setDefault = false;
			}
		}
	}
}
   
window.onload = function() {	
    updateGoStatsForm();
}

function getParam(name)
{
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var tmpURL = window.location.href;
  var results = regex.exec( tmpURL );
  if( results == null )
    return "";
  else
    return results[1];
}