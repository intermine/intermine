<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- database.jsp -->
<html:link href="${object.url}" target="view_window">
  <c:out value="${object.title}"/>
</html:link>
<!-- /database.jsp -->
