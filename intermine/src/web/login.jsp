<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- login.jsp -->
<html:xhtml/>
<div class="body">
  <html:form action="/loginAction">
    <fmt:message key="login.haspassword"/><br/><br/>
    <table>
      <tr>
        <td><fmt:message key="login.username"/></td>
        <td><html:text property="username"/><br/></td>
      </tr>
      <tr>
        <td><fmt:message key="login.password"/></td>
        <td><html:password property="password"/><br/></td>
        <td><html:submit property="action"><fmt:message key="login.login"/></html:submit></td>
      </tr>
    </table>
  </html:form>

  <br/>

  <html:form action="/requestPasswordAction">  
    <fmt:message key="login.needspassword"/><br/><br/>
    <table>
      <tr>
        <td><fmt:message key="login.username"/></td>
        <td><html:text property="username"/></td>
        <td><html:submit property="action"><fmt:message key="login.passwordrequest"/></html:submit></td>
      </tr>
    </table>
  </html:form>
</div>
<!-- /login.jsp -->
