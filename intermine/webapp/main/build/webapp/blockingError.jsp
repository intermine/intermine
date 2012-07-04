<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- blockingError.jsp -->
<html:html lang="true" xhtml="true">
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/webapp.css'/>"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/inlineTagEditor.css'/>"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/resultstables.css'/>" />
<c:set var="theme" value="${WEB_PROPERTIES['theme']}"/>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/themes/${theme}/theme.css'/>"/>
<script type="text/javascript" src="<html:rewrite page='/js/jquery-1.5.1.min.js'/>"></script>

<body>
<div align="center" id="headercontainer">

<div id="header">
    <a href="${WEB_PROPERTIES['project.sitePrefix']}" alt="Home" rel="NOFOLLOW"><img id="logo" src="model/images/logo.png" width="45px" height="43px" alt="Logo" /></a>
    <h1><html:link href="${WEB_PROPERTIES['project.sitePrefix']}/"><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></html:link></h1>
    <p id="version"><fmt:message key="header.version"/> <c:out value="${WEB_PROPERTIES['project.releaseVersion']}" escapeXml="false"/></span>
    <p><c:out value="${WEB_PROPERTIES['project.subTitle']}" escapeXml="false"/></p>
  </div>
</div>
<div id="pagecontentcontainer" align="center">
      <div id="pagecontent">
      
        <div id="navtrail">

  <!-- contact us -->

<script type="text/javascript">
jQuery(document).ready(function() {
  jQuery("p#contactUsLink").toggle();
});
</script>

    <p id="contactUsLink" style="display:none;" class="alignleft">
    <a href="#" onclick="showContactForm();return false;"><fmt:message key="feedback.link"/></a>
    </p>

    <p id="takeATourLink" style="display:none;" class="alignleft">
    <im:popupHelp pageName="tour/start">Take a tour</im:popupHelp>
    </p>

    <!-- Nav trail -->
    <p class="alignright">
    <html:link href="${WEB_PROPERTIES['project.sitePrefix']}"><c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/></html:link>
    <im:contextHelp/>
   </p>
 </div>

<div style="clear: both;"></div>
      <tiles:insert page="errorMessagesContainers.jsp"/>
      <%-- Context help bar --%>
      <tiles:insert page="/contextHelp.jsp"/>
      <tiles:insert name="errorMessages.tile"/>
      
      <%-- footer (welcome logo, bottom nav, and feedback link) --%>
      <c:import url="footer.jsp"/>
      </div>
 </div>
</body>
</html:html>
<!-- /blockinError.jsp -->