<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<%-- Page that will take a request parameter of a file to transform and then include it --%>

<!-- static.jsp -->
<c:choose>
  <c:when test="${empty param.file}">
    <fmt:message key="documentation.error"/>
  </c:when>
  <c:otherwise>
    <c:import url="${param.file}"/>
  </c:otherwise>
</c:choose>
<!-- /static.jsp -->
