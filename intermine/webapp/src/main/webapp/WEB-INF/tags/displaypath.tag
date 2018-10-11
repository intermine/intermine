<%@ tag body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<%@ attribute name="path" required="true" %>

<c:choose>
  <c:when test="${empty QUERY}">
    <c:set var="formattedPath" value="${imf:formatPathStr(path, INTERMINE_API, WEBCONFIG)}"/>
  </c:when>
  <c:otherwise>
    <c:set var="formattedPath" value="${imf:formatQueryPath(path, QUERY, WEBCONFIG)}"/>
  </c:otherwise>
</c:choose>

<c:out value="${formattedPath}"/>
