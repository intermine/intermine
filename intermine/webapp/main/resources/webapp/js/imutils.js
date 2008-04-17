// from http://www.quirksmode.org/blog/archives/2005/10/_and_the_winner_1.html

function addEvent( obj, type, fn )
{
	if (obj.addEventListener)
		obj.addEventListener( type, fn, false );
	else if (obj.attachEvent)
	{
		obj["e"+type+fn] = fn;
		obj[type+fn] = function() { obj["e"+type+fn]( window.event ); }
		obj.attachEvent( "on"+type, obj[type+fn] );
	}
}

function removeEvent( obj, type, fn )
{
	if (obj.removeEventListener)
		obj.removeEventListener( type, fn, false );
	else if (obj.detachEvent)
	{
		obj.detachEvent( "on"+type, obj[type+fn] );
		obj[type+fn] = null;
		obj["e"+type+fn] = null;
	}
}

function toggleDivs(source,destination){
   document.getElementById(source).style.display = 'none';
   document.getElementById(destination).style.display = 'block';
}

function disableEnterKey(e)
{
    var keyCode;

    if(window.event) {
        keyCode = window.event.keyCode;     //IE
    } else {
        keyCode = e.which;     //firefox

    }

    return (keyCode != 13);
}

// Redefined in objectDetails.jsp page
function toggleHidden(elementId) {
    var element = document.getElementById(elementId);
    var display = element.style.display;
     if(display=='none') {
		toggleOpen(element, elementId);
     } else {
		toggleClose(element, elementId);
     }
}

function toggleClose(element, elementId) {
	element.style.display = 'none';
    document.getElementById(elementId + 'Toggle').src = 'images/undisclosed.gif';
}

function toggleOpen(element, elementId) {
	element.style.display = 'block';
	document.getElementById(elementId + 'Toggle').src = 'images/disclosed.gif';
}

function toggleAll(count, prefix, display, extraField, saveState) {
	for (i = 0; i < count; i++) {
		var elementId = prefix + i;
		var element = document.getElementById(elementId);
		if (element != null) {    		
			if(display=='expand') {
				toggleOpen(element, elementId);
			} else {
				toggleClose(element, elementId);
			}
     	}
     	if (saveState == true && element != null && window.saveToggleState /* function exists*/) {
     		saveToggleState(elementId);
     	}
     }
     if (extraField != null  && (element = document.getElementById(extraField)) != null) {
      		if(display=='expand') {
				toggleOpen(element, extraField);
			} else {
				toggleClose(element, extraField);
			}		
			if (window.saveToggleState /* function exists*/) {
				saveToggleState(extraField);
			}
     }
}

function swapStyles(elementId,style1,style2,checkbox){
	if(document.getElementById(checkbox)!=null && !document.getElementById(checkbox).checked){
  	  if(document.getElementById(elementId).className == style1) {
		  document.getElementById(elementId).className = style2;
	  } else {
		  document.getElementById(elementId).className = style1;
	  }
    }
}

/* Show or hide element with specified id */
function display(id, beDisplayed) {
    if (beDisplayed == true) {
        document.getElementById(id).style.display = 'block';
    } else {
        document.getElementById(id).style.display = 'none'; 
    }
}

/* Center element with specified id to the center of window */
function center(id) {
    var el = document.getElementById(id);
    var elWidth = el.offsetWidth;
    var elHeight = el.offsetHeight;
    var x,y;
    if (navigator.appName.indexOf('Microsoft')!=-1) {
        x = (document.body.clientWidth - elWidth) / 2 ;
        y = (document.body.clientHeight - elHeight) / 2;
    }else{
        x = (window.innerWidth - elWidth) / 2;
        y = (window.innerHeight - elHeight) / 2;
    }
    /* I prefer center a bit above real center */
    y = y - 50;
    el.style.position = 'absolute';
    el.style.left = x + 'px';  
    el.style.top = y + 'px';
}

/* Open pop up window with specified url, width and height. */
function openPopWindow(url, width, height) {
    var newWin = "";
    var popFeatures = "width=" + width + ",height=" + height + ",toolbar=0,location=0,directories=0,status=0,menuBar=0,scrollBars=1,resizable=1";
    newWin = window.open(url,'newWin',popFeatures);
    newWin.focus();
}
