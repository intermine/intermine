<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- export.jsp -->

<html:xhtml/>

<html:link action="/exportOptions?table=${tableName}&amp;type=csv">
  <fmt:message key="exporter.csv.description"/>
</html:link>
<br/>

<c:if test="${pagedTable.estimatedSize <= WEB_PROPERTIES['max.excel.export.size']}">
  <html:link action="/exportOptions?table=${tableName}&amp;type=excel">
    <fmt:message key="exporter.excel.description">
      <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
    </fmt:message>
  </html:link>
  <br/>
</c:if>

<fmt:setBundle basename="model"/>

<c:forEach var="entry" items="${exporters}" varStatus="status">
  <c:set var="exporterId" value="${entry.key}"/>
  <c:choose>
    <c:when test="${empty entry.value}">
      <span class="nullStrike"><fmt:message key="exporter.${exporterId}.description"/></span><br>
    </c:when>
    <c:otherwise>
      <html:link action="/exportOptions?table=${tableName}&amp;type=${exporterId}">
        <fmt:message key="exporter.${exporterId}.description"/>
      </html:link><br>
    </c:otherwise>
  </c:choose>
</c:forEach>

<!-- /export.jsp -->
