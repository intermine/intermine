// http://www.alistapart.com/articles/footers/

var _footerHeight=-1;

function getWindowHeight() {
    var windowHeight = 0;
    if (typeof(window.innerHeight) == 'number') {
	    windowHeight = window.innerHeight;
    }
    else {
        if (document.documentElement && document.documentElement.clientHeight) {
            windowHeight = document.documentElement.clientHeight;
        }
	    else {
            if (document.body && document.body.clientHeight) {
			    windowHeight = document.body.clientHeight;
		    }
	    }
    }
    return windowHeight;
}

function setFooter() {
    if (document.getElementById) {
        var windowHeight = getWindowHeight();
		if (windowHeight > 0) {
			var headerHeight = document.getElementById('header').offsetHeight;
			var contentHeight = document.getElementById('pagecontent').offsetHeight;
			var footerElement = document.getElementById('footer');
			var footerHeight  = footerElement.offsetHeight;
			if (windowHeight - (headerHeight + contentHeight + footerHeight) >= 0) {
				footerElement.style.position = 'absolute';
				footerElement.style.top = (windowHeight - footerHeight) + 'px';
				footerElement.style.marginTop = '0';
            }
            else {
                footerElement.style.position = 'static';
			}
            _footerHeight=footerHeight;
		}
	}
}

window.onload = function() {
	setFooter();
}

window.onresize = function() {
	setFooter();
}

// Change of font size
window.onmouseover = function() {
    if (document.getElementById) {
        if (_footerHeight!=document.getElementById('footer').offsetHeight) {
            // Not needed for Opera
            if (navigator.userAgent.indexOf('Opera') == -1)
                setFooter();
            else
                _footerHeight=document.getElementById('footer').offsetHeight;
        }
    }
}

