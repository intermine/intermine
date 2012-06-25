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
if(new java.io.File(application.getRealPath("js")+"/"+pageName+".js").exists()) {
    request.setAttribute("pageJS","true");
}
%>
<c:if test="${pageCSS == 'true'}">
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/${pageName}.css'/>"/>
</c:if>

<c:set var="theme" value="${WEB_PROPERTIES['theme']}"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/themes/${theme}/theme.css'/>"/>

<script type="text/javascript" src="<html:rewrite page='/js/jquery-1.7.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/intermine.js'/>"></script>

<c:if test="${pageName != 'begin'}">
  <c:if test="${pageName == 'results' || pageName == 'bagDetails' || pageName == 'report'}">
    <script type="text/javascript" src="<html:rewrite page='/js/jquery.qtip-1.0.0-rc3.min.js'/>"></script>
    <script type="text/javascript" src="<html:rewrite page='/js/raphael.js'/>"></script>
    <script type="text/javascript" src="<html:rewrite page='/js/jsphylosvg.js'/>"></script>

    <!-- Ideally these imports should live in resultsTable.jsp - but I cannot get them to only import once -->
    <c:set var="jsLib" value="${WEB_PROPERTIES['ws.imtables.provider']}"/>
    <link type="text/css" rel="stylesheet" href="${jsLib}/css/bootstrap.css"></link>
    <link type="text/css" rel="stylesheet" href="${jsLib}/lib/css/flick/jquery-ui-1.8.19.custom.css"></link>
    <link type="text/css" rel="stylesheet" href="${jsLib}/lib/google-code-prettify/prettify.css"></link>
    <link type="text/css" rel="stylesheet" href="${jsLib}/css/tables.css"></link>
    <link type="text/css" rel="stylesheet" href="${jsLib}/css/flymine.css"></link>

    <script src="${jsLib}/lib/underscore-min.js"></script>
    <script src="${jsLib}/lib/backbone.js"></script>
    <script src="js/im.js"></script>
    <script src="${jsLib}/js/deps.js"></script>
    <script src="${jsLib}/js/imtables.js"></script>
  <c:if test="${WEB_PROPERTIES['jbrowse'] == 'true'}">
    <!--
    <link rel="stylesheet" type="text/css" href="/jbrowse/jslib/dijit/themes/tundra/tundra.css"></link>
        <link rel="stylesheet" type="text/css" href="/jbrowse/jslib/dojo/resources/dojo.css"></link>
        <link rel="stylesheet" type="text/css" href="/jbrowse/genome.css"></link>

        <script type="text/javascript" src="/jbrowse/jslib/dojo/dojo.js" djConfig="isDebug: false"></script>
        <script type="text/javascript" src="/jbrowse/jslib/dojo/jbrowse_dojo.js" ></script>

    <script type="text/javascript" src="/jbrowse/js/Browser.js"></script>
        <script type="text/javascript" src="/jbrowse/js/Util.js"></script>
        <script type="text/javascript" src="/jbrowse/js/NCList.js"></script>
        <script type="text/javascript" src="/jbrowse/js/LazyPatricia.js"></script>
        <script type="text/javascript" src="/jbrowse/js/LazyArray.js"></script>
        <script type="text/javascript" src="/jbrowse/js/Track.js"></script>
        <script type="text/javascript" src="/jbrowse/js/SequenceTrack.js"></script>
        <script type="text/javascript" src="/jbrowse/js/Layout.js"></script>
        <script type="text/javascript" src="/jbrowse/js/FeatureTrack.js"></script>
        <script type="text/javascript" src="/jbrowse/js/UITracks.js"></script>
        <script type="text/javascript" src="/jbrowse/js/ImageTrack.js"></script>
        <script type="text/javascript" src="/jbrowse/js/GenomeView.js"></script>
        <script type="text/javascript" src="/jbrowse/data/refSeqs.js"></script>
        <script type="text/javascript" src="/jbrowse/data/trackInfo.js"></script>
    -->
  </c:if>
  </c:if>

<!--
  <c:if test="${pageName == 'begin'}">
    <script type="text/javascript" src="<html:rewrite page='/js/jQuery.roundCorners-1.1.1.js'/>"></script>
    <script type="text/javascript" src="<html:rewrite page='/js/excanvas.js'/>"></script>
  </c:if>
-->

  <c:if test="${pageName == 'results' || pageName == 'query' || pageName == 'templates' || pageName == 'bagDetails' || pageName == 'bag' || pageName == 'mymine'}">
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
  <c:if test="${pageName != 'report' && pageName != 'mymine' && pageName != 'bagDetails' && pageName != 'results'}">
   <script type="text/javascript" src="<html:rewrite page='/js/prototype.js'/>"></script>
  </c:if>
</c:if>
  <script type="text/javascript" src="<html:rewrite page='/dwr/interface/AjaxServices.js'/>"></script>
  <script type="text/javascript" src="<html:rewrite page='/dwr/interface/TrackAjaxServices.js'/>"></script>
  <script type="text/javascript" src="<html:rewrite page='/dwr/engine.js'/>"></script>
  <script type="text/javascript" src="<html:rewrite page='/dwr/util.js'/>"></script>
  <script type="text/javascript" src="<html:rewrite page='/js/imdwr.js'/>"></script>
  <script type="text/javascript" src="<html:rewrite page='/js/imutils.js'/>"></script>
  <script type="text/javascript" src="<html:rewrite page='/js/jquery-syntax/jquery.syntax.js'/>"></script>
  <link rel="stylesheet" type="text/css" href="<html:rewrite page='/js/jquery-syntax/jquery.syntax.layout.list.css'/>">
  <link rel="stylesheet" type="text/css" href="<html:rewrite page='/js/jquery-syntax/jquery.syntax.core.css'/>">

  <script type="text/javascript" src="<html:rewrite page='/js/inlineTagEditor.js'/>"></script>

  <script type="text/javascript" src="<html:rewrite page='/js/date.js'/>"></script>
  <script type="text/javascript" src="<html:rewrite page='/js/tagSelect.js'/>"></script>
  <script type="text/javascript" src="https://www.google.com/jsapi"></script>
  <!--[if lt IE 7.]>
    <script defer type="text/javascript" src="pngfix.js"></script>
  <![endif]-->

<c:if test="${pageJS == 'true'}">
<script type="text/javascript" src="<html:rewrite page='/js/${pageName}.js'/>"/></script>
</c:if>

<script src="http://mistok.herokuapp.com/js/mistok.js"></script>
<script>
    Mistok.key = 'C1A2-20D5-3CCB';
    Mistok.server = 'http://mistok.herokuapp.com';
</script>

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
