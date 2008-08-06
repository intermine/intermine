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
<script src="js/jquery-ui-personalized-1.5.2.min.js" type="text/javascript"></script>
<script type="text/javascript">
  // Use jQuery via jQuery(...)
  jQuery(document).ready(function(){
    jQuery('#pathsList').sortable({
        placeholder: "ui-selected", 
        revert: true
        });
  });
  
  function updatePathsString() {
      var sorted = jQuery('#pathsList').sortable( "serialize");
      jQuery('#pathsString').val(sorted);
  }
</script>
<style type="text/css" media="screen">
    .ui-selected{
        background:#CCFFCC;
    }
    #pathsList {
        list-style-position: inside; height: 30px; cursor: hand; cursor: pointer;
    }
    #pathsList li{
        float:left;
        list-style: none;
        border:2px solid #bbbbbb;
        background:#FFF;
        padding:5px;
        margin:5px 1px 10px 1px;
    }
</style>

<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
<h2><c:choose>
  <c:when test="${type == 'csv' || type == 'excel'}">
    <fmt:message key="exporter.${type}.description">
      <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
    </fmt:message>
  </c:when>
  <c:otherwise>
    <%-- <fmt:message bundle="model" key="exporter.${type}.description"/> --%>
  </c:otherwise>
</c:choose></h2>
<br/>

<!-- exporting type: ${type} -->

<html:form action="/${type}ExportAction" onsubmit="updatePathsString();">
  <c:choose>
    <c:when test="${type == 'csv'}">
      Choose a format:<br/>
      <html:radio property="format" value="csv"/>Comma separated values<br/>
      <html:radio property="format" value="tab"/>Tab separated values<br/>
    </c:when>
    <c:otherwise>
      <c:set var="tileName" value="${type}ExportOptions.tile"/>
      <p> tileName: ${tileName} </p>
      <c:if test="${!empty $tileName}">
        <tiles:insert name="${tileName}"/>
      </c:if>
    </c:otherwise>
  </c:choose>

  <br/>

  <html:hidden property="pathsString" styleId="pathsString" value="${pathsString}"/>
  <html:hidden property="table" value="${table}"/>
  <html:hidden property="type" value="${type}"/>

      
  <ul id="pathsList">
    <c:forEach var="path" items="${paths}" varStatus="status">
      <li id="${path}_${status.count}">${path}</li>
    </c:forEach>
  </ul>
  
  <br clear="both"/>
  <html:submit property="submit"><fmt:message key="export.submit"/></html:submit>
</html:form>
</im:boxarea>
</div>
<!-- /exportOptions.jsp -->
