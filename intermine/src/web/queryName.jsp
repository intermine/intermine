<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<!-- queryName.jsp -->
<div class="queryName">
  <c:if test="${SAVED_QUERY_NAME != null}">
    Query saved as:         
    <html:link action="/loadQuery?queryName=${SAVED_QUERY_NAME}">
      <c:out value="${SAVED_QUERY_NAME}"/>
    </html:link>
  </c:if>
</div>
<!-- /queryName.jsp -->
