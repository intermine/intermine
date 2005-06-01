<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- createTemplate.jsp -->
<c:if test="${!empty QUERY}">
  <form method="link" action="<html:rewrite action="/createTemplate"/>">
    <input type="submit" value="<fmt:message key="template.create"/>"/>
  </form>
</c:if>
<!-- /createTemplate.jsp -->
