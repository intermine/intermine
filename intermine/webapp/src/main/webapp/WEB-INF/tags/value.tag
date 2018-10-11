<%@ tag body-content="scriptless" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<jsp:doBody var="body"/>

<c:choose>
  <c:when test="${fn:startsWith(fn:trim(body), 'http://')}">
    <a href="${body}" class="value extlink">
  </c:when>
  <c:when test="${!fn:startsWith(fn:trim(body), '<STYLE')}">
    <span class="value">
  </c:when>
</c:choose>

<c:choose>
    <%-- FIXME we should configure which fields are raw HTML instead of looking for style tag --%>
     <c:when test="${fn:startsWith(fn:trim(body), '<STYLE')}">
        ${body}
    </c:when>
    <c:otherwise>
        <c:out value="${body}"/>
    </c:otherwise>
</c:choose>

<c:choose>
  <c:when test="${fn:startsWith(fn:trim(body), 'http://')}">
    </a>
  </c:when>
  <c:when test="${!fn:startsWith(fn:trim(body), '<STYLE')}">
    </span>
  </c:when>
</c:choose>
