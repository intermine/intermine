<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- error.jsp -->
<h2>
  <fmt:message key="error.title"/>
</h2>

<html:messages id="error">
  <c:out value="${error}"/>
  <br/>
</html:messages>

<h2>
  <fmt:message key="error.stacktrace"/>
</h2>

<c:out value="${stacktrace}"/><br/>
<!-- /error.jsp =-->