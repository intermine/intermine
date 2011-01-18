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

<ul class="inline">
<li class="first">
<html:link action="/exportOptions?table=${tableName}&amp;type=csv&amp;trail=${queryTrailLink}|${tableName}">
  <fmt:message key="exporter.csv.name"/>
</html:link>
</li>

<li><html:link action="/exportOptions?table=${tableName}&amp;type=galaxy&amp;trail=${queryTrailLink}|${tableName}">
  <fmt:message key="exporter.galaxy.description"/>
</html:link></li>

<c:forEach var="entry" items="${exporters}" varStatus="status">
  <c:set var="exporterId" value="${entry.key}"/>
  <c:choose>
    <c:when test="${empty entry.value}">
      <li><span class="nullStrike"><fmt:message key="exporter.${exporterId}.description"/></span></li>
    </c:when>
    <c:otherwise>
      <li><html:link action="/exportOptions?table=${tableName}&amp;type=${exporterId}&amp;trail=${queryTrailLink}|${tableName}">
        <fmt:message key="exporter.${exporterId}.description"/>
      </html:link></li>
    </c:otherwise>
  </c:choose>
</c:forEach>
</ul>

<!-- /export.jsp -->
