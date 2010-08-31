<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
<script src="js/exportoptions.js" type="text/javascript" ></script>

<script type="text/javascript">
  jQuery.noConflict();
  // Use jQuery via jQuery(...)
  jQuery(document).ready(function(){
    jQuery('#pathsList').sortable({
            revert: true
        });
  });
</script>

<style type="text/css" media="screen">
    #pathsList {
        list-style-position: inside;
        cursor: hand;
        cursor: pointer;
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
<%-- TODO FIXME
<tiles:get name="objectTrail.tile"/>
 --%>
<fmt:message var="exportSubmitMessage" key="export.submit"/>

<div align="center">
<div style="clear:both;width:60%" class="body" align="left">

<c:choose>
  <c:when test="${type == 'galaxy'}">
    <c:set var="tileName" value="${type}ExportOptions.tile"/>
    <tiles:insert name="${tileName}"/>
  </c:when>
  <c:otherwise>
    <html:form action="/${type}ExportAction" onsubmit="updatePathsString();">
    <fieldset>
    <legend><c:choose>
      <c:when test="${type == 'csv' || type == 'excel'}">
        <fmt:message key="exporter.${type}.description">
          <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
        </fmt:message>
        <fmt:message var="exportReorderMessage" key="export.reorder.columns"/>
      </c:when>
      <c:otherwise>
        <fmt:message var="exportReorderMessage" key="export.reorder"/>
        <fmt:message key="exporter.${type}.description"/>
      </c:otherwise>
    </c:choose></legend>

    <!-- exporting type: ${type} -->
    <ol>
      <li><fieldset>
          <c:choose>
        <c:when test="${type == 'csv'}">
          <legend>Choose a format:</legend>
          <ol>
            <li><html:radio property="format" value="csv"/><label>Comma separated values</label></li>
            <li><html:radio property="format" value="tab"/><label>Tab separated values</label></li>
          </ol>

        </c:when>
        <c:when test="${type == 'excel'}">
          <%-- no extra options --%>
        </c:when>
        <c:otherwise>
          <c:set var="tileName" value="${type}ExportOptions.tile"/>
          <tiles:insert name="${tileName}"/>
        </c:otherwise>
          </c:choose>
        </fieldset>
      </li>
      <c:if test="${type == 'csv'}">
      <li class="columnHeaderOption">
        <input type="checkbox" name="includeHeaders" checked/>
        <label>Include column headers in output</label>
      </li>
      </c:if>
      <li class="columnHeaderOption">
        <html:checkbox property="doGzip"/>
        <label>Compress data using gzip</label>
      <li>

      <html:hidden property="pathsString" styleId="pathsString" value="${pathsString}"/>
      <html:hidden property="table" value="${table}"/>
      <html:hidden property="type" value="${type}"/>

      <li><label>Add column:</label> &nbsp;
      <tiles:insert name="availableColumns.tile">
         <tiles:put name="table" value="${table}" />
      </tiles:insert>
      &nbsp;
      <button type="button" onclick="javascript:addSelectedPath()" id="columnAddButton">Add</button></li>

      <li><label>${exportReorderMessage}:</label>
      <ul id="pathsList">
      </ul>

      <script type="text/javascript">
          pathIndex = 1;

        <c:forEach var="path" items="${pathsMap}">
           addPathElement("${path.key}", "${path.value}");
        </c:forEach>

        if (document.getElementById('columnToAdd')[0].value == '') {
            document.getElementById("columnAddButton").disabled = true;
            document.getElementById("columnToAdd").disabled = true;
        }
      </script></li>
    </ol>
    </fieldset>
      <fieldset class="submit"><html:submit property="submit">${exportSubmitMessage}</html:submit></fieldset>
    </html:form>
  </c:otherwise>
</c:choose>
</div>
</div>
<!-- /exportOptions.jsp -->
