<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<!-- layout.jsp -->
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<html:html locale="true" xhtml="true">
  <%-- from the tiles config file for description.jsp --%>
  <tiles:importAttribute name="pageName"/>

  <head>
    <html:base/>
    <link rel="stylesheet" type="text/css" href="${WEB_PROPERTIES["project.sitePrefix"]}/style/default.css"/>
    <link rel="stylesheet" type="text/css" href="webapp.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    <meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
    <meta content="Integrated queryable database for Drosophila and Anopheles genomics" 
          name="description"/>
    <meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>
    <script src="${WEB_PROPERTIES['project.sitePrefix']}/style/footer.js" type="text/javascript">;</script>
    <title>
      <fmt:message key="${pageName}.title" var="pageTitle"/>
      <c:out value="${WEB_PROPERTIES['project.title']} - ${pageTitle}" escapeXml="false"/>
    </title>
  </head>
  
  <body>
    <tiles:get name="header"/>
    <tiles:get name="menu"/>
    <div id="content">
    
      <%-- figure out whether or not we have a page description, if not we don't
           render a box around the content frame. --%>
      <fmt:message key="${pageName}.description" var="description"/>
      <c:set var="hasDesc" value="${!empty description}"/>
      
      <tiles:get name="errorMessages"/>
      
      <%-- table only if we have a description --%>
      <c:if test="${hasDesc}">
        <table class="box" cellspacing="0" cellpadding="6" border="0" width="100%" align="center">
      </c:if>
      
      <tiles:insert attribute="description">
        <tiles:put name="pageName" beanName="pageName" beanScope="tile"/>
      </tiles:insert>

      <%-- table only if we have a description --%>
      <c:if test="${hasDesc}">
        <tr>
          <td valign="top" colspan="2">
            <tiles:get name="body"/>
          </td>
        </tr>
        </table>
      </c:if>

      <c:if test="${not hasDesc}">
        <tiles:get name="body"/>
      </c:if>

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
