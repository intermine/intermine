<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<!-- examples.jsp -->
<fmt:setBundle basename="model"/>

<c:if test="${!empty TEMPLATE_QUERIES}">
  <c:forEach items="${TEMPLATE_QUERIES}" var="templateQuery">
    <span class="title">
      <c:out value="${templateQuery.value.cleanDescription}"/>
    </span>
    <span class="link">
      <html:link action="/template?name=${templateQuery.key}">
        <img class="arrow" src="images/right-arrow.png" alt="->"/>
      </html:link>
    </span>
    <br/>
  </c:forEach>
</c:if>
<!-- /examples.jsp -->
