<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- hiddenField.jsp -->
<div style="white-space: nowrap">
  <span class="fieldName"><c:out value="${fieldDescriptor.name}"/></span>:
  <c:if test="${object[fieldDescriptor.name] == null}">
    <fmt:message key="report.nullField"/>
  </c:if>
  <c:if test="${object[fieldDescriptor.name] != null}">
    <fmt:message key="hidden.field"/>
  </c:if>
</div>
<!-- /hiddenField.jsp -->
