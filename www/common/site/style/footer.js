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
				footerElement.style.position = 'fixed';
				footerElement.style.bottom = 0;
				//footerElement.style.top = (windowHeight - footerHeight) + 'px';
				//footerElement.style.marginTop = '0';
            }
            else {
                footerElement.style.position = 'static';
			}
            _footerHeight=footerHeight;
		}
	}
}


function getDocHeight(doc) {
  var docHt = 0, sh, oh;
  if (doc.height) docHt = doc.height;
  else if (doc.body) {
    if (doc.body.scrollHeight) docHt = sh = doc.body.scrollHeight;
    if (doc.body.offsetHeight) docHt = oh = doc.body.offsetHeight;
    if (sh && oh) docHt = Math.max(sh, oh);
  }
  return docHt;
}

function setIframeHeight(iframeName) {
  var iframeWin = window.frames[iframeName];
  var iframeEl = document.getElementById? document.getElementById(iframeName): document.all? document.all[iframeName]: null;
  if ( iframeEl && iframeWin ) {
    iframeEl.style.height = "auto"; // helps resize (for some) if new doc shorter than previous  
    var docHt = getDocHeight(iframeWin.document);
    // need to add to height to be sure it will all show
    if (docHt) iframeEl.style.height = docHt+1 + "px";
  }
}

function readCookie(name)
{
  var nameEQ = name + "=";
  var ca = document.cookie.split(';');
  for(var i=0;i < ca.length;i++)
  {
    var c = ca[i];
    while (c.charAt(0)==' ') c = c.substring(1,c.length);
    if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
  }
  return null;
}

function linkTo(url, label)
{
  return '<a href="'+url+'">'+label+'</a>';
}

/*
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
}*/

