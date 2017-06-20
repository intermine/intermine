<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- createAccount.jsp -->
<html:xhtml/>
<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
  <html:form action="/createAccountAction">
      <b><i><fmt:message key="createAccount.privacy"/></i></b>
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
      </tr>
      <tr>
      <c:if test="${!empty WEB_PROPERTIES['mail.mailing-list']}">
        <td>&nbsp;</td>
        <td><html:checkbox property="mailinglist"/><i>&nbsp;<fmt:message key="createAccount.mailinglist"/></i></td>
      </c:if>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
        <td><html:submit property="action"><fmt:message key="createAccount.createAccount"/></html:submit></td>
      </tr>
    </table>
  </html:form>
</im:boxarea>
</div>
<!-- /createAccount.jsp -->
