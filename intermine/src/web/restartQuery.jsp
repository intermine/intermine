<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- restartQuery.jsp -->
<html:form action="/restartQuery">
  <html:submit property="action">
    <fmt:message key="query.reset"/>
  </html:submit>
</html:form>
<!-- /restartQuery.jsp -->
