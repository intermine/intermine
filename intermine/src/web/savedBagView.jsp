<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<div class="savedView">
  <c:if test="${!empty SAVED_BAGS}">
    <span class="title"><fmt:message key="query.savedbagsheader"/></span>
    <ul>
      <c:forEach items="${SAVED_BAGS}" var="bagName">
        <li>
          <c:out value="${bagName.key}"/>
        </li>
      </c:forEach>
    </ul>
  </c:if>
</div>
