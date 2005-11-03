<%@ tag body-content="scriptless" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<jsp:doBody var="body"/>

<c:choose>
  <c:when test="${fn:startsWith(fn:trim(body), 'http://')}">
    <a href="${body}" class="value extlink">
  </c:when>
  <c:otherwise>
    <span class="value">
  </c:otherwise>
</c:choose>
${body}
<c:choose>
  <c:when test="${fn:startsWith(fn:trim(body), 'http://')}">
    </a>
  </c:when>
  <c:otherwise>
    </span>
  </c:otherwise>
</c:choose>
