<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<div class="savedView">
  <c:if test="${!empty SAVED_QUERIES}">
    <span class="title"><fmt:message key="query.savedqueriesheader"/></span>
    <ul>
      <c:forEach items="${SAVED_QUERIES}" var="queryName">
        <li>
          <c:out value="${queryName.key}"/>
        </li>
      </c:forEach>
    </ul>
  </c:if>
</div>
