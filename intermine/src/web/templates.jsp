<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- templates.jsp -->
<c:choose>
  <c:when test="${empty param.category}">
    <c:forEach items="${CATEGORIES}" var="category">
      <c:if test="${!empty CATEGORY_TEMPLATES[category]}">
        <div class="heading">${category}</div>
        <div class="body"><im:templateList type="global" category="${category}"/></div>
        <im:vspacer height="5"/>
      </c:if>
    </c:forEach>
  </c:when>
  <c:otherwise>
    <div class="heading">${param.category}</div>
    <div class="body"><im:templateList type="global" category="${param.category}"/></div>
  </c:otherwise>
</c:choose>
<!-- /templates.jsp -->
