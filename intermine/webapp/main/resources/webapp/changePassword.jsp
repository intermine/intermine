<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- changePassword.jsp -->
<html:xhtml/>
<div class="body">
  <html:form action="/changePasswordAction">
    <html:hidden property="username" value="${PROFILE.username}"/>
    <table>
      <tr>
        <td><fmt:message key="password.oldpassword"/></td>
        <td><html:password property="oldpassword"/></td>
      </tr>
      <tr>
        <td><fmt:message key="password.newpassword"/></td>
        <td><html:password property="newpassword"/></td>
      </tr>
      <tr>
        <td><fmt:message key="password.newpassword2"/></td>
        <td><html:password property="newpassword2"/></td>
        <td><html:submit property="action"><fmt:message key="password.passwordchange"/></html:submit></td>
      </tr>
    </table>
  </html:form>
</div>
<!-- /changePassword.jsp -->
