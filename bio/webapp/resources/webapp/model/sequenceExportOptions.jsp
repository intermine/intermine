<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- sequenceExportOptions.jsp -->
<html:xhtml />

Sequence to export:
<br />
<c:forEach items="${exportClassPaths}" var="entry" varStatus="status">
	<c:set var="path" value="${entry.key}" />
	<c:choose>
		<c:when test="${fn:length(exportClassPaths) == 1}">
			<html:hidden property="sequencePath" value="${path}" />
            ${entry.value}<br />
		</c:when>
		<c:otherwise>
		   <c:choose>
                <c:when test="${status.first}">
                    <input type="radio" name="sequencePath" value="${path}" checked="checked" />${entry.value}<br />
                </c:when>
                <c:otherwise>
                     <input type="radio" name="sequencePath" value="${path}" />${entry.value}<br />
                </c:otherwise>
            </c:choose>
		</c:otherwise>
	</c:choose>
</c:forEach>

<!-- /sequenceExportOptions.jsp -->
