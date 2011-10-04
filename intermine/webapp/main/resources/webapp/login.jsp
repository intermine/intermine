<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- login.jsp -->
<html:xhtml/>
<div class="body" align="center">

<im:boxarea stylename="plainbox" fixedWidth="60%">
  <html:form action="/loginAction" focus="username" method="post" enctype="multipart/form-data">
    <h3><fmt:message key="login.haspassword"/></h3>
    <html:hidden property="returnToString"/>
    <table>
      <tr>
        <td><fmt:message key="login.username"/></td>
        <td><html:text property="username"/><br/></td>
      </tr>
      <tr>
        <td><fmt:message key="login.password"/></td>
        <td><html:password property="password"/><br/></td>
        <td><html:submit property="action"><fmt:message key="login.login"/></html:submit>
        &nbsp;&nbsp;<html:link action="/createAccount.do">...or create an account</html:link>
        </td>
      </tr>
    </table>
  </html:form>


<script language="javascript">
  var visibility = 'block';
  function toggleDiv(){
    document.getElementById('passwordDiv').style.display=visibility;
    if(visibility=='block') visibility='none';
    else visibility='block';
  }
</script>
  <br/>
<a href="javascript:toggleDiv();" >Forgotten password</a>
<div id="passwordDiv" style="display:none;padding-top:20px">
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
  <br/>

</div>
</im:boxarea>

<c:if test="${!empty OPENID_PROVIDERS && WEB_PROPERTIES['openid.allowed'] != 'false'}">
  <im:debug message="${OPENID_PROVIDERS}"/>
  <im:boxarea stylename="plainbox" fixedWidth="60%">
	<h3><fmt:message key="login.openid"/></h3>
	<ul>
		<c:forEach var="provider" items="${OPENID_PROVIDERS}">
			<li><a href="/${WEB_PROPERTIES['webapp.path']}/openid?provider=${provider}">Log in with ${provider}</a></li>
		</c:forEach>
	</ul>
  </im:boxarea>
</c:if>
<!-- /login.jsp -->
