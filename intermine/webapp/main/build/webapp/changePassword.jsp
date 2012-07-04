<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- changePassword.jsp -->
<html:xhtml/>
&nbsp;
  <html:form action="/changePasswordAction">
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
<!-- /changePassword.jsp -->
