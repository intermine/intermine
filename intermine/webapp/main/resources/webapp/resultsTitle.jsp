<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%-- results page title --%>

<c:choose>
<c:when test="${fn:startsWith(param.table, 'col')}">
  <fmt:message key="collectionDetails.title"/>
</c:when>
<c:when test="${fn:startsWith(param.table, 'bag')}">
  <fmt:message key="bagDetails.title"/>
</c:when>
<c:otherwise>
  <fmt:message key="results.title"/>
</c:otherwise>
</c:choose>

<%-- /resultsTitle.jsp --%>