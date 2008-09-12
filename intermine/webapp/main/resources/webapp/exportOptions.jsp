<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- exportOptions.jsp -->
<html:xhtml/>

<link rel="stylesheet" href="css/exportOptions.css" type="text/css" />
<script src="js/jquery-1.2.6.js" type="text/javascript" ></script>
<script src="js/exportoptions.js" type="text/javascript" ></script>
<script src="js/jquery-ui-personalized-1.5.2.min.js" type="text/javascript"></script>

<script type="text/javascript">
  // Use jQuery via jQuery(...)
  jQuery(document).ready(function(){
    jQuery('#pathsList').sortable({
            revert: true
        });
  });
</script>

<style type="text/css" media="screen">
    #pathsList {
        list-style-position: inside; height: 30px; cursor: hand; cursor: pointer;
    }
    #pathsList li{
        float:left;
        list-style: none;
        border:1px solid #bbbbbb;
        background:#FFF;
        padding:5px;
        margin:5px 1px 10px 1px;
    }
</style>

<tiles:get name="objectTrail.tile"/>

<fmt:message var="exportSubmitMessage" key="export.submit"/>

<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
<h2><c:choose>
  <c:when test="${type == 'csv' || type == 'excel'}">
    <fmt:message key="exporter.${type}.description">
      <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
    </fmt:message>
    <fmt:message var="exportReorderMessage" key="export.reorder.columns"/>
  </c:when>
  <c:otherwise>
    <fmt:message var="exportReorderMessage" key="export.reorder"/>
    <fmt:setBundle basename="model"/>
    <fmt:message key="exporter.${type}.description"/>
  </c:otherwise>
</c:choose></h2>
<div style="margin-top: 10px;">${exportReorderMessage}</div>

<!-- exporting type: ${type} -->

<html:form action="/${type}ExportAction" onsubmit="updatePathsString();">
  <div style="margin-top: 10px; margin-bottom: 10px;">
  <c:choose>
    <c:when test="${type == 'csv'}">
      Choose a format:<br/>
      <html:radio property="format" value="csv"/>Comma separated values<br/>
      <html:radio property="format" value="tab"/>Tab separated values<br/>
    </c:when>
    <c:when test="${type == 'excel'}">
      <%-- no extra options --%>
    </c:when>
    <c:otherwise>
      <c:set var="tileName" value="${type}ExportOptions.tile"/>
      <tiles:insert name="${tileName}"/>
    </c:otherwise>
  </c:choose>
  </div>

  <html:hidden property="pathsString" styleId="pathsString" value="${pathsString}"/>
  <html:hidden property="table" value="${table}"/>
  <html:hidden property="type" value="${type}"/>

  Add column &nbsp;
  <tiles:insert name="availableColumns.tile">
     <tiles:put name="table" value="${table}" />
  </tiles:insert>
  &nbsp;
  <button type="button" onclick="javascript:addSelectedPath()">Add</button>    
  <br />

  <ul id="pathsList">
  </ul>
  
  <script type="text/javascript">
      pathIndex = 1;
      
	  <c:forEach var="path" items="${paths}">
	     addPathElement("${path}");
	  </c:forEach>
  </script>

  <br clear="both"/>

  <html:submit property="submit">${exportSubmitMessage}</html:submit>
</html:form>
</im:boxarea>
</div>
<!-- /exportOptions.jsp -->
