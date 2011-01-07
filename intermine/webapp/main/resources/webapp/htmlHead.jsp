<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- htmlHead.jsp -->

<tiles:importAttribute name="htmlPageTitle" ignore="true"/>
<tiles:importAttribute name="pageName" ignore="true"/>

<link href="${WEB_PROPERTIES['project.rss']}" rel="alternate" type="application/rss+xml" title="${WEB_PROPERTIES['project.title']} | News" />

<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/webapp.css'/>"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/inlineTagEditor.css'/>"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/resultstables.css'/>" />

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

<c:set var="theme" value="${WEB_PROPERTIES['theme']}"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/themes/${theme}/theme.css'/>"/>

<c:choose>
  <c:when test="${pageName == 'spanUploadOptions' || pageName == 'begin'}">
    <script type="text/javascript" src="<html:rewrite page='/js/jquery-1.4.2.min.js'/>"></script>
  </c:when>
  <c:otherwise>
    <script type="text/javascript" src="<html:rewrite page='/js/jquery-1.3.2.min.js'/>"></script>
  </c:otherwise>
</c:choose>

<c:if test="${pageName != 'begin'}">
	<c:if test="${pageName == 'results' || pageName == 'bagDetails' || pageName == 'objectDetails'}">
	  <script type="text/javascript" src="<html:rewrite page='/js/jquery.qtip-1.0.0-rc3.min.js'/>"></script>
	</c:if>
<!--
	<c:if test="${pageName == 'begin'}">
	  <script type="text/javascript" src="<html:rewrite page='/js/jQuery.roundCorners-1.1.1.js'/>"></script>
	  <script type="text/javascript" src="<html:rewrite page='/js/excanvas.js'/>"></script>
	</c:if>
-->
	<c:if test="${pageName == 'results' || pageName == 'query' || pageName == 'templates' || pageName == 'bagDetails' || pageName == 'objectDetails' || pageName == 'bag' || pageName == 'mymine'}">
	  <script type="text/javascript" src="<html:rewrite page='/js/jquery.boxy.js'/>"></script>
	  <link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/boxy.css'/>"/>
	  <script type="text/javascript" src="<html:rewrite page='/js/jquery.dimensions.min.js'/>"></script>
	  <script type="text/javascript" src="<html:rewrite page='/js/jquery.center.js'/>"></script>
	  <c:if test="${pageName == 'bagDetails'}">
	  	<script type="text/javascript" src="<html:rewrite page='/js/textarea-resize.js'/>"></script>
	  </c:if>
	</c:if>
	
	<c:if test="${pageName == 'query' || pageName == 'exportOptions'}">
	  <link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/jquery-ui-1.7.2.custom.css'/>"/>
	  <script type="text/javascript" src="<html:rewrite page='/js/jquery-ui-1.7.2.custom.min.js'/>"></script>
	</c:if>
	
	<script type="text/javascript">
	  jQuery.noConflict();
	</script>
	
	<%-- this has to live after jQuery.  do not move --%>
	<script type="text/javascript" src="<html:rewrite page='/js/prototype.js'/>"></script>
</c:if>	
	<script type="text/javascript" src="<html:rewrite page='/dwr/interface/AjaxServices.js'/>"></script>
	<script type="text/javascript" src="<html:rewrite page='/dwr/engine.js'/>"></script>
	<script type="text/javascript" src="<html:rewrite page='/dwr/util.js'/>"></script>
	<script type="text/javascript" src="<html:rewrite page='/js/imdwr.js'/>"></script>
	<script type="text/javascript" src="<html:rewrite page='/js/imutils.js'/>"></script>
	<script type="text/javascript" src="<html:rewrite page='/js/inlineTagEditor.js'/>"></script>
	<script type="text/javascript" src="<html:rewrite page='/js/date.js'/>"></script>
	<script type="text/javascript" src="<html:rewrite page='/js/tagSelect.js'/>"></script>
	<script type="text/javascript" src="<html:rewrite page='/js/table.js'/>"></script>
	<!--[if lt IE 7.]>
	  <script defer type="text/javascript" src="pngfix.js"></script>
	<![endif]-->

<meta content="${WEB_PROPERTIES['meta.keywords']}" name="keywords"/>
<meta content="${WEB_PROPERTIES['meta.description']}" name="description"/>
<meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>

<!-- print stylesheet -->
<c:if test="${pageName == 'bagDetails' || pageName == 'results'}">
	<link rel="stylesheet" href="<html:rewrite page='/css/print.css'/>" type="text/css" media="print" />
</c:if>

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
<!-- /htmlHead.jsp -->
