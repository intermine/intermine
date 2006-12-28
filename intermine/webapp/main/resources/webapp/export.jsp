<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- export.jsp -->
<html:xhtml/>

<div class="heading">
  <fmt:message key="export.export"/><im:manualLink section="manualExportData.shtml"/>
</div>
<div class="body">
  <ul>
    <li>
      <html:link action="/exportAction?table=${tableName}&amp;type=csv&amp;tableType=${tableType}">
        <fmt:message key="export.csv"/>
      </html:link>
    </li>
    <li>
      <html:link action="/exportAction?table=${tableName}&amp;type=tab&amp;tableType=${tableType}">
        <fmt:message key="export.tabdelimited"/>
      </html:link>
    </li>
    <c:if test="${pagedTable.size <= WEB_PROPERTIES['max.excel.export.size']}">
      <li>
        <html:link action="/exportAction?table=${tableName}&amp;type=excel&amp;tableType=${tableType}">
          <fmt:message key="export.excel">
            <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
          </fmt:message>
        </html:link>
      </li>
    </c:if>

    <fmt:setBundle basename="model"/>

    <c:forEach var="entry" items="${exporters}" varStatus="status">
      <li>
        <c:choose>
          <c:when test="${empty entry.value}">
            <span class="nullStrike"><fmt:message key="exporter.${entry.key}.description"/></span>
          </c:when>
          <c:otherwise>
        <html:link action="${entry.value.actionPath}&amp;table=${tableName}&amp;tableType=${tableType}">
          <fmt:message key="exporter.${entry.key}.description"/>
        </html:link>
          </c:otherwise>
        </c:choose>
      </li>
    </c:forEach>

  </ul>
</div>
<!-- /export.jsp -->
