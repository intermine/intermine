<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- createAccount.jsp -->
<html:xhtml/>
<div class="body">
  <html:form action="/createAccountAction">

	<div style="width:70%">
	  <b><i><fmt:message key="createAccount.privacy"/></i></b>
	</div>
	<p/>
    <table>
      <tr>
        <td><fmt:message key="createAccount.username"/></td>
        <td><html:text property="username"/><br/></td>
      </tr>
      <tr>
        <td><fmt:message key="createAccount.password"/></td>
        <td><html:password property="password"/><br/></td>
      </tr>
      <tr>
        <td><fmt:message key="createAccount.password2"/></td>
        <td><html:password property="password2"/><br/></td>
        <td><html:submit property="action"><fmt:message key="createAccount.createAccount"/></html:submit></td>
      </tr>
    </table>
  </html:form>
</div>
<!-- /createAccount.jsp -->