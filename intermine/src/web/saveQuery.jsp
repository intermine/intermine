<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- saveQuery.jsp -->
<c:if test="${!empty QUERY}">
  <html:form action="/saveQuery">
    <html:text property="queryName"/>
    <html:submit property="action">
      <fmt:message key="query.save"/>
    </html:submit>
  </html:form>
</c:if>
<!-- /saveQuery.jsp -->
