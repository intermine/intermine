<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<!-- layout.jsp -->
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<html:html locale="true" xhtml="true">
  <%-- from the tiles config file for description.jsp --%>
  <tiles:importAttribute name="pageName"/>

  <head>
    <html:base/>
    <link rel="stylesheet" type="text/css" href="<html:rewrite href='${WEB_PROPERTIES["project.sitePrefix"]}/style/default.css'/>"/>
    <link rel="stylesheet" type="text/css" href="webapp.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    <meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
    <meta content="Integrated queryable database for Drosophila and Anopheles genomics" 
          name="description"/>
    <meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>
    <script src="<html:rewrite href='${WEB_PROPERTIES["project.sitePrefix"]}/style/footer.js'/>" type="text/javascript">;</script>
    <title>
      <fmt:message key="${pageName}.title" var="pageTitle"/>
      <c:out value="${WEB_PROPERTIES['project.title']} - ${pageTitle}" escapeXml="false"/>
    </title>
  </head>
  
  <body>
    <tiles:get name="header"/>
    <tiles:get name="menu"/>
    <div class="main-layout" id="content">
      <tiles:get name="errorMessages"/>
      <tiles:insert attribute="description">
        <tiles:put name="pageName" beanName="pageName" beanScope="tile"/>
      </tiles:insert>
      <tiles:get name="body"/>
    </div>
    <tiles:get name="footer"/>
  </body>
</html:html>
<!-- /layout.jsp -->
