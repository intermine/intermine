<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- htmlHead.jsp -->

<tiles:importAttribute name="htmlPageTitle" ignore="true"/>
<tiles:importAttribute name="pageName" ignore="true"/>


<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/webapp.css'/>"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/inlineTagEditor.css'/>"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/datePicker.css'/>"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/datePickerLocal.css'/>"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/inlineTagEditor.css'/>"/>

<link href="${WEB_PROPERTIES['project.sitePrefix']}/rss.xml" rel="alternate" type="application/rss+xml" title="FlyMine | News" />

<%
/* In Safari, loading a css that doesnt exist causes weirdness */
String pageName = (String) request.getAttribute("pageName");
if(new java.io.File(application.getRealPath("css")+"/"+pageName+".css").exists()) {
        request.setAttribute("pageCSS","true");
}
%>
<c:if test="${pageCSS == 'true'}">
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/${pageName}.css'/>"/>
</c:if>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/theme/theme.css'/>"/>


<script type="text/javascript" src="<html:rewrite page='/js/jquery-1.2.6.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/jquery-ui-personalized-1.5.2.min.js'/>"></script>
<script type="text/javascript">
  jQuery.noConflict();
</script>

<script type="text/javascript" src="<html:rewrite page='/js/prototype.js'/>"></script>
<!-- <script type="text/javascript" src="<html:rewrite page='/js/scriptaculous.js'/>"></script> -->

<script type="text/javascript" src="<html:rewrite page='/dwr/interface/AjaxServices.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/dwr/engine.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/dwr/util.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/imdwr.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/imutils.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/niftycube.js'/>"></script>

<script type="text/javascript" src="<html:rewrite page='/js/date.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/jquery.datePicker.js'/>"></script>

<meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
<meta content="Integrated queryable database for Drosophila and Anopheles genomics"
      name="description"/>
<meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>

<title>
  <c:choose>
    <c:when test="${empty pageName}">
      <c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/>
    </c:when>
    <c:otherwise>
      <c:out value="${WEB_PROPERTIES['project.title']}: ${htmlPageTitle}" escapeXml="false"/>
    </c:otherwise>
  </c:choose>
</title>

<script type="text/javascript">
<!--
  function showContactForm()
  {
    document.getElementById('contactFormDiv').style.display='';
    document.getElementById('contactFormDivButton').style.display='none';
    window.scrollTo(0, 99999);
    document.getElementById("fbname").focus();
  }

	
	//must be there because of html:rewrite
	function refreshTags(uid, type) {
	    new Ajax.Updater('currentTags-'+uid, '<html:rewrite action="/inlineTagEditorChange"/>',
	        {parameters:'method=currentTags&uid='+uid+'&type='+type, asynchronous:true});
	}
	
  Position.center = function(element){
      var options = Object.extend({
            zIndex: 999,
            update: false
         }, arguments[1] || {});

      element = $(element)

      if(!element._centered){
          Element.setStyle(element, {position: 'absolute', zIndex: options.zIndex });
          element._centered = true;
      }

      var dims = Element.getDimensions(element);

      Position.prepare();
      var winSize = Position.getWindowSize();
      var winWidth = winSize.width;
      var winHeight = winSize.height;

      var offLeft = (Position.deltaX + Math.floor((winWidth-dims.width)/2));
      var offTop = (Position.deltaY + Math.floor((winHeight-dims.height)/2));
      element.style.top = ((offTop != null && offTop > 0) ? offTop : '0')+ 'px';
      element.style.left = ((offLeft != null && offLeft > 0) ? offLeft :'0') + 'px';

      if (options.update) {
          Event.observe(window, 'resize', function(evt) { Position.center(element); }, false);
          Event.observe(window, 'scroll', function(evt) { Position.center(element); }, false);
      }
  }

//-->
</script>
<!-- /htmlHead.jsp -->
