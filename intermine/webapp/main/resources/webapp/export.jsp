<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- export.jsp -->

<html:xhtml/>

<html:link action="/exportAction?table=${tableName}&amp;type=csv">
    <fmt:message key="export.csv"/>
</html:link><br>

<html:link action="/exportAction?table=${tableName}&amp;type=tab">
    <fmt:message key="export.tabdelimited"/>
</html:link><br>

<c:if test="${pagedTable.estimatedSize <= WEB_PROPERTIES['max.excel.export.size']}">
    <html:link action="/exportAction?table=${tableName}&amp;type=excel">
        <fmt:message key="export.excel">
            <fmt:param value="${WEB_PROPERTIES['max.excel.export.size']}"/>
        </fmt:message>
    </html:link><br>
</c:if>

<fmt:setBundle basename="model"/>

<c:forEach var="entry" items="${exporters}" varStatus="status">
    <c:choose>
        <c:when test="${empty entry.value}">
            <span class="nullStrike"><fmt:message key="exporter.${entry.key}.description"/></span><br>
        </c:when>
	    <c:otherwise>
	        <html:link action="${entry.value.actionPath}&amp;table=${tableName}">
	            <fmt:message key="exporter.${entry.key}.description"/>
	        </html:link><br>
	    </c:otherwise>
    </c:choose>
</c:forEach>

<!-- /export.jsp -->
