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

function toggleHidden(elementId) {
    var element = document.getElementById(elementId);
    var display = element.style.display;
     if(display=='none') {
		element.style.display = 'block';
         document.getElementById(elementId + 'Toggle').src = 'images/disclosed.gif';
     } else {
		element.style.display = 'none';
        document.getElementById(elementId + 'Toggle').src = 'images/undisclosed.gif';
     }
}

function toggleAll(count, prefix, display, extraField) {
	for (i = 0; i < count; i++) {
		var elementId = prefix + i;
		if (document.getElementById(elementId) != null) {    		
	    	toggleHidden(elementId);
     	}
     }
     if (extraField != null) {
		toggleHidden(extraField);
     }
}


