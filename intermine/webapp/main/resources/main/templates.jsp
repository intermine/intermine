<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- templates.jsp -->
<c:choose>
  <c:when test="${empty param.category}">
    <c:forEach items="${CATEGORY_TEMPLATES}" var="category">
      <c:if test="${!empty CATEGORY_TEMPLATES[category.key]}">
        <div class="heading">${category.key}</div>
        <div class="body"><im:templateList type="global" category="${category.key}"/></div>
        <im:vspacer height="5"/>
      </c:if>
    </c:forEach>
  </c:when>
  <c:otherwise>
    <div class="heading">${param.category}</div>
    <div class="body"><im:templateList type="global" category="${param.category}"/></div>
  </c:otherwise>
</c:choose>
<div class="body">
  <p>
    <html:link action="/templateSearch"><fmt:message key="templates.searchtemplates"/></html:link>
  </p>
</div>
<!-- /templates.jsp -->
