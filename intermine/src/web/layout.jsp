<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<!-- layout.jsp -->
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<html:html locale="true" xhtml="true">
  <%-- from the tiles config file for description.jsp --%>
  <tiles:importAttribute name="pageName"/>

  <head>
    <html:base/>
    <link rel="stylesheet" type="text/css" 
          href="${WEB_PROPERTIES["project.sitePrefix"]}/style/base.css"/>
    <link rel="stylesheet" type="text/css" 
          href="${WEB_PROPERTIES["project.sitePrefix"]}/style/branding.css"/>
    <link rel="stylesheet" type="text/css" href="webapp.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    <meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
    <meta content="Integrated queryable database for Drosophila and Anopheles genomics" 
          name="description"/>
    <meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>
    <script src="${WEB_PROPERTIES['project.sitePrefix']}/style/footer.js" type="text/javascript">;</script>
    <title>
      <fmt:message key="${pageName}.title" var="pageTitle"/>
      <c:out value="${WEB_PROPERTIES['project.title']}: ${pageTitle}" escapeXml="false"/>
    </title>
  </head>
  
  <body>
    <tiles:get name="header"/>
    <tiles:get name="menu"/>
    <div id="pagecontent">
    
      <%-- Render messages --%>
      <tiles:get name="errorMessages"/>
      <%-- Context help bar --%>
      <tiles:insert page="/contextHelp.jsp"/>
      
      <%-- Construct help page key --%>
      <fmt:message key="${pageName}.help" var="help"/>
      <c:if test="${!empty help}">
        <c:set var="helpUrl" value="${WEB_PROPERTIES['project.sitePrefix']}/doc/webapp/${pageName}.html"/>
      </c:if>
      <im:box titleKey="${pageName}.description" helpUrl="${helpUrl}">
        <tiles:get name="body"/>
      </im:box>
    </div>
    
    <c:if test="${IS_SUPERUSER}">
      <div class="admin-msg">
        <span class="smallnote">
          <fmt:message key="intermine.superuser.msg"/>
        </span>
      </div>
    </c:if>
    
    <tiles:get name="footer"/>
  </body>
</html:html>
<!-- /layout.jsp -->
