<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- description.jsp -->
<c:if test="${pageDescription != null && pageDescription ne ''}">
  <div class="description">
    <fmt:message key="${pageDescription}.long"/>
  </div>
  <br/>
</c:if>
<!-- /description.jsp -->
