<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<!-- layout.jsp -->
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<html:html locale="true" xhtml="true">
  <%-- from the tiles config file for description.jsp --%>
  <tiles:importAttribute name="pageName"/>

  <head>
    <html:base/>
    <link rel="stylesheet" type="text/css" href="http://www.intermine.org/style/intermine.css"/>
    <link rel="stylesheet" type="text/css" href="intermine.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    <meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
    <meta content="Integrated queryable database for Drosophila and Anopheles genomics" 
          name="description"/>
    <meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>
    <title>
      <fmt:message key="${pageName}.title" var="pageTitle"/>
      <c:out value="${WEB_PROPERTIES['project.title']} - ${pageTitle}" escapeXml="false"/>
    </title>
  </head>
  
  <body>
    <div class="main-layout">
      <tiles:get name="header"/>
      <tiles:get name="menu"/>
      <br/>
      <tiles:get name="errorMessages"/>
      <tiles:insert attribute="description">
        <tiles:put name="pageName" beanName="pageName" beanScope="tile"/>
      </tiles:insert>
      <tiles:get name="body"/>
      <br/>
      <tiles:get name="footer"/>
    </div>
  </body>
</html:html>
<!-- /layout.jsp -->
