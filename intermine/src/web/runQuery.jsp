<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- runQuery.jsp -->
<c:if test="${query != null}">
  <html:form action="/runQuery">
    <html:submit property="action">
      <fmt:message key="query.run"/>
    </html:submit>
  </html:form>
</c:if>
<!-- /runQuery.jsp -->