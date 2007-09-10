<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- layout.jsp -->
<html:xhtml/>
<html:html locale="true" xhtml="true">
  <%-- from the tiles config file for description.jsp --%>
  <tiles:importAttribute name="pageName" scope="request"/>
  <head>
    <html:base/>
    <tiles:insert name="htmlHead.jsp">
      <tiles:put name="title" value="${title}"/>
    </tiles:insert>
  </head>

  <body>
  
  <tiles:insert name="headMenu.jsp">
    <tiles:put name="header"/>
    <tiles:put name="menu"/>
  </tiles:insert>

  <div id="pagecontent">
    
      <%-- Render messages --%>
      <tiles:get name="errorMessages"/>
      <%-- Context help bar --%>
      <tiles:insert page="/contextHelp.jsp"/>
      


      <tiles:get name="body"/>
      
      <%-- footer (welcome logo, bottom nav, and feedback link) --%>
	  <c:import url="footer.jsp"/>
      
      <c:if test="${param.debug != null}">
        <im:vspacer height="11"/>
        <im:box title="Debug">
          <tiles:insert page="/session.jsp"/>
        </im:box>
      </c:if>
      
    </div>
    
    <c:if test="${IS_SUPERUSER}">
      <div class="admin-msg">
        <span class="smallnote">
          <fmt:message key="intermine.superuser.msg"/>
        </span>
      </div>
    </c:if>
    
    <c:set var="googleAnalyticsId" value="${WEB_PROPERTIES['google.analytics.id']}"/>
    <c:if test="${!empty googleAnalyticsId}">
      <script defer="true" src="http://www.google-analytics.com/urchin.js" type="text/javascript">
      </script>
      <script defer="true" type="text/javascript">
        _uacct = "${googleAnalyticsId}";
        try {urchinTracker();} catch (e) {<%-- ignore - google is down --%>  }
      </script>
    </c:if>

  </body>
</html:html>
<!-- /layout.jsp -->

