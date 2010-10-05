<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- export.jsp -->

<c:set var="queryTrailLink" value="|query"/>
<c:if test="${fn:startsWith(tableName, 'bag.')}">
  <c:set var="queryTrailLink" value=""/>
</c:if>
<html:xhtml/>

<html:link action="/exportOptions?table=${tableName}&amp;type=csv&amp;trail=${queryTrailLink}|${tableName}">
  <fmt:message key="exporter.csv.description"/>
</html:link>
<br/>

<c:if test="${pagedTable.estimatedSize <= WEB_PROPERTIES['max.excel.export.size']}">
  <html:link action="/exportOptions?table=${tableName}&amp;type=excel&amp;trail=${queryTrailLink}|${tableName}">
    <fmt:message key="exporter.excel.description">
      <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
    </fmt:message>
  </html:link>
  <br/>
</c:if>

    <html:link action="/exportOptions?table=${tableName}&amp;type=galaxy&amp;trail=${queryTrailLink}|${tableName}">
      <fmt:message key="exporter.galaxy.description"/>
    </html:link>
    <br/>
<%--
  <c:choose>
    <c:when test="${exportAsBED}">
        <a href="${GALAXY_URL}" target="_blank" onclick="javascript:jQuery.post('${GALAXY_URL}',jQuery('#galaxyform').serialize());">
        <a href="javascript:jQuery('#galaxyform').submit();">
          <fmt:message key="exporter.galaxy.description"/>
        </a>
        <br/>
    </c:when>
    <c:otherwise>
      <span class="nullStrike"><fmt:message key="exporter.galaxy.description"/></span><br>
    </c:otherwise>
  </c:choose>
--%>

<c:forEach var="entry" items="${exporters}" varStatus="status">
  <c:set var="exporterId" value="${entry.key}"/>
  <c:choose>
    <c:when test="${empty entry.value}">
      <span class="nullStrike"><fmt:message key="exporter.${exporterId}.description"/></span><br>
    </c:when>
    <c:otherwise>
      <html:link action="/exportOptions?table=${tableName}&amp;type=${exporterId}&amp;trail=${queryTrailLink}|${tableName}">
        <fmt:message key="exporter.${exporterId}.description"/>
      </html:link><br>
    </c:otherwise>
  </c:choose>
</c:forEach>

<!-- /export.jsp -->
