<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- login.jsp -->
<html:form action="/loginAction">
  <table>
    <tr><td><fmt:message key="login.username"/></td><td><html:text property="username"/><br/></td></tr>
    <tr><td><fmt:message key="login.password"/></td><td><html:password property="password"/><br/></td></tr>
  </table>
  <html:submit property="action"/>
</html:form>
<!-- /login.jsp -->
