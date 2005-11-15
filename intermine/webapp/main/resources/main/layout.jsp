<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- layout.jsp -->
<html:xhtml/>
<html:html locale="true" xhtml="true">
  <%-- from the tiles config file for description.jsp --%>
  <tiles:importAttribute name="pageName" scope="request"/>

  <head>
    <html:base/>
    <link rel="stylesheet" type="text/css" href="webapp.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    
    <script type="text/javascript" src="js/prototype.js"></script>
    <script type="text/javascript" src="js/scriptaculous.js"></script>
    
    <meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
    <meta content="Integrated queryable database for Drosophila and Anopheles genomics" 
          name="description"/>
    <meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>
    
    <title>
      <tiles:get name="title"/>
    </title>
    
    <script type="text/javascript">
    <!--
      function showFeedbackForm()
      {
        document.getElementById('feedbackFormDiv').style.display='';
        document.getElementById('feedbackFormDivButton').style.display='none';
        window.scrollTo(0, 99999);
        document.getElementById("fbname").focus();
      }
    //-->
    </script>
    
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
      <fmt:message key="${pageName}.help.link" var="helplink"/>
      <c:if test="${!empty helplink}">
        <c:set var="helpUrl" value="${WEB_PROPERTIES['project.helpLocation']}${helplink}"/>
      </c:if>
      <im:box titleKey="${pageName}.description" helpUrl="${helpUrl}">
        <tiles:get name="body"/>
      </im:box>
      
      <div id="feedbackFormDivButton">
        <im:vspacer height="11"/>
        <div class="expandButton">
           <a href="#" onclick="showFeedbackForm();return false">
             <b><fmt:message key="feedbackBox.title"/></b>
           </a>
      	</div>
      </div>
      
      <div id="feedbackFormDiv" style="display:none">
          <im:vspacer height="11"/>
          <im:box titleKey="feedbackBox.title">
            <tiles:get name="feedbackForm"/>
          </im:box>
      </div>
      
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
    
  </body>
</html:html>
<!-- /layout.jsp -->

