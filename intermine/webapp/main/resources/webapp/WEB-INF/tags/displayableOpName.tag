<%@ tag body-content="empty" %>
<%@ attribute name="opName" required="true" %>
<%@ attribute name="valueType" required="true" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:choose>
  <c:when test="${valueType != 'String'}">
    <c:out value="${opName}"/>
  </c:when>
  <c:when test="${opName == '='}">
    <fmt:message key="query.ops.text.equals"/>
  </c:when>
  <c:when test="${opName == '!='}">
    <fmt:message key="query.ops.text.notEquals"/>
  </c:when>
  <c:when test="${opName == '<'}">
    <fmt:message key="query.ops.text.lessThan"/>
  </c:when>
  <c:when test="${opName == '<='}">
    <fmt:message key="query.ops.text.lessThanEquals"/>
  </c:when>
  <c:when test="${opName == '>'}">
    <fmt:message key="query.ops.text.greaterThan"/>
  </c:when>
  <c:when test="${opName == '>='}">
    <fmt:message key="query.ops.text.greaterThanEquals"/>
  </c:when>
  <c:otherwise>
    <c:out value="${opName}"/>
  </c:otherwise>
</c:choose>
