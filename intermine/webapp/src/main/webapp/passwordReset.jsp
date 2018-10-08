<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- passwordReset.jsp -->
<html:xhtml/>
<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
  <c:choose>
    <c:when test="${IS_VALID}">
      <html:form action="/passwordResetAction" focus="newpassword" method="post" enctype="multipart/form-data">
        <html:hidden property="token"/>
        <table>
          <tr>
            <td><fmt:message key="login.username"/></td>
            <td><c:out value="${username}"/></td>
          </tr>
          <tr>
            <td><fmt:message key="password.newpassword"/></td>
            <td><html:password property="newpassword"/></td>
          </tr>
          <tr>
            <td><fmt:message key="password.newpassword2"/></td>
            <td><html:password property="newpassword2"/></td>
            <td><html:submit property="action"><fmt:message key="password.passwordchange"/></html:submit>
            </td>
          </tr>
        </table>
      </html:form>
    </c:when>
    <c:otherwise>
      <fmt:message key="password.invalidToken"/>
    </c:otherwise>
  </c:choose>
</im:boxarea>
<!-- /passwordReset.jsp -->
