<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- description.jsp -->
<c:if test="${!empty pageDescription}">
  <fmt:message key="${pageDescription}" var="description"/>
  <c:if test="${!empty description}">
    <div class="description">
      <c:out value="${description}"/>
    </div>
    <br/>
  </c:if>
</c:if>
<!-- /description.jsp -->
