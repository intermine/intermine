<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- database.jsp -->
<html:link href="${object.url}" target="view_window">
  <c:out value="${object.title}"/>
</html:link>
<!-- /database.jsp -->
